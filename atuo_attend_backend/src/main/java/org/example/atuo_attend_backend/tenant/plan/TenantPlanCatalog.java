package org.example.atuo_attend_backend.tenant.plan;

/**
 * 套餐与配额（硬编码中间层，后续可换为数据库 + 支付回调更新 {@code aa_tenant.plan_code}）。
 * 付费专业档：{@code pro} 专业版低于 {@code pro_plus} 专业增强版（限额递增）。
 * 兼容：历史 {@code enterprise} 在 {@link #resolve} 中映射为 {@code pro_plus}。
 * <p>
 * 产品规则：用量超过当前档位上限时，已有资源可继续使用，仅禁止继续新建（见各 {@code assertCan*} 调用点）。
 */
public final class TenantPlanCatalog {

    /**
     * @param maxMembers             协作成员上限
     * @param maxGithubRepos         已绑定 GitHub 的报价项目上限
     * @param maxQuoteProjects       报价单（biz_quote_project）总数上限
     * @param maxClientBoardsEnabled 已启用客户看板的项目数上限
     * @param maxAgentSessions       Agent 会话创建总数（累计）
     * @param maxCollabProjects      协作项目（biz_project）总数上限
     * @param maxNexusAccounts       快捷运维云账号数上限
     */
    public record TenantPlan(
            String code,
            String label,
            int maxMembers,
            int maxGithubRepos,
            int maxQuoteProjects,
            int maxClientBoardsEnabled,
            int maxAgentSessions,
            int maxCollabProjects,
            int maxNexusAccounts) {
    }

    public static final TenantPlan FREE = new TenantPlan(
            "free", "免费版",
            20, 3,
            3, 3, 3, 5, 2);

    public static final TenantPlan TEAM = new TenantPlan(
            "team", "团队版",
            100, 20,
            50, 20, 200, 30, 20);

    /** 专业版（较低一档付费专业档） */
    public static final TenantPlan PRO = new TenantPlan(
            "pro", "专业版",
            500, 80,
            400, 80, 1500, 150, 40);

    /** 专业增强版（较高一档付费专业档） */
    public static final TenantPlan PRO_PLUS = new TenantPlan(
            "pro_plus", "专业增强版",
            10_000, 500,
            2000, 500, 5000, 500, 100);

    private TenantPlanCatalog() {
    }

    public static TenantPlan resolve(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return FREE;
        }
        return switch (planCode.trim().toLowerCase()) {
            case "team" -> TEAM;
            case "pro" -> PRO;
            case "pro_plus", "enterprise" -> PRO_PLUS;
            default -> FREE;
        };
    }

    public static String displayLabel(String planCode) {
        return resolve(planCode).label();
    }
}
