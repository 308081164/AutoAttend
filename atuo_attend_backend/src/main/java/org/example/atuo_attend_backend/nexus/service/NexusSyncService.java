package org.example.atuo_attend_backend.nexus.service;

import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunEcsAdapter;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudInstance;
import org.example.atuo_attend_backend.nexus.domain.NexusMemoryMetricPoint;
import org.example.atuo_attend_backend.nexus.domain.NexusCpuMetricPoint;
import org.example.atuo_attend_backend.nexus.domain.NexusAutoSyncConfig;
import org.example.atuo_attend_backend.nexus.mapper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class NexusSyncService {

    private final NexusCloudAccountMapper accountMapper;
    private final NexusInstanceMapper instanceMapper;
    private final NexusCpuMetricMapper cpuMetricMapper;
    private final NexusMemoryMetricMapper memoryMetricMapper;
    private final NexusAutoSyncConfigMapper autoSyncConfigMapper;
    private final NexusAuditLogMapper auditLogMapper;
    private final NexusCryptoService cryptoService;
    private final NexusExtensionSyncService extensionSyncService;

    private final AliyunEcsAdapter aliyunEcsAdapter = new AliyunEcsAdapter();
    private final org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunCmsAdapter aliyunCmsAdapter = new org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunCmsAdapter();

    private final int maxInstancesPerRun;

    public NexusSyncService(
            NexusCloudAccountMapper accountMapper,
            NexusInstanceMapper instanceMapper,
            NexusCpuMetricMapper cpuMetricMapper,
            NexusMemoryMetricMapper memoryMetricMapper,
            NexusAutoSyncConfigMapper autoSyncConfigMapper,
            NexusAuditLogMapper auditLogMapper,
            NexusCryptoService cryptoService,
            NexusExtensionSyncService extensionSyncService,
            @Value("${nexus.sync.max-instances-per-run:200}") int maxInstancesPerRun
    ) {
        this.accountMapper = accountMapper;
        this.instanceMapper = instanceMapper;
        this.cpuMetricMapper = cpuMetricMapper;
        this.memoryMetricMapper = memoryMetricMapper;
        this.autoSyncConfigMapper = autoSyncConfigMapper;
        this.auditLogMapper = auditLogMapper;
        this.cryptoService = cryptoService;
        this.extensionSyncService = extensionSyncService;
        this.maxInstancesPerRun = maxInstancesPerRun;
    }

    public NexusSyncOutcome syncAccount(long tenantId, long accountId, String auditAction, Long actorUserId, String actorPhone) {
        Instant nowUtc = Instant.now();
        try {
            autoSyncConfigMapper.ensureDefault(tenantId);
            NexusAutoSyncConfig config = autoSyncConfigMapper.findByTenant(tenantId);
            if (config == null || !config.isEnabled()) {
                return NexusSyncOutcome.skip("auto sync disabled or config missing");
            }

            NexusCloudAccount account = accountMapper.findForSync(tenantId, accountId);
            if (account == null) return NexusSyncOutcome.fail("account not found");
            if (!Objects.equals(account.getProvider(), "aliyun")) {
                return NexusSyncOutcome.fail("provider not supported: " + account.getProvider());
            }

            String accessKeyId = cryptoService.decrypt(account.getAccessKeyIdEnc());
            String accessKeySecret = cryptoService.decrypt(account.getAccessKeySecretEnc());
            String regionId = account.getRegionId();
            int pageSize = 50;

            var instances = aliyunEcsAdapter.listInstances(accessKeyId, accessKeySecret, regionId, pageSize);
            // 保护：避免单账号过大导致本轮超时
            if (instances.size() > maxInstancesPerRun) {
                instances = instances.subList(0, maxInstancesPerRun);
            }

            for (var ins : instances) {
                int changed = instanceMapper.upsert(
                        tenantId,
                        accountId,
                        ins.instanceId,
                        ins.instanceName,
                        ins.status,
                        ins.instanceType,
                        ins.zoneId,
                        ins.publicIp,
                        ins.privateIp,
                        ins.osName,
                        ins.memoryMb
                );
                // changed 不强制使用；mvp 只要落库成功即可
            }

            Instant startTimeUtc = nowUtc.minusSeconds(config.getCpuWindowMinutes() * 60L);
            Instant endTimeUtc = nowUtc;

            for (var ins : instances) {
                // 注意：DescribeInstanceMonitorData 一次返回最多 400 条（按 (End-Start)/Period 约束）
                var points = aliyunEcsAdapter.fetchCpuPoints(
                        accessKeyId, accessKeySecret, regionId,
                        ins.instanceId,
                        startTimeUtc, endTimeUtc,
                        config.getCpuPeriodSeconds()
                );
                for (var pt : points) {
                    NexusCpuMetricPoint p = new NexusCpuMetricPoint();
                    p.setTenantId(tenantId);
                    p.setAccountId(accountId);
                    p.setInstanceId(pt.instanceId);
                    p.setTs(pt.ts);
                    p.setValue(pt.cpuValue != null ? pt.cpuValue : 0d);
                    cpuMetricMapper.upsertPoint(p);
                }
            }

            // 内存利用率：来自 CMS DescribeMetricList（acs_ecs_dashboard / memory_usedutilization）
            for (var ins : instances) {
                var memPoints = aliyunCmsAdapter.fetchMemoryPoints(
                        accessKeyId, accessKeySecret, regionId,
                        ins.instanceId,
                        startTimeUtc, endTimeUtc,
                        config.getCpuPeriodSeconds() // MVP：复用 CPU 的 period/window 配置
                );
                for (var mp : memPoints) {
                    mp.setTenantId(tenantId);
                    mp.setAccountId(accountId);
                    mp.setInstanceId(mp.getInstanceId() != null ? mp.getInstanceId() : ins.instanceId);
                    memoryMetricMapper.upsertPoint(mp);
                }
            }

            try {
                extensionSyncService.syncExtensions(tenantId, accountId, accessKeyId, accessKeySecret, regionId);
            } catch (Exception ignored) {
                // 扩展同步失败不阻断主流程（日志在 extension 内）
            }

            accountMapper.updateLastAutoSyncAt(tenantId, accountId, LocalDateTime.now());
            auditLogMapper.insert(tenantId, actorUserId, actorPhone, auditAction,
                    "cloud_account", String.valueOf(accountId), "success", null);
            return NexusSyncOutcome.success();
        } catch (Exception e) {
            try {
                auditLogMapper.insert(tenantId, actorUserId, actorPhone, auditAction,
                        "cloud_account", String.valueOf(accountId), "fail",
                        e.getMessage() != null ? e.getMessage().substring(0, Math.min(500, e.getMessage().length())) : null);
            } catch (Exception ignored) {
            }
            return NexusSyncOutcome.fail(e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "unknown" : e.getMessage()));
        }
    }

    public static class NexusSyncOutcome {
        public boolean success;
        public boolean skip;
        public String reason;

        public static NexusSyncOutcome success() {
            NexusSyncOutcome o = new NexusSyncOutcome();
            o.success = true;
            o.skip = false;
            o.reason = null;
            return o;
        }

        public static NexusSyncOutcome fail(String reason) {
            NexusSyncOutcome o = new NexusSyncOutcome();
            o.success = false;
            o.skip = false;
            o.reason = reason;
            return o;
        }

        public static NexusSyncOutcome skip(String reason) {
            NexusSyncOutcome o = new NexusSyncOutcome();
            o.success = false;
            o.skip = true;
            o.reason = reason;
            return o;
        }
    }
}

