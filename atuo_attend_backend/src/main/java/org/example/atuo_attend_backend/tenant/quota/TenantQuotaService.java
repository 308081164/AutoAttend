package org.example.atuo_attend_backend.tenant.quota;

import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.billing.TenantBillingService;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.tenant.plan.TenantPlanCatalog;
import org.springframework.stereotype.Service;

/**
 * 租户套餐配额校验（成员数、已绑定 GitHub 仓库的报价项目数等）。
 */
@Service
public class TenantQuotaService {
    public record QuotaDecision(boolean allowed, String message, long currentUsage, int limit) {}

    private final TenantMapper tenantMapper;
    private final BizUserMapper bizUserMapper;
    private final QuoteProjectMapper quoteProjectMapper;
    private final SystemConfigService systemConfigService;
    private final TenantBillingService tenantBillingService;

    public TenantQuotaService(TenantMapper tenantMapper,
                              BizUserMapper bizUserMapper,
                              QuoteProjectMapper quoteProjectMapper,
                              SystemConfigService systemConfigService,
                              TenantBillingService tenantBillingService) {
        this.tenantMapper = tenantMapper;
        this.bizUserMapper = bizUserMapper;
        this.quoteProjectMapper = quoteProjectMapper;
        this.systemConfigService = systemConfigService;
        this.tenantBillingService = tenantBillingService;
    }

    public void assertCanAddMember(long tenantId) {
        QuotaDecision d = checkCanAddMember(tenantId);
        if (!d.allowed()) {
            throw new IllegalArgumentException(d.message());
        }
    }

    public QuotaDecision checkCanAddMember(long tenantId) {
        var t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            return new QuotaDecision(false, "租户不存在", 0, 0);
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            return new QuotaDecision(false, "组织已暂停服务，请联系平台支持", 0, 0);
        }
        var plan = resolvePlanWithOverrides(t.getPlanCode());
        long n = bizUserMapper.countByTenant(tenantId);
        if (n >= plan.maxMembers()) {
            return new QuotaDecision(false, "协作成员数已达当前套餐上限（" + plan.maxMembers() + "），已创建成员可继续使用，但暂不可新增，请及时升级/续费。", n, plan.maxMembers());
        }
        return new QuotaDecision(true, "", n, plan.maxMembers());
    }

    public void assertCanLinkGithubRepo(long tenantId) {
        QuotaDecision d = checkCanLinkGithubRepo(tenantId);
        if (!d.allowed()) {
            throw new IllegalArgumentException(d.message());
        }
    }

    public QuotaDecision checkCanLinkGithubRepo(long tenantId) {
        var t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            return new QuotaDecision(false, "租户不存在", 0, 0);
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            return new QuotaDecision(false, "组织已暂停服务，请联系平台支持", 0, 0);
        }
        var plan = resolvePlanWithOverrides(t.getPlanCode());
        long linked = quoteProjectMapper.countGithubLinkedByTenant(tenantId);
        if (linked >= plan.maxGithubRepos()) {
            return new QuotaDecision(false, "已绑定 GitHub 仓库数量已达当前套餐上限（" + plan.maxGithubRepos() + "），已创建仓库可继续使用，但暂不可新增，请及时升级/续费。", linked, plan.maxGithubRepos());
        }
        return new QuotaDecision(true, "", linked, plan.maxGithubRepos());
    }

    private TenantPlanCatalog.TenantPlan resolvePlanWithOverrides(String planCode) {
        TenantPlanCatalog.TenantPlan base = TenantPlanCatalog.resolve(planCode);
        String code = base.code();
        int maxMembers = base.maxMembers();
        int maxGithubRepos = base.maxGithubRepos();
        if ("free".equalsIgnoreCase(code)) {
            maxMembers = nvlPositive(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_FREE_MAX_MEMBERS), maxMembers);
            maxGithubRepos = nvlPositive(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_FREE_MAX_GITHUB_REPOS), maxGithubRepos);
        } else if ("team".equalsIgnoreCase(code)) {
            maxMembers = nvlPositive(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_TEAM_MAX_MEMBERS), maxMembers);
            maxGithubRepos = nvlPositive(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_TEAM_MAX_GITHUB_REPOS), maxGithubRepos);
        } else if ("pro".equalsIgnoreCase(code) || "enterprise".equalsIgnoreCase(code)) {
            maxMembers = nvlPositive(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_PRO_MAX_MEMBERS), maxMembers);
            maxGithubRepos = nvlPositive(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_PRO_MAX_GITHUB_REPOS), maxGithubRepos);
        }
        return new TenantPlanCatalog.TenantPlan(
                base.code(), base.label(),
                maxMembers, maxGithubRepos,
                base.maxQuoteProjects(), base.maxClientBoardsEnabled(),
                base.maxAgentSessions(), base.maxCollabProjects(), base.maxNexusAccounts());
    }

    private static int nvlPositive(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }
}
