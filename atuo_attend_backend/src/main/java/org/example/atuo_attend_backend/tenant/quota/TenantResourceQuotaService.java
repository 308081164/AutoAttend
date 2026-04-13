package org.example.atuo_attend_backend.tenant.quota;

import org.example.atuo_attend_backend.agent.mapper.AgentSessionMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectClientBoardMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.nexus.mapper.NexusCloudAccountMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.billing.TenantBillingService;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.plan.TenantPlanCatalog;
import org.springframework.stereotype.Service;

/**
 * 租户各业务资源配额（报价单数、客户看板、Agent、协作项目、快捷运维账号等）。
 */
@Service
public class TenantResourceQuotaService {

    private final TenantBillingService tenantBillingService;
    private final SystemConfigService systemConfigService;
    private final QuoteProjectMapper quoteProjectMapper;
    private final BizProjectClientBoardMapper clientBoardMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final BizProjectMapper bizProjectMapper;
    private final NexusCloudAccountMapper nexusCloudAccountMapper;

    public TenantResourceQuotaService(TenantBillingService tenantBillingService,
                                      SystemConfigService systemConfigService,
                                      QuoteProjectMapper quoteProjectMapper,
                                      BizProjectClientBoardMapper clientBoardMapper,
                                      AgentSessionMapper agentSessionMapper,
                                      BizProjectMapper bizProjectMapper,
                                      NexusCloudAccountMapper nexusCloudAccountMapper) {
        this.tenantBillingService = tenantBillingService;
        this.systemConfigService = systemConfigService;
        this.quoteProjectMapper = quoteProjectMapper;
        this.clientBoardMapper = clientBoardMapper;
        this.agentSessionMapper = agentSessionMapper;
        this.bizProjectMapper = bizProjectMapper;
        this.nexusCloudAccountMapper = nexusCloudAccountMapper;
    }

    private TenantPlanCatalog.TenantPlan effectivePlan(long tenantId) {
        Tenant t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            return TenantPlanCatalog.FREE;
        }
        return resolvePlanWithOverrides(t.getPlanCode());
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

    public void assertCanCreateQuoteProject(long tenantId) {
        Tenant t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            throw new IllegalArgumentException("组织已暂停服务，请联系平台支持");
        }
        var plan = resolvePlanWithOverrides(t.getPlanCode());
        long n = quoteProjectMapper.countAll(tenantId);
        if (n >= plan.maxQuoteProjects()) {
            throw new IllegalArgumentException("报价单数量已达当前套餐上限（" + plan.maxQuoteProjects()
                    + "），请升级套餐或删除无用报价单后再试。");
        }
    }

    public void assertCanEnableClientBoard(long tenantId, boolean wasEnabled, boolean willEnable) {
        if (!willEnable || wasEnabled) {
            return;
        }
        Tenant t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            throw new IllegalArgumentException("组织已暂停服务，请联系平台支持");
        }
        var plan = resolvePlanWithOverrides(t.getPlanCode());
        long n = clientBoardMapper.countEnabledByTenant(tenantId);
        if (n >= plan.maxClientBoardsEnabled()) {
            throw new IllegalArgumentException("已启用的客户看板数量已达当前套餐上限（" + plan.maxClientBoardsEnabled()
                    + "），请升级套餐或关闭部分看板后再试。");
        }
    }

    public void assertCanCreateAgentSession(long tenantId) {
        Tenant t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            throw new IllegalArgumentException("组织已暂停服务，请联系平台支持");
        }
        var plan = resolvePlanWithOverrides(t.getPlanCode());
        long n = agentSessionMapper.countByTenant(tenantId);
        if (n >= plan.maxAgentSessions()) {
            throw new IllegalArgumentException("Agent 需求引导会话数已达当前套餐上限（" + plan.maxAgentSessions()
                    + "），请升级套餐后再试。");
        }
    }

    public void assertCanCreateCollabProject(long tenantId) {
        Tenant t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            throw new IllegalArgumentException("组织已暂停服务，请联系平台支持");
        }
        var plan = resolvePlanWithOverrides(t.getPlanCode());
        long n = bizProjectMapper.countByTenant(tenantId);
        if (n >= plan.maxCollabProjects()) {
            throw new IllegalArgumentException("协作项目（项目管理）数量已达当前套餐上限（" + plan.maxCollabProjects()
                    + "），请升级套餐后再试。");
        }
    }

    public void assertCanCreateNexusAccount(long tenantId) {
        Tenant t = tenantBillingService.ensureCurrent(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            throw new IllegalArgumentException("组织已暂停服务，请联系平台支持");
        }
        var plan = resolvePlanWithOverrides(t.getPlanCode());
        long n = nexusCloudAccountMapper.countByTenant(tenantId);
        if (n >= plan.maxNexusAccounts()) {
            throw new IllegalArgumentException("快捷运维云账号数量已达当前套餐上限（" + plan.maxNexusAccounts()
                    + "），请升级套餐后再试。");
        }
    }

    /** 供前端展示当前档位下的各上限（与 assert 使用同一套 resolve） */
    public TenantPlanCatalog.TenantPlan planForTenant(long tenantId) {
        return effectivePlan(tenantId);
    }

    public java.util.Map<String, Long> usageSnapshot(long tenantId) {
        java.util.Map<String, Long> m = new java.util.HashMap<>();
        m.put("quoteProjects", quoteProjectMapper.countAll(tenantId));
        m.put("clientBoardsEnabled", clientBoardMapper.countEnabledByTenant(tenantId));
        m.put("agentSessions", agentSessionMapper.countByTenant(tenantId));
        m.put("collabProjects", bizProjectMapper.countByTenant(tenantId));
        m.put("nexusAccounts", nexusCloudAccountMapper.countByTenant(tenantId));
        return m;
    }
}
