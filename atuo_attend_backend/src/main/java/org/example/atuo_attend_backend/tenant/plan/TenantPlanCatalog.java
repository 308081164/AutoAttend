package org.example.atuo_attend_backend.tenant.plan;

/**
 * 套餐与配额（硬编码中间层，后续可换为数据库 + 支付回调更新 {@code aa_tenant.plan_code}）。
 */
public final class TenantPlanCatalog {

    public record TenantPlan(String code, String label, int maxMembers, int maxGithubRepos) {
    }

    public static final TenantPlan FREE = new TenantPlan("free", "免费版", 20, 3);
    public static final TenantPlan TEAM = new TenantPlan("team", "团队版", 100, 20);
    public static final TenantPlan PRO = new TenantPlan("pro", "专业版", 10_000, 500);

    private TenantPlanCatalog() {
    }

    public static TenantPlan resolve(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return FREE;
        }
        return switch (planCode.trim().toLowerCase()) {
            case "team" -> TEAM;
            case "pro" -> PRO;
            default -> FREE;
        };
    }
}
