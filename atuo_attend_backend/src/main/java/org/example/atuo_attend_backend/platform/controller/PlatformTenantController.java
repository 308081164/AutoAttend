package org.example.atuo_attend_backend.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.platform.auth.PlatformAuthFilter;
import org.example.atuo_attend_backend.platform.dto.PlatformTenantOpsRow;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsAuditMapper;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsReportMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlatformTenantController(PlatformOpsReportMapper platformOpsReportMapper,
                                    TenantMapper tenantMapper,
                                    AdminSessionMapper adminSessionMapper,
                                    PlatformOpsAuditMapper platformOpsAuditMapper,
                                    SubscriptionOrderMapper subscriptionOrderMapper) {
        this.platformOpsReportMapper = platformOpsReportMapper;
        this.tenantMapper = tenantMapper;
        this.adminSessionMapper = adminSessionMapper;
        this.platformOpsAuditMapper = platformOpsAuditMapper;
        this.subscriptionOrderMapper = subscriptionOrderMapper;
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
        Map<String, Object> data = new HashMap<>();
        data.put("tenant", row);
        data.put("mrrApproxCents30d", mrrCents);
        data.put("activePaidSubscriptions", activeSubs);
        return ApiResponse.ok(data);
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
}
