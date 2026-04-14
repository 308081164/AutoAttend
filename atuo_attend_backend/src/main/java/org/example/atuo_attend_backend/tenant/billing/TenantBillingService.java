package org.example.atuo_attend_backend.tenant.billing;

import org.example.atuo_attend_backend.ai.official.OfficialAiPoolService;
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

    /** 尝鲜版（team）：¥49/月（模拟） */
    public static final int PRICE_TEAM_MONTHLY_CENTS = 4_900;
    /** 尝鲜版首月：¥19.9（每租户首次购买 team 时） */
    public static final int PRICE_TEAM_FIRST_MONTH_CENTS = 1_990;
    /** 兼容旧常量：与月价一致 */
    public static final int PRICE_TEAM_CENTS = PRICE_TEAM_MONTHLY_CENTS;
    /** 专业版：¥199/年（模拟订单金额，按年续期窗口） */
    public static final int PRICE_PRO_ANNUAL_CENTS = 19_900;
    /** 专业增强版：¥799/年 */
    public static final int PRICE_PRO_PLUS_ANNUAL_CENTS = 79_900;

    private final TenantMapper tenantMapper;
    private final OfficialAiPoolService officialAiPoolService;

    public TenantBillingService(TenantMapper tenantMapper, OfficialAiPoolService officialAiPoolService) {
        this.tenantMapper = tenantMapper;
        this.officialAiPoolService = officialAiPoolService;
    }

    private static int rank(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return 0;
        }
        return switch (planCode.trim().toLowerCase()) {
            case "pro_plus", "enterprise" -> 4;
            case "pro" -> 3;
            case "team" -> 1;
            default -> 0;
        };
    }

    private static String higherPlan(String a, String b) {
        return rank(a) >= rank(b) ? normalize(a) : normalize(b);
    }

    private static String normalize(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return TenantPlanCatalog.FREE.code();
        }
        String s = planCode.trim().toLowerCase();
        if ("enterprise".equals(s)) {
            return TenantPlanCatalog.PRO_PLUS.code();
        }
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
        officialAiPoolService.ensureWelcomeOfficialBalance(tenantId);
        return tenantMapper.findById(tenantId);
    }

    public int priceCentsForPlan(String planCode) {
        String p = normalize(planCode);
        return switch (p) {
            case "team" -> PRICE_TEAM_MONTHLY_CENTS;
            case "pro" -> PRICE_PRO_ANNUAL_CENTS;
            case "pro_plus" -> PRICE_PRO_PLUS_ANNUAL_CENTS;
            default -> 0;
        };
    }

    /** 当前租户购买尝鲜版应付标价（分）：首月优惠价或正价 */
    public int teamPurchaseListPriceCents(Tenant t) {
        if (t == null) {
            return PRICE_TEAM_MONTHLY_CENTS;
        }
        if (!Boolean.TRUE.equals(t.getTeamFirstMonthUsed())) {
            return PRICE_TEAM_FIRST_MONTH_CENTS;
        }
        return PRICE_TEAM_MONTHLY_CENTS;
    }

    @Transactional
    public LocalDateTime applyMockPurchase(long tenantId, String selectedPlanCode) {
        String sel = normalize(selectedPlanCode);
        if (!"team".equals(sel) && !"pro".equals(sel) && !"pro_plus".equals(sel)) {
            throw new IllegalArgumentException("仅支持购买 team、pro 或 pro_plus 档位");
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
        int addDays = "team".equals(sel) ? 30 : 365;

        LocalDateTime newEnd = baseStart.plusDays(addDays);

        tenantMapper.updatePlanSubscriptionAndBaseline(tenantId, effectivePlan, newEnd, baseline);
        if ("team".equals(sel) && !Boolean.TRUE.equals(t.getTeamFirstMonthUsed())) {
            tenantMapper.markTeamFirstMonthUsed(tenantId);
        }
        t.setPlanCode(effectivePlan);
        t.setSubscriptionEndsAt(newEnd);
        t.setBillingBaselinePlanCode(baseline);
        return newEnd;
    }

    /**
     * 平台运维：为指定租户开通/续期高阶档位（不产生订单）。
     */
    @Transactional
    public LocalDateTime grantPlanWindow(long tenantId, String planSel, int days) {
        if (days <= 0 || days > 3650) {
            throw new IllegalArgumentException("days 须在 1～3650 之间");
        }
        String sel = normalize(planSel);
        if (!"team".equals(sel) && !"pro".equals(sel) && !"pro_plus".equals(sel)) {
            throw new IllegalArgumentException("不支持的档位");
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
        LocalDateTime newEnd = baseStart.plusDays(days);
        tenantMapper.updatePlanSubscriptionAndBaseline(tenantId, effectivePlan, newEnd, baseline);
        return newEnd;
    }
}
