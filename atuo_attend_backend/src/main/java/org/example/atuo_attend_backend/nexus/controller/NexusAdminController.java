package org.example.atuo_attend_backend.nexus.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunBssAdapter;
import org.example.atuo_attend_backend.nexus.domain.NexusAlertRule;
import org.example.atuo_attend_backend.nexus.domain.NexusAutoSyncConfig;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudInstance;
import org.example.atuo_attend_backend.nexus.dto.*;
import org.example.atuo_attend_backend.nexus.mapper.*;
import org.example.atuo_attend_backend.nexus.service.NexusBssCostService;
import org.example.atuo_attend_backend.nexus.service.NexusEcsOpsService;
import org.example.atuo_attend_backend.nexus.service.NexusSyncService;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.YearMonth;
import java.util.ArrayList;
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
    private final NexusEcsOpsService ecsOpsService;
    private final NexusBssCostService bssCostService;
    private final NexusAlertRuleMapper alertRuleMapper;
    private final TenantResourceQuotaService tenantResourceQuotaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NexusAdminController(
            NexusCloudAccountMapper accountMapper,
            NexusInstanceMapper instanceMapper,
            NexusCpuMetricMapper cpuMetricMapper,
            NexusMemoryMetricMapper memoryMetricMapper,
            NexusAutoSyncConfigMapper autoSyncConfigMapper,
            NexusCryptoService cryptoService,
            NexusSyncService syncService,
            NexusAuditLogMapper auditLogMapper,
            NexusEcsOpsService ecsOpsService,
            NexusBssCostService bssCostService,
            NexusAlertRuleMapper alertRuleMapper,
            TenantResourceQuotaService tenantResourceQuotaService
    ) {
        this.accountMapper = accountMapper;
        this.instanceMapper = instanceMapper;
        this.cpuMetricMapper = cpuMetricMapper;
        this.memoryMetricMapper = memoryMetricMapper;
        this.autoSyncConfigMapper = autoSyncConfigMapper;
        this.cryptoService = cryptoService;
        this.syncService = syncService;
        this.auditLogMapper = auditLogMapper;
        this.ecsOpsService = ecsOpsService;
        this.bssCostService = bssCostService;
        this.alertRuleMapper = alertRuleMapper;
        this.tenantResourceQuotaService = tenantResourceQuotaService;
    }

    /**
     * 管理端接口应优先使用会话中的租户 ID。{@link TenantClearFilter} 先于 {@link AdminAuthFilter} 执行，
     * 会在进入 Controller 前清空 {@link TenantContext}，因此不能仅依赖 ThreadLocal。
     */
    private static long tenantIdFrom(HttpServletRequest request) {
        Object tid = request != null ? request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID) : null;
        if (tid instanceof Long l) {
            return l;
        }
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    @GetMapping("/accounts")
    public ApiResponse<Map<String, Object>> listAccounts(HttpServletRequest request) {
        long tenantId = tenantIdFrom(request);
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
        long tenantId = tenantIdFrom(request);

        if (req == null) return ApiResponse.error(40000, "request is null");
        if (req.getDisplayName() == null || req.getDisplayName().isBlank()) return ApiResponse.error(40000, "displayName required");
        if (req.getRegionId() == null || req.getRegionId().isBlank()) return ApiResponse.error(40000, "regionId required");
        if (req.getAccessKeyId() == null || req.getAccessKeyId().isBlank()) return ApiResponse.error(40000, "accessKeyId required");
        if (req.getAccessKeySecret() == null || req.getAccessKeySecret().isBlank()) return ApiResponse.error(40000, "accessKeySecret required");

        try {
            tenantResourceQuotaService.assertCanCreateNexusAccount(tenantId);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }

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
            @PathVariable long accountId,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        List<NexusCloudInstance> items = instanceMapper.listByAccount(tenantId, accountId);
        return ApiResponse.ok(items);
    }

    @PostMapping("/accounts/{accountId}/sync")
    public ApiResponse<Map<String, Object>> manualSync(
            @PathVariable long accountId,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
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
            @RequestParam(defaultValue = "60") int limit,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        List<NexusCpuMetricMapper.MetricRow> items = cpuMetricMapper.listCpuPoints(tenantId, accountId, instanceId, limit);
        return ApiResponse.ok(items);
    }

    @GetMapping("/accounts/{accountId}/instances/{instanceId}/memory-metrics")
    public ApiResponse<List<NexusMemoryMetricMapper.MetricRow>> memoryMetrics(
            @PathVariable long accountId,
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "60") int limit,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
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
        long tenantId = tenantIdFrom(request);
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

    @GetMapping("/sync-config")
    public ApiResponse<NexusAutoSyncConfig> getSyncConfig(HttpServletRequest request) {
        long tenantId = tenantIdFrom(request);
        autoSyncConfigMapper.ensureDefault(tenantId);
        NexusAutoSyncConfig cfg = autoSyncConfigMapper.findByTenant(tenantId);
        return ApiResponse.ok(cfg);
    }

    @PutMapping("/sync-config")
    public ApiResponse<Void> updateSyncConfig(
            @RequestBody NexusSyncConfigUpdateRequest req,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        autoSyncConfigMapper.ensureDefault(tenantId);
        NexusAutoSyncConfig cur = autoSyncConfigMapper.findByTenant(tenantId);
        if (cur == null) return ApiResponse.error(50000, "config missing");
        boolean en = req.getEnabled() != null ? req.getEnabled() : cur.isEnabled();
        int g = req.getGlobalIntervalSeconds() != null ? req.getGlobalIntervalSeconds() : cur.getGlobalIntervalSeconds();
        int p = req.getCpuPeriodSeconds() != null ? req.getCpuPeriodSeconds() : cur.getCpuPeriodSeconds();
        int w = req.getCpuWindowMinutes() != null ? req.getCpuWindowMinutes() : cur.getCpuWindowMinutes();
        if (g < 10 || p < 15 || w < 1) {
            return ApiResponse.error(40000, "invalid interval/window (min global 10s, period 15s, window 1m)");
        }
        autoSyncConfigMapper.updateConfig(tenantId, en, g, p, w);
        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        auditLogMapper.insert(tenantId, actorUserId, actorPhone, "nexus.sync_config.update", "tenant", String.valueOf(tenantId), "success", null);
        return ApiResponse.ok(null);
    }

    @PutMapping("/accounts/{accountId}/settings")
    public ApiResponse<Void> updateAccountSettings(
            @PathVariable long accountId,
            @RequestBody NexusAccountSettingsRequest req,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        NexusCloudAccount acc = accountMapper.findForSync(tenantId, accountId);
        if (acc == null) return ApiResponse.error(40400, "account not found");
        if (req.getDisplayName() != null && !req.getDisplayName().isBlank()) {
            accountMapper.updateDisplayName(tenantId, accountId, req.getDisplayName().trim());
        }
        if (req.getAutoSyncIntervalSeconds() != null) {
            Integer v = req.getAutoSyncIntervalSeconds();
            if (v < 10) return ApiResponse.error(40000, "autoSyncIntervalSeconds must be >= 10 or use null for global default");
            accountMapper.updateAutoSyncInterval(tenantId, accountId, v);
        }
        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        auditLogMapper.insert(tenantId, actorUserId, actorPhone, "nexus.account.settings", "cloud_account", String.valueOf(accountId), "success", null);
        return ApiResponse.ok(null);
    }

    /**
     * 将本云账号的巡检间隔恢复为「跟随全局」：auto_sync_interval_seconds = NULL
     */
    @PostMapping("/accounts/{accountId}/settings/clear-interval-override")
    public ApiResponse<Void> clearAccountIntervalOverride(
            @PathVariable long accountId,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        if (accountMapper.findForSync(tenantId, accountId) == null) return ApiResponse.error(40400, "account not found");
        accountMapper.updateAutoSyncInterval(tenantId, accountId, null);
        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        auditLogMapper.insert(tenantId, actorUserId, actorPhone, "nexus.account.clear_interval", "cloud_account", String.valueOf(accountId), "success", null);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/accounts/{accountId}/instances/{instanceId}/ops")
    public ApiResponse<Void> patchInstanceOps(
            @PathVariable long accountId,
            @PathVariable String instanceId,
            @RequestBody NexusInstanceOpsRequest req,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        String url = req != null && req.getBtPanelUrl() != null ? req.getBtPanelUrl().trim() : null;
        if (url != null && url.isEmpty()) url = null;
        if (url != null && !url.startsWith("https://") && !url.startsWith("http://")) {
            return ApiResponse.error(40000, "btPanelUrl must be http(s) URL");
        }
        int n = instanceMapper.updateBtPanelUrl(tenantId, accountId, instanceId, url);
        if (n <= 0) return ApiResponse.error(40400, "instance not found");
        return ApiResponse.ok(null);
    }

    @PostMapping("/accounts/{accountId}/instances/{instanceId}/lifecycle")
    public ApiResponse<Map<String, Object>> instanceLifecycle(
            @PathVariable long accountId,
            @PathVariable String instanceId,
            @RequestBody NexusLifecycleRequest req,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        if (req == null || req.getAction() == null) return ApiResponse.error(40000, "action required");
        boolean force = Boolean.TRUE.equals(req.getForceStop());
        String action = req.getAction().trim().toLowerCase();
        try {
            ecsOpsService.runLifecycle(tenantId, accountId, instanceId, action, force);
            auditLogMapper.insert(tenantId, actorUserId, actorPhone, "nexus.ecs." + action, "instance", instanceId, "success", null);
            return ApiResponse.ok(Map.of("ok", true));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            auditLogMapper.insert(tenantId, actorUserId, actorPhone, "nexus.ecs." + action, "instance", instanceId, "fail",
                    msg.length() > 500 ? msg.substring(0, 500) : msg);
            return ApiResponse.error(50000, msg);
        }
    }

    @PostMapping("/accounts/{accountId}/instances/{instanceId}/bt-panel/action")
    public ApiResponse<Void> btPanelAction(
            @PathVariable long accountId,
            @PathVariable String instanceId,
            @RequestBody NexusBtPanelActionRequest req,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        Long actorUserId = (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String actorPhone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        String type = req != null ? req.getType() : null;
        if (!"open".equals(type)) return ApiResponse.error(40000, "type must be open");
        String metaJson;
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("url", req.getUrl());
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            metaJson = null;
        }
        auditLogMapper.insert(tenantId, actorUserId, actorPhone, "nexus.bt_panel.open", "instance", instanceId, "success", metaJson);
        return ApiResponse.ok(null);
    }

    @GetMapping("/accounts/{accountId}/cost/summary")
    public ApiResponse<Map<String, Object>> costSummary(
            @PathVariable long accountId,
            @RequestParam(required = false) String billingCycle,
            @RequestParam(defaultValue = "15") int topN,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        String cycle = billingCycle;
        if (cycle == null || cycle.isBlank()) {
            cycle = YearMonth.now().minusMonths(1).toString();
        }
        try {
            AliyunBssAdapter.CostSummary s = bssCostService.summarizeForAccount(tenantId, accountId, cycle, topN);
            List<Map<String, Object>> top = new ArrayList<>();
            for (AliyunBssAdapter.InstanceCostRow r : s.topByInstance) {
                Map<String, Object> m = new HashMap<>();
                m.put("instanceId", r.instanceId);
                m.put("instanceName", r.instanceName);
                m.put("amount", r.amount);
                top.add(m);
            }
            return ApiResponse.ok(Map.of(
                    "billingCycle", cycle,
                    "totalPretaxEcs", s.totalPretax,
                    "topByInstance", top
            ));
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "cost query failed");
        }
    }

    @GetMapping("/alert-rules")
    public ApiResponse<List<NexusAlertRule>> listAlertRules(HttpServletRequest request) {
        long tenantId = tenantIdFrom(request);
        return ApiResponse.ok(alertRuleMapper.listByTenant(tenantId));
    }

    @PostMapping("/alert-rules")
    public ApiResponse<Map<String, Object>> createAlertRule(
            @RequestBody NexusAlertRuleWriteRequest req,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        ApiResponse<Map<String, Object>> err = validateAlertWrite(tenantId, req, true);
        if (err != null) return err;
        NexusAlertRule r = toRule(tenantId, req, null);
        alertRuleMapper.insert(r);
        return ApiResponse.ok(Map.of("id", r.getId()));
    }

    @PutMapping("/alert-rules/{ruleId}")
    public ApiResponse<Void> updateAlertRule(
            @PathVariable long ruleId,
            @RequestBody NexusAlertRuleWriteRequest req,
            HttpServletRequest request
    ) {
        long tenantId = tenantIdFrom(request);
        NexusAlertRule existing = alertRuleMapper.findById(tenantId, ruleId);
        if (existing == null) return ApiResponse.error(40400, "rule not found");
        ApiResponse<Map<String, Object>> err = validateAlertWrite(tenantId, req, false);
        if (err != null) {
            return ApiResponse.error(err.getCode(), err.getMessage());
        }
        NexusAlertRule r = toRule(tenantId, req, existing);
        r.setId(ruleId);
        r.setLastTriggeredAt(existing.getLastTriggeredAt());
        alertRuleMapper.update(r);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/alert-rules/{ruleId}")
    public ApiResponse<Void> deleteAlertRule(@PathVariable long ruleId, HttpServletRequest request) {
        long tenantId = tenantIdFrom(request);
        int n = alertRuleMapper.delete(tenantId, ruleId);
        if (n <= 0) return ApiResponse.error(40400, "rule not found");
        return ApiResponse.ok(null);
    }

    private ApiResponse<Map<String, Object>> validateAlertWrite(long tenantId, NexusAlertRuleWriteRequest req, boolean creating) {
        if (req == null) return ApiResponse.error(40000, "body required");
        if (creating && (req.getAccountId() == null)) {
            return ApiResponse.error(40000, "accountId required");
        }
        if (req.getAccountId() != null && accountMapper.findForSync(tenantId, req.getAccountId()) == null) {
            return ApiResponse.error(40000, "account not found");
        }
        String mt = req.getMetricType() != null ? req.getMetricType().trim().toLowerCase() : "";
        if (!mt.equals("cpu") && !mt.equals("memory")) {
            return ApiResponse.error(40000, "metricType must be cpu or memory");
        }
        String op = req.getOp() != null ? req.getOp().trim().toLowerCase() : "";
        if (!op.equals("gt") && !op.equals("gte") && !op.equals("lt") && !op.equals("lte")) {
            return ApiResponse.error(40000, "op must be gt|gte|lt|lte");
        }
        if (req.getThreshold() == null) return ApiResponse.error(40000, "threshold required");
        boolean hasChannel = (req.getWebhookUrl() != null && !req.getWebhookUrl().isBlank())
                || (req.getNotifyEmail() != null && !req.getNotifyEmail().isBlank());
        if (!hasChannel) return ApiResponse.error(40000, "webhookUrl or notifyEmail required");
        return null;
    }

    private static NexusAlertRule toRule(long tenantId, NexusAlertRuleWriteRequest req, NexusAlertRule existing) {
        NexusAlertRule r = new NexusAlertRule();
        r.setTenantId(tenantId);
        Long acc = req.getAccountId() != null ? req.getAccountId() : (existing != null ? existing.getAccountId() : null);
        r.setAccountId(acc);
        String inst = req.getInstanceId();
        if (inst != null) {
            inst = inst.trim();
            r.setInstanceId(inst.isEmpty() ? null : inst);
        } else {
            r.setInstanceId(existing != null ? existing.getInstanceId() : null);
        }
        String name = req.getName() != null ? req.getName().trim() : null;
        r.setName(name != null && !name.isEmpty() ? name : (existing != null ? existing.getName() : "rule"));
        r.setMetricType(req.getMetricType().trim().toLowerCase());
        r.setOp(req.getOp().trim().toLowerCase());
        r.setThreshold(req.getThreshold());
        r.setDurationMinutes(req.getDurationMinutes() != null ? req.getDurationMinutes()
                : (existing != null ? existing.getDurationMinutes() : 5));
        String wh = req.getWebhookUrl() != null ? req.getWebhookUrl().trim() : null;
        r.setWebhookUrl(wh != null && !wh.isEmpty() ? wh : (existing != null ? existing.getWebhookUrl() : null));
        String em = req.getNotifyEmail() != null ? req.getNotifyEmail().trim() : null;
        r.setNotifyEmail(em != null && !em.isEmpty() ? em : (existing != null ? existing.getNotifyEmail() : null));
        r.setSilenceSeconds(req.getSilenceSeconds() != null ? req.getSilenceSeconds()
                : (existing != null ? existing.getSilenceSeconds() : 900));
        if (req.getEnabled() != null) {
            r.setEnabled(Boolean.TRUE.equals(req.getEnabled()));
        } else {
            r.setEnabled(existing == null || existing.isEnabled());
        }
        return r;
    }
}

