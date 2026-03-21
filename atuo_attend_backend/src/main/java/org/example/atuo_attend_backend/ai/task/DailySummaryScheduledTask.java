package org.example.atuo_attend_backend.ai.task;

import org.example.atuo_attend_backend.ai.service.ProjectDailySummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    public DailySummaryScheduledTask(ProjectDailySummaryService projectDailySummaryService) {
        this.projectDailySummaryService = projectDailySummaryService;
    }

    @Scheduled(cron = "${app.daily-summary.cron:0 0 4 * * *}", zone = "${app.daily-summary.timezone:Asia/Shanghai}")
    public void runDailySummaries() {
        try {
            int n = projectDailySummaryService.runSummariesForYesterday();
            if (n > 0) {
                log.info("Daily project summaries job finished: {} repo(s) processed", n);
            }
        } catch (Exception e) {
            log.warn("Daily project summaries job failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
