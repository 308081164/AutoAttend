package org.example.atuo_attend_backend.nexus.service;

import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunCmsAdapter;
import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunEcsAdapter;
import org.example.atuo_attend_backend.nexus.domain.NexusAutoSyncConfig;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.domain.NexusMemoryMetricPoint;
import org.example.atuo_attend_backend.nexus.dto.NexusMetricChartPoint;
import org.example.atuo_attend_backend.nexus.mapper.NexusAutoSyncConfigMapper;
import org.example.atuo_attend_backend.nexus.mapper.NexusCloudAccountMapper;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 按需从阿里云拉取 CPU/内存指标时间序列（不落库），供前端轮询图表使用，减轻后台定时巡检对「仅看图」场景的依赖。
 */
@Service
public class NexusMetricLiveService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final NexusCloudAccountMapper accountMapper;
    private final NexusCryptoService cryptoService;
    private final NexusAutoSyncConfigMapper autoSyncConfigMapper;

    private final AliyunEcsAdapter ecsAdapter = new AliyunEcsAdapter();
    private final AliyunCmsAdapter cmsAdapter = new AliyunCmsAdapter();

    public NexusMetricLiveService(
            NexusCloudAccountMapper accountMapper,
            NexusCryptoService cryptoService,
            NexusAutoSyncConfigMapper autoSyncConfigMapper
    ) {
        this.accountMapper = accountMapper;
        this.cryptoService = cryptoService;
        this.autoSyncConfigMapper = autoSyncConfigMapper;
    }

    public List<NexusMetricChartPoint> fetchCpuLive(long tenantId, long accountId, String instanceId) throws Exception {
        ResolvedAccount ra = resolveAccount(tenantId, accountId);
        NexusAutoSyncConfig cfg = resolveConfig(tenantId);
        TimeWindow w = cpuQueryWindow(cfg);
        List<AliyunEcsAdapter.CpuPointInfo> points = ecsAdapter.fetchCpuPoints(
                ra.accessKeyId, ra.accessKeySecret, ra.regionId,
                instanceId, w.start(), w.end(), w.periodSeconds()
        );
        List<NexusMetricChartPoint> out = new ArrayList<>();
        if (points != null) {
            for (AliyunEcsAdapter.CpuPointInfo p : points) {
                if (p == null || p.ts == null) continue;
                out.add(new NexusMetricChartPoint(p.ts.format(TS_FORMAT), p.cpuValue != null ? p.cpuValue : 0d));
            }
        }
        return out;
    }

    public List<NexusMetricChartPoint> fetchMemoryLive(long tenantId, long accountId, String instanceId) throws Exception {
        ResolvedAccount ra = resolveAccount(tenantId, accountId);
        NexusAutoSyncConfig cfg = resolveConfig(tenantId);
        TimeWindow w = cpuQueryWindow(cfg);
        // CMS 常用周期为 60/300；与 CPU 窗口对齐，避免 period 过小导致无点
        int cmsPeriod = w.periodSeconds() >= 300 ? 300 : 60;
        List<NexusMemoryMetricPoint> points = cmsAdapter.fetchMemoryPoints(
                ra.accessKeyId, ra.accessKeySecret, ra.regionId,
                instanceId, w.start(), w.end(), cmsPeriod
        );
        List<NexusMetricChartPoint> out = new ArrayList<>();
        if (points != null) {
            for (NexusMemoryMetricPoint mp : points) {
                if (mp == null || mp.getTs() == null) continue;
                double v = mp.getValue() != null ? mp.getValue() : 0d;
                out.add(new NexusMetricChartPoint(mp.getTs().format(TS_FORMAT), v));
            }
        }
        return out;
    }

    private ResolvedAccount resolveAccount(long tenantId, long accountId) throws IllegalArgumentException {
        NexusCloudAccount acc = accountMapper.findForSync(tenantId, accountId);
        if (acc == null) {
            throw new IllegalArgumentException("account not found");
        }
        if (!"aliyun".equals(acc.getProvider())) {
            throw new IllegalArgumentException("provider not supported");
        }
        String ak = cryptoService.decrypt(acc.getAccessKeyIdEnc());
        String sk = cryptoService.decrypt(acc.getAccessKeySecretEnc());
        String region = Objects.requireNonNullElse(acc.getRegionId(), "").trim();
        if (region.isEmpty()) {
            throw new IllegalArgumentException("region missing");
        }
        return new ResolvedAccount(ak, sk, region);
    }

    private NexusAutoSyncConfig resolveConfig(long tenantId) {
        autoSyncConfigMapper.ensureDefault(tenantId);
        NexusAutoSyncConfig c = autoSyncConfigMapper.findByTenant(tenantId);
        if (c == null) {
            NexusAutoSyncConfig d = new NexusAutoSyncConfig();
            d.setCpuPeriodSeconds(60);
            d.setCpuWindowMinutes(60);
            return d;
        }
        int period = c.getCpuPeriodSeconds() > 0 ? c.getCpuPeriodSeconds() : 60;
        int win = c.getCpuWindowMinutes() > 0 ? c.getCpuWindowMinutes() : 60;
        c.setCpuPeriodSeconds(period);
        c.setCpuWindowMinutes(win);
        return c;
    }

    private record ResolvedAccount(String accessKeyId, String accessKeySecret, String regionId) {
    }

    /**
     * DescribeInstanceMonitorData：{@code (End-Start)/Period <= 400}；周期过小 + 窗口过大会非法。
     */
    private static TimeWindow cpuQueryWindow(NexusAutoSyncConfig cfg) {
        int period = Math.max(15, cfg.getCpuPeriodSeconds());
        // 上限与巡检配置一致，避免单次请求窗口过大
        int windowMin = Math.min(240, Math.max(1, cfg.getCpuWindowMinutes()));
        long spanSec = (long) windowMin * 60L;
        long maxSpan = (long) period * 399L;
        if (spanSec > maxSpan) {
            spanSec = maxSpan;
        }
        Instant end = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant start = end.minusSeconds(spanSec);
        return new TimeWindow(start, end, period);
    }

    private record TimeWindow(Instant start, Instant end, int periodSeconds) {
    }
}
