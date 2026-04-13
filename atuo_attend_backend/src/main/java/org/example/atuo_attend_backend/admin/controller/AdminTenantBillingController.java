package org.example.atuo_attend_backend.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.billing.TenantBillingService;
import org.example.atuo_attend_backend.tenant.mapper.SubscriptionOrderMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.tenant.plan.TenantPlanCatalog;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 租户侧：模拟支付开通会员（不跳转第三方支付）。
 */
@RestController
@RequestMapping("/api/admin/billing")
public class AdminTenantBillingController {

    private final TenantMapper tenantMapper;
    private final TenantBillingService tenantBillingService;
    private final SubscriptionOrderMapper subscriptionOrderMapper;

    public AdminTenantBillingController(TenantMapper tenantMapper,
                                        TenantBillingService tenantBillingService,
                                        SubscriptionOrderMapper subscriptionOrderMapper) {
        this.tenantMapper = tenantMapper;
        this.tenantBillingService = tenantBillingService;
        this.subscriptionOrderMapper = subscriptionOrderMapper;
    }

    private static long tenantId(HttpServletRequest request) {
        Object v = request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        return v instanceof Long ? (Long) v : 0L;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        var t = tenantBillingService.ensureCurrent(tid);
        if (t == null) {
            return ApiResponse.error(40400, "租户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("planCode", t.getPlanCode());
        data.put("billingBaselinePlanCode", t.getBillingBaselinePlanCode());
        data.put("subscriptionEndsAt", t.getSubscriptionEndsAt());
        data.put("teamPriceCents", TenantBillingService.PRICE_TEAM_CENTS);
        data.put("proPriceCents", TenantBillingService.PRICE_PRO_CENTS);
        return ApiResponse.ok(data);
    }

    @PostMapping("/mock-pay")
    public ApiResponse<Map<String, Object>> mockPay(@RequestBody Map<String, String> body,
                                                    HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        String plan = body != null ? body.get("planCode") : null;
        try {
            LocalDateTime ends = tenantBillingService.applyMockPurchase(tid, plan);
            var t = tenantMapper.findById(tid);
            String purchased = TenantPlanCatalog.resolve(plan).code();
            int cents = tenantBillingService.priceCentsForPlan(purchased);
            subscriptionOrderMapper.insert(tid, purchased, cents, "CNY", "mock", "completed");
            Map<String, Object> data = new HashMap<>();
            data.put("planCode", t != null ? t.getPlanCode() : plan);
            data.put("subscriptionEndsAt", ends);
            data.put("message", "模拟支付成功，已开通 24 小时权益（无真实扣款）");
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }
}
