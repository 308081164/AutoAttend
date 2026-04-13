package org.example.atuo_attend_backend.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.billing.TenantBillingService;
import org.example.atuo_attend_backend.tenant.plan.TenantPlanCatalog;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作台：当前租户权益与用量摘要。
 */
@RestController
@RequestMapping("/api/admin/workspace")
public class AdminWorkspaceController {

    private final TenantBillingService tenantBillingService;
    private final TenantResourceQuotaService tenantResourceQuotaService;

    public AdminWorkspaceController(TenantBillingService tenantBillingService,
                                    TenantResourceQuotaService tenantResourceQuotaService) {
        this.tenantBillingService = tenantBillingService;
        this.tenantResourceQuotaService = tenantResourceQuotaService;
    }

    private static long tenantId(HttpServletRequest request) {
        Object v = request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        return v instanceof Long ? (Long) v : 0L;
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        var t = tenantBillingService.ensureCurrent(tid);
        if (t == null) {
            return ApiResponse.error(40400, "租户不存在");
        }
        var plan = tenantResourceQuotaService.planForTenant(tid);
        Map<String, Object> data = new HashMap<>();
        data.put("planCode", t.getPlanCode());
        data.put("planDisplayLabel", TenantPlanCatalog.displayLabel(t.getPlanCode()));
        data.put("subscriptionEndsAt", t.getSubscriptionEndsAt());
        data.put("billingBaselinePlanCode", t.getBillingBaselinePlanCode());
        data.put("status", t.getStatus());
        data.put("limits", quotaMap(plan));
        data.put("usage", tenantResourceQuotaService.usageSnapshot(tid));
        return ApiResponse.ok(data);
    }

    private static Map<String, Object> quotaMap(TenantPlanCatalog.TenantPlan p) {
        Map<String, Object> m = new HashMap<>();
        m.put("maxMembers", p.maxMembers());
        m.put("maxGithubRepos", p.maxGithubRepos());
        m.put("maxQuoteProjects", p.maxQuoteProjects());
        m.put("maxClientBoardsEnabled", p.maxClientBoardsEnabled());
        m.put("maxAgentSessions", p.maxAgentSessions());
        m.put("maxCollabProjects", p.maxCollabProjects());
        m.put("maxNexusAccounts", p.maxNexusAccounts());
        return m;
    }
}
