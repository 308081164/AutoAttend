package org.example.atuo_attend_backend.agent.service;

import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.BackgroundTextItem;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 获客链接「快速创建报价 + Agent 会话」：超量时仍服务客户，对租户隐藏超量项目。
 */
@Service
public class PublicAgentQuickStartService {

    private final QuoteProjectMapper projectMapper;
    private final AgentSessionService sessionService;
    private final TenantResourceQuotaService tenantResourceQuotaService;

    public PublicAgentQuickStartService(QuoteProjectMapper projectMapper,
                                        AgentSessionService sessionService,
                                        TenantResourceQuotaService tenantResourceQuotaService) {
        this.projectMapper = projectMapper;
        this.sessionService = sessionService;
        this.tenantResourceQuotaService = tenantResourceQuotaService;
    }

    public record QuickStartResult(long projectId, String publicToken, String projectName, String quoteKind) {}

    @Transactional(rollbackFor = Exception.class)
    public QuickStartResult quickStart(long tenantId, String projectName, String quoteKind) {
        tenantResourceQuotaService.assertCanCreateAgentSessionViaPublicQuickStart(tenantId);

        boolean quotaLocked = tenantResourceQuotaService.shouldQuotaLockNewQuickStartProject(tenantId);

        QuoteProject project = new QuoteProject();
        project.setTenantId(tenantId);
        project.setName(projectName);
        project.setProjectType("other");
        project.setTechStack("vue_node");
        project.setDesignType("need_design");
        project.setDataMigration("none");
        project.setConcurrency("lt100");
        project.setSecurityLevel("normal");
        project.setDeployType("cloud");
        project.setStatus("draft");
        project.setQuoteKind(quoteKind);
        project.setQuoteSubjectMode("legal_entity");
        project.setQuotaLocked(quotaLocked);
        projectMapper.insert(project);
        long projectId = project.getId();

        List<BackgroundTextItem> backgrounds = new ArrayList<>();
        BackgroundTextItem bt1 = new BackgroundTextItem();
        bt1.setLabel("项目名称");
        bt1.setContent(projectName);
        backgrounds.add(bt1);
        BackgroundTextItem bt2 = new BackgroundTextItem();
        bt2.setLabel("报价模式");
        bt2.setContent("single".equals(quoteKind) ? "单体应用" : "解决方案级");
        backgrounds.add(bt2);

        AgentSession session = sessionService.createSession(
                tenantId, projectId, null, backgrounds, null, true);

        return new QuickStartResult(projectId, session.getPublicToken(), projectName, quoteKind);
    }
}
