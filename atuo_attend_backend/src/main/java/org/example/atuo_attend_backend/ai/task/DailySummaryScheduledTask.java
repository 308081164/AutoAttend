package org.example.atuo_attend_backend.ai.task;

import org.example.atuo_attend_backend.ai.service.ProjectDailySummaryService;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日凌晨按配置时区执行：为昨日有提交的各仓库生成「项目每日进展总结」（需 AI 配置中开启开关且已填 API Key）。
 */
@Component
@ConditionalOnProperty(name = "app.daily-summary.enabled", havingValue = "true", matchIfMissing = true)
public class DailySummaryScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryScheduledTask.class);

    private final ProjectDailySummaryService projectDailySummaryService;
    private final TenantMapper tenantMapper;

    public DailySummaryScheduledTask(ProjectDailySummaryService projectDailySummaryService,
                                     TenantMapper tenantMapper) {
        this.projectDailySummaryService = projectDailySummaryService;
        this.tenantMapper = tenantMapper;
    }

    @Scheduled(cron = "${app.daily-summary.cron:0 0 4 * * *}", zone = "${app.daily-summary.timezone:Asia/Shanghai}")
    public void runDailySummaries() {
        try {
            AtomicInteger total = new AtomicInteger();
            for (Tenant t : tenantMapper.listAll()) {
                TenantContext.runWithTenantId(t.getId(), () ->
                        total.addAndGet(projectDailySummaryService.runSummariesForYesterday()));
            }
            if (total.get() > 0) {
                log.info("Daily project summaries job finished: {} repo(s) processed (all tenants)", total.get());
            }
        } catch (Exception e) {
            log.warn("Daily project summaries job failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
