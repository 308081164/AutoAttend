package org.example.atuo_attend_backend.tenant.billing;

import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.tenant.plan.TenantPlanCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订阅权益：模拟支付窗口、到期回退基线档位；配额读取前应先 {@link #ensureCurrent(Tenant)}。
 */
@Service
public class TenantBillingService {

    public static final int PRICE_TEAM_CENTS = 9_900;
    public static final int PRICE_PRO_CENTS = 29_900;

    private final TenantMapper tenantMapper;

    public TenantBillingService(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    private static int rank(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return 0;
        }
        return switch (planCode.trim().toLowerCase()) {
            case "pro" -> 2;
            case "team" -> 1;
            default -> 0;
        };
    }

    private static String higherPlan(String a, String b) {
        return rank(a) >= rank(b) ? normalize(a) : normalize(b);
    }

    private static String normalize(String planCode) {
        TenantPlanCatalog.TenantPlan p = TenantPlanCatalog.resolve(planCode);
        return p.code();
    }

    /**
     * 若订阅已过期，将 {@code plan_code} 回退到 {@code billing_baseline_plan_code} 并清空截止时间。
     */
    public Tenant ensureCurrent(Tenant t) {
        if (t == null) {
            return null;
        }
        LocalDateTime ends = t.getSubscriptionEndsAt();
        if (ends == null) {
            return t;
        }
        if (!ends.isBefore(LocalDateTime.now())) {
            return t;
        }
        String baseline = t.getBillingBaselinePlanCode();
        if (baseline == null || baseline.isBlank()) {
            baseline = "free";
        }
        baseline = normalize(baseline);
        tenantMapper.revertExpiredSubscription(t.getId(), baseline);
        t.setPlanCode(baseline);
        t.setSubscriptionEndsAt(null);
        return t;
    }

    public Tenant ensureCurrent(long tenantId) {
        Tenant t = tenantMapper.findById(tenantId);
        if (t == null) {
            return null;
        }
        ensureCurrent(t);
        return tenantMapper.findById(tenantId);
    }

    public int priceCentsForPlan(String planCode) {
        String p = normalize(planCode);
        return switch (p) {
            case "team" -> PRICE_TEAM_CENTS;
            case "pro" -> PRICE_PRO_CENTS;
            default -> 0;
        };
    }

    @Transactional
    public LocalDateTime applyMockPurchase(long tenantId, String selectedPlanCode) {
        String sel = normalize(selectedPlanCode);
        if (!"team".equals(sel) && !"pro".equals(sel)) {
            throw new IllegalArgumentException("仅支持购买 team 或 pro 档位");
        }
        Tenant t = tenantMapper.findById(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        ensureCurrent(t);
        String baseline = t.getBillingBaselinePlanCode();
        if (baseline == null || baseline.isBlank()) {
            baseline = "free";
        }
        baseline = normalize(baseline);

        String effectivePlan = higherPlan(t.getPlanCode(), sel);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime prevEnd = t.getSubscriptionEndsAt();
        LocalDateTime baseStart = prevEnd != null && prevEnd.isAfter(now) ? prevEnd : now;
        LocalDateTime newEnd = baseStart.plusDays(1);

        tenantMapper.updatePlanSubscriptionAndBaseline(tenantId, effectivePlan, newEnd, baseline);
        t.setPlanCode(effectivePlan);
        t.setSubscriptionEndsAt(newEnd);
        t.setBillingBaselinePlanCode(baseline);
        return newEnd;
    }
}
