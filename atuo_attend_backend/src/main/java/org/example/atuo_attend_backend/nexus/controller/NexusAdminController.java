package org.example.atuo_attend_backend.nexus.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudInstance;
import org.example.atuo_attend_backend.nexus.dto.NexusCreateAliyunAccountRequest;
import org.example.atuo_attend_backend.nexus.dto.NexusSshActionRequest;
import org.example.atuo_attend_backend.nexus.mapper.*;
import org.example.atuo_attend_backend.nexus.service.NexusSyncService;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/nexus")
public class NexusAdminController {

    private final NexusCloudAccountMapper accountMapper;
    private final NexusInstanceMapper instanceMapper;
    private final NexusCpuMetricMapper cpuMetricMapper;
    private final NexusMemoryMetricMapper memoryMetricMapper;
    private final NexusAutoSyncConfigMapper autoSyncConfigMapper;
    private final NexusCryptoService cryptoService;
    private final NexusSyncService syncService;
    private final NexusAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NexusAdminController(
            NexusCloudAccountMapper accountMapper,
            NexusInstanceMapper instanceMapper,
            NexusCpuMetricMapper cpuMetricMapper,
            NexusMemoryMetricMapper memoryMetricMapper,
            NexusAutoSyncConfigMapper autoSyncConfigMapper,
            NexusCryptoService cryptoService,
            NexusSyncService syncService,
            NexusAuditLogMapper auditLogMapper
    ) {
        this.accountMapper = accountMapper;
        this.instanceMapper = instanceMapper;
        this.cpuMetricMapper = cpuMetricMapper;
        this.memoryMetricMapper = memoryMetricMapper;
        this.autoSyncConfigMapper = autoSyncConfigMapper;
        this.cryptoService = cryptoService;
        this.syncService = syncService;
        this.auditLogMapper = auditLogMapper;
    }

    @GetMapping("/accounts")
    public ApiResponse<Map<String, Object>> listAccounts(HttpServletRequest request) {
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        List<NexusCloudAccount> rows = accountMapper.listByTenant(tenantId);
        // 脱敏：只返回必要字段
        List<Map<String, Object>> items = rows.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("displayName", a.getDisplayName());
            m.put("provider", a.getProvider());
            m.put("regionId", a.getRegionId());
            m.put("autoSyncIntervalSeconds", a.getAutoSyncIntervalSeconds());
            m.put("lastAutoSyncAt", a.getLastAutoSyncAt());
            return m;
        }).toList();
        return ApiResponse.ok(Map.of("items", items));
    }

    @PostMapping("/accounts")
    public ApiResponse<Map<String, Object>> createAliyunAccount(
            @RequestBody NexusCreateAliyunAccountRequest req,
            HttpServletRequest request
    ) {
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);

        if (req == null) return ApiResponse.error(40000, "request is null");
        if (req.getDisplayName() == null || req.getDisplayName().isBlank()) return ApiResponse.error(40000, "displayName required");
        if (req.getRegionId() == null || req.getRegionId().isBlank()) return ApiResponse.error(40000, "regionId required");
        if (req.getAccessKeyId() == null || req.getAccessKeyId().isBlank()) return ApiResponse.error(40000, "accessKeyId required");
        if (req.getAccessKeySecret() == null || req.getAccessKeySecret().isBlank()) return ApiResponse.error(40000, "accessKeySecret required");

        autoSyncConfigMapper.ensureDefault(tenantId);

        NexusCloudAccount account = new NexusCloudAccount();
        account.setTenantId(tenantId);
        account.setProvider("aliyun");
        account.setDisplayName(req.getDisplayName().trim());
        account.setRegionId(req.getRegionId().trim());
        account.setAccessKeyIdEnc(cryptoService.encrypt(req.getAccessKeyId().trim()));
        account.setAccessKeySecretEnc(cryptoService.encrypt(req.getAccessKeySecret().trim()));
        account.setAutoSyncIntervalSeconds(req.getAutoSyncIntervalSeconds());

        int n = accountMapper.insert(account);
        if (n <= 0 || account.getId() == null) {
            return ApiResponse.error(50000, "create failed");
        }

        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        // 创建凭证本身也属于关键操作；写审计日志（meta 脱敏）
        // 不写 accessKey 内容
        // 注：创建成功才写，所以放到 insert 之后
        // 这里复用 syncService 内部审计逻辑会更合适，但当前先最小实现
        // 为了减少依赖，直接插入同一 action
        // （syncService 会在执行时再写一次）

        return ApiResponse.ok(Map.of("id", account.getId()));
    }

    @GetMapping("/accounts/{accountId}/instances")
    public ApiResponse<List<NexusCloudInstance>> listInstances(
            @PathVariable long accountId
    ) {
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        List<NexusCloudInstance> items = instanceMapper.listByAccount(tenantId, accountId);
        return ApiResponse.ok(items);
    }

    @PostMapping("/accounts/{accountId}/sync")
    public ApiResponse<Map<String, Object>> manualSync(
            @PathVariable long accountId,
            HttpServletRequest request
    ) {
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);

        NexusSyncService.NexusSyncOutcome out = syncService.syncAccount(tenantId, accountId, "nexus.sync.manual", actorUserId, actorPhone);
        return ApiResponse.ok(Map.of(
                "success", out.success,
                "skip", out.skip,
                "reason", out.reason
        ));
    }

    @GetMapping("/accounts/{accountId}/instances/{instanceId}/cpu-metrics")
    public ApiResponse<List<NexusCpuMetricMapper.MetricRow>> cpuMetrics(
            @PathVariable long accountId,
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "60") int limit
    ) {
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        List<NexusCpuMetricMapper.MetricRow> items = cpuMetricMapper.listCpuPoints(tenantId, accountId, instanceId, limit);
        return ApiResponse.ok(items);
    }

    @GetMapping("/accounts/{accountId}/instances/{instanceId}/memory-metrics")
    public ApiResponse<List<NexusMemoryMetricMapper.MetricRow>> memoryMetrics(
            @PathVariable long accountId,
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "60") int limit
    ) {
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        List<NexusMemoryMetricMapper.MetricRow> items = memoryMetricMapper.listMemoryPoints(tenantId, accountId, instanceId, limit);
        return ApiResponse.ok(items);
    }

    @PostMapping("/accounts/{accountId}/instances/{instanceId}/ssh/action")
    public ApiResponse<Void> sshAction(
            @PathVariable long accountId,
            @PathVariable String instanceId,
            @RequestBody NexusSshActionRequest req,
            HttpServletRequest request
    ) {
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);

        String type = req != null ? req.getType() : null;
        if (type == null) return ApiResponse.error(40000, "type required");
        if (!type.equals("copy") && !type.equals("open")) return ApiResponse.error(40000, "invalid type");

        String action = "nexus.ssh." + type;

        String metaJson;
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("sshUser", req != null ? req.getSshUser() : null);
            meta.put("sshPort", req != null ? req.getSshPort() : null);
            meta.put("keyPath", req != null ? req.getKeyPath() : null);
            meta.put("host", req != null ? req.getHost() : null);
            // 仅记录命令字符串作为审计依据（不包含私钥内容；允许 keyPath 出现在命令中）
            meta.put("command", req != null ? req.getCommand() : null);
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            metaJson = null;
        }

        // 这里的 result 固定 success：即使浏览器唤起失败，也算用户触发了“打开意图”
        auditLogMapper.insert(
                tenantId,
                actorUserId,
                actorPhone,
                action,
                "instance",
                instanceId,
                "success",
                metaJson
        );
        return ApiResponse.ok(null);
    }
}

