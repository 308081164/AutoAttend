package org.example.atuo_attend_backend.tenant.quota;

import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.tenant.plan.TenantPlanCatalog;
import org.springframework.stereotype.Service;

/**
 * 租户套餐配额校验（成员数、已绑定 GitHub 仓库的报价项目数等）。
 */
@Service
public class TenantQuotaService {

    private final TenantMapper tenantMapper;
    private final BizUserMapper bizUserMapper;
    private final QuoteProjectMapper quoteProjectMapper;

    public TenantQuotaService(TenantMapper tenantMapper,
                              BizUserMapper bizUserMapper,
                              QuoteProjectMapper quoteProjectMapper) {
        this.tenantMapper = tenantMapper;
        this.bizUserMapper = bizUserMapper;
        this.quoteProjectMapper = quoteProjectMapper;
    }

    public void assertCanAddMember(long tenantId) {
        var t = tenantMapper.findById(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            throw new IllegalArgumentException("组织已暂停服务，请联系平台支持");
        }
        var plan = TenantPlanCatalog.resolve(t.getPlanCode());
        long n = bizUserMapper.countByTenant(tenantId);
        if (n >= plan.maxMembers()) {
            throw new IllegalArgumentException("协作成员数已达当前套餐上限（" + plan.maxMembers() + "），请升级套餐或联系管理员。");
        }
    }

    public void assertCanLinkGithubRepo(long tenantId) {
        var t = tenantMapper.findById(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if ("suspended".equalsIgnoreCase(t.getStatus())) {
            throw new IllegalArgumentException("组织已暂停服务，请联系平台支持");
        }
        var plan = TenantPlanCatalog.resolve(t.getPlanCode());
        long linked = quoteProjectMapper.countGithubLinkedByTenant(tenantId);
        if (linked >= plan.maxGithubRepos()) {
            throw new IllegalArgumentException("已绑定 GitHub 仓库数量已达当前套餐上限（" + plan.maxGithubRepos() + "），请升级套餐。");
        }
    }
}
