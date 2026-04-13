package org.example.atuo_attend_backend.tenant.plan;

/**
 * 套餐与配额（硬编码中间层，后续可换为数据库 + 支付回调更新 {@code aa_tenant.plan_code}）。
 * {@code pro} 与 {@code enterprise} 数值一致，标签区分为「专业版」与「企业版」。
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

    private static final int E_MAX_M = 10_000;
    private static final int E_MAX_GH = 500;
    private static final int E_MAX_QUOTE = 2000;
    private static final int E_MAX_BOARD = 500;
    private static final int E_MAX_AGENT = 5000;
    private static final int E_MAX_COLLAB = 500;
    private static final int E_MAX_NEXUS = 100;

    public static final TenantPlan FREE = new TenantPlan(
            "free", "免费版",
            20, 3,
            3, 3, 3, 5, 2);

    public static final TenantPlan TEAM = new TenantPlan(
            "team", "团队版",
            100, 20,
            50, 20, 200, 30, 20);

    public static final TenantPlan PRO = new TenantPlan(
            "pro", "专业版",
            E_MAX_M, E_MAX_GH, E_MAX_QUOTE, E_MAX_BOARD, E_MAX_AGENT, E_MAX_COLLAB, E_MAX_NEXUS);

    public static final TenantPlan ENTERPRISE = new TenantPlan(
            "enterprise", "企业版",
            E_MAX_M, E_MAX_GH, E_MAX_QUOTE, E_MAX_BOARD, E_MAX_AGENT, E_MAX_COLLAB, E_MAX_NEXUS);

    private TenantPlanCatalog() {
    }

    public static TenantPlan resolve(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return FREE;
        }
        return switch (planCode.trim().toLowerCase()) {
            case "team" -> TEAM;
            case "pro" -> PRO;
            case "enterprise" -> ENTERPRISE;
            default -> FREE;
        };
    }

    public static String displayLabel(String planCode) {
        return resolve(planCode).label();
    }
}
