package org.example.atuo_attend_backend.ai.task;

import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.ai.service.AiAnalysisService;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 定时任务：定期扫描近期提交，对尚未有 AI 分析结果且已有 Diff 的提交自动执行分析并落库。
 * 仅在「AI 分析」已启用且已配置 API Key 时执行；每轮最多处理条数受限，避免瞬时耗满配额。
 */
@Component
public class AiAnalysisScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisScheduledTask.class);

    /** 每次调度拉取的最近提交数量 */
    private static final int FETCH_COMMITS_LIMIT = 50;
    /** 每轮最多执行分析的提交数（控制 DeepSeek 调用频率与成本） */
    private static final int MAX_ANALYSIS_PER_RUN = 3;

    private final AiAnalysisConfigService configService;
    private final AiAnalysisService analysisService;
    private final CommitService commitService;
    private final TenantMapper tenantMapper;

    public AiAnalysisScheduledTask(AiAnalysisConfigService configService,
                                  AiAnalysisService analysisService,
                                  CommitService commitService,
                                  TenantMapper tenantMapper) {
        this.configService = configService;
        this.analysisService = analysisService;
        this.commitService = commitService;
        this.tenantMapper = tenantMapper;
    }

    @Scheduled(fixedDelayString = "${app.ai-analysis.schedule.fixed-delay-ms:600000}")
    public void runAutoAnalysis() {
        for (Tenant t : tenantMapper.listAll()) {
            TenantContext.runWithTenantId(t.getId(), this::runAutoAnalysisForCurrentTenant);
        }
    }

    private void runAutoAnalysisForCurrentTenant() {
        AiAnalysisConfig config = configService.getConfig();
        if (!Boolean.TRUE.equals(config.getEnabled()) || config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.trace("AI auto-analysis skipped: disabled or no API key");
            return;
        }
        List<CommitRecord> recent = commitService.listPaged(1, FETCH_COMMITS_LIMIT);
        int analyzed = 0;
        for (CommitRecord c : recent) {
            if (analyzed >= MAX_ANALYSIS_PER_RUN) break;
            String repo = c.getRepoFullName();
            String sha = c.getCommitSha();
            if (repo == null || sha == null) continue;
            if (analysisService.getResult(repo, sha).isPresent()) continue;
            Optional<AiAnalysisResult> result = analysisService.runAnalysis(repo, sha);
            if (result.isPresent()) {
                analyzed++;
                log.info("AI auto-analysis done for {}@{}", repo, sha.substring(0, Math.min(7, sha.length())));
            }
        }
        if (analyzed > 0) {
            log.info("AI auto-analysis run finished: {} commit(s) analyzed this round", analyzed);
        }
    }
}
