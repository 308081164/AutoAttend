package org.example.atuo_attend_backend.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.platform.auth.PlatformAuthFilter;
import org.example.atuo_attend_backend.platform.dto.PlatformTenantOpsRow;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsAuditMapper;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsReportMapper;
import org.example.atuo_attend_backend.tenant.billing.TenantBillingService;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.AdminSessionMapper;
import org.example.atuo_attend_backend.tenant.mapper.SubscriptionOrderMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台监测台：租户详情与运维动作（需平台会话）。
 */
@RestController
@RequestMapping("/api/platform/tenants")
public class PlatformTenantController {

    private final PlatformOpsReportMapper platformOpsReportMapper;
    private final TenantMapper tenantMapper;
    private final AdminSessionMapper adminSessionMapper;
    private final PlatformOpsAuditMapper platformOpsAuditMapper;
    private final SubscriptionOrderMapper subscriptionOrderMapper;
    private final TenantBillingService tenantBillingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlatformTenantController(PlatformOpsReportMapper platformOpsReportMapper,
                                    TenantMapper tenantMapper,
                                    AdminSessionMapper adminSessionMapper,
                                    PlatformOpsAuditMapper platformOpsAuditMapper,
                                    SubscriptionOrderMapper subscriptionOrderMapper,
                                    TenantBillingService tenantBillingService) {
        this.platformOpsReportMapper = platformOpsReportMapper;
        this.tenantMapper = tenantMapper;
        this.adminSessionMapper = adminSessionMapper;
        this.platformOpsAuditMapper = platformOpsAuditMapper;
        this.subscriptionOrderMapper = subscriptionOrderMapper;
        this.tenantBillingService = tenantBillingService;
    }

    private Long sessionId(HttpServletRequest request) {
        Object v = request.getAttribute(PlatformAuthFilter.ATTR_PLATFORM_SESSION_ID);
        return v instanceof Long ? (Long) v : null;
    }

    private void audit(HttpServletRequest request, String action, long tenantId, Map<String, Object> payload) {
        Long sid = sessionId(request);
        String json = null;
        if (payload != null) {
            try {
                json = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                json = "{}";
            }
        }
        platformOpsAuditMapper.insert(sid, "platform", action, tenantId, json);
    }

    @GetMapping("/{tenantId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("tenantId") long tenantId,
                                                   HttpServletRequest request) {
        PlatformTenantOpsRow row = platformOpsReportMapper.findTenantOpsRowById(tenantId);
        if (row == null) {
            return ApiResponse.error(40400, "租户不存在");
        }
        long mrrCents = subscriptionOrderMapper.sumAmountCentsLast30Days();
        long activeSubs = tenantMapper.countActivePaidSubscriptions();
        Tenant t = tenantMapper.findById(tenantId);
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> tenantPayload = new HashMap<>();
        tenantPayload.put("ops", row);
        if (t != null) {
            tenantPayload.put("projectMarketplaceEnabled", t.getProjectMarketplaceEnabled());
            tenantPayload.put("projectMarketplaceAllowPublish", t.getProjectMarketplaceAllowPublish());
        }
        data.put("tenant", tenantPayload);
        data.put("mrrApproxCents30d", mrrCents);
        data.put("activePaidSubscriptions", activeSubs);
        return ApiResponse.ok(data);
    }

    /**
     * 租户级「项目信息发布」：是否对本租户开放浏览、是否允许发布（仍须管理员账号具备发布权限位）。
     */
    @PutMapping("/{tenantId}/project-marketplace")
    public ApiResponse<Void> updateProjectMarketplace(@PathVariable("tenantId") long tenantId,
                                                      @RequestBody(required = false) Map<String, Object> body,
                                                      HttpServletRequest request) {
        if (tenantMapper.findById(tenantId) == null) {
            return ApiResponse.error(40400, "租户不存在");
        }
        boolean browse = body != null && body.get("projectMarketplaceEnabled") instanceof Boolean
                ? (Boolean) body.get("projectMarketplaceEnabled") : true;
        boolean publish = body != null && body.get("projectMarketplaceAllowPublish") instanceof Boolean
                ? (Boolean) body.get("projectMarketplaceAllowPublish") : false;
        tenantMapper.updateProjectMarketplacePrefs(tenantId, browse, publish);
        audit(request, "tenant.project_marketplace", tenantId,
                Map.of("projectMarketplaceEnabled", browse, "projectMarketplaceAllowPublish", publish));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{tenantId}/suspend")
    public ApiResponse<Map<String, Object>> suspend(@PathVariable("tenantId") long tenantId,
                                                  HttpServletRequest request) {
        int n = tenantMapper.updateStatus(tenantId, "suspended");
        if (n == 0) {
            return ApiResponse.error(40400, "租户不存在");
        }
        audit(request, "tenant.suspend", tenantId, Map.of("status", "suspended"));
        return ApiResponse.ok(Map.of("ok", true));
    }

    @PostMapping("/{tenantId}/resume")
    public ApiResponse<Map<String, Object>> resume(@PathVariable("tenantId") long tenantId,
                                                   HttpServletRequest request) {
        int n = tenantMapper.updateStatus(tenantId, "active");
        if (n == 0) {
            return ApiResponse.error(40400, "租户不存在");
        }
        audit(request, "tenant.resume", tenantId, Map.of("status", "active"));
        return ApiResponse.ok(Map.of("ok", true));
    }

    @PostMapping("/{tenantId}/revoke-admin-sessions")
    public ApiResponse<Map<String, Object>> revokeAdminSessions(@PathVariable("tenantId") long tenantId,
                                                                HttpServletRequest request) {
        if (tenantMapper.findById(tenantId) == null) {
            return ApiResponse.error(40400, "租户不存在");
        }
        int deleted = adminSessionMapper.deleteByTenantId(tenantId);
        audit(request, "tenant.revoke_admin_sessions", tenantId, Map.of("deletedSessions", deleted));
        return ApiResponse.ok(Map.of("deletedSessions", deleted));
    }

    /**
     * 一键开通专业增强版：续期 {@code pro_plus} 档位权益（默认 365 天，不产生订单）。
     * {@code grant-enterprise} 为旧路径，仍映射为 pro_plus。
     */
    @PostMapping({"/{tenantId}/grant-pro-plus", "/{tenantId}/grant-enterprise"})
    public ApiResponse<Map<String, Object>> grantProPlus(@PathVariable("tenantId") long tenantId,
                                                       @RequestBody(required = false) Map<String, Object> body,
                                                       HttpServletRequest request) {
        if (tenantMapper.findById(tenantId) == null) {
            return ApiResponse.error(40400, "租户不存在");
        }
        int days = 365;
        if (body != null && body.get("days") instanceof Number n) {
            days = n.intValue();
        }
        try {
            var ends = tenantBillingService.grantPlanWindow(tenantId, "pro_plus", days);
            audit(request, "tenant.grant_pro_plus", tenantId, Map.of("days", days, "subscriptionEndsAt", String.valueOf(ends)));
            return ApiResponse.ok(Map.of("planCode", "pro_plus", "subscriptionEndsAt", ends, "days", days));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

}
