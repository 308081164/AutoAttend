package org.example.atuo_attend_backend.report.task;

import org.example.atuo_attend_backend.report.service.ReportMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * 定时发送日报邮件（项目日报 + 开发者日报）。
 * 默认在 daily-summary 之后执行（见 application.properties：app.report-mail.cron）。
 */
@Component
public class ReportMailScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(ReportMailScheduledTask.class);

    private final ReportMailService reportMailService;

    @Value("${app.report-mail.enabled:true}")
    private boolean enabled;

    @Value("${app.report-mail.timezone:Asia/Shanghai}")
    private String timezoneId;

    public ReportMailScheduledTask(ReportMailService reportMailService) {
        this.reportMailService = reportMailService;
    }

    @Scheduled(cron = "${app.report-mail.cron:0 30 4 * * *}", zone = "${app.report-mail.timezone:Asia/Shanghai}")
    public void run() {
        if (!enabled) return;
        ZoneId zone;
        try {
            zone = ZoneId.of(timezoneId);
        } catch (Exception e) {
            zone = ZoneId.of("Asia/Shanghai");
        }
        try {
            int sent = reportMailService.sendForYesterday(zone);
            log.info("Report mail scheduled job done: sent={}", sent);
        } catch (Exception e) {
            log.warn("Report mail scheduled job failed: {}", e.getMessage());
        }
    }
}

