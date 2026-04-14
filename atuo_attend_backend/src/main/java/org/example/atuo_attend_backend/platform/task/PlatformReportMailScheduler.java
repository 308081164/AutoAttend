package org.example.atuo_attend_backend.platform.task;

import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.report.service.ReportMailService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;

/**
 * 日报邮件：cron / 时区 / 开关从平台系统配置（tenant_id=0）读取，支持监测台保存后重新调度。
 */
@Component
public class PlatformReportMailScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlatformReportMailScheduler.class);

    private final SystemConfigService systemConfigService;
    private final ReportMailService reportMailService;
    private final ThreadPoolTaskScheduler platformReportMailTaskScheduler;

    private volatile ScheduledFuture<?> scheduledFuture;

    public PlatformReportMailScheduler(SystemConfigService systemConfigService,
                                       ReportMailService reportMailService,
                                       @Qualifier("platformReportMailTaskScheduler") ThreadPoolTaskScheduler platformReportMailTaskScheduler) {
        this.systemConfigService = systemConfigService;
        this.reportMailService = reportMailService;
        this.platformReportMailTaskScheduler = platformReportMailTaskScheduler;
    }

    @PostConstruct
    public void init() {
        reschedule();
    }

    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        if (!systemConfigService.getPlatformReportMailEnabled()) {
            log.info("Report mail scheduler: disabled by platform settings");
            return;
        }
        String cron = systemConfigService.getPlatformReportMailCron();
        String tzId = systemConfigService.getPlatformReportMailTimezone();
        ZoneId zone;
        try {
            zone = ZoneId.of(tzId);
        } catch (Exception e) {
            zone = ZoneId.of("Asia/Shanghai");
        }
        try {
            CronTrigger trigger = new CronTrigger(cron, zone);
            scheduledFuture = platformReportMailTaskScheduler.schedule(this::runJob, trigger);
            log.info("Report mail scheduler: registered cron={} zone={}", cron, zone);
        } catch (Exception e) {
            log.warn("Report mail scheduler: invalid cron '{}', not scheduled: {}", cron, e.getMessage());
        }
    }

    private void runJob() {
        ZoneId z;
        try {
            z = ZoneId.of(systemConfigService.getPlatformReportMailTimezone());
        } catch (Exception e) {
            z = ZoneId.of("Asia/Shanghai");
        }
        final ZoneId zone = z;
        try {
            TenantContext.runWithTenantId(TenantConstants.DEFAULT_TENANT_ID, () -> {
                int sent = reportMailService.sendForYesterday(zone);
                log.info("Report mail job done: sent={}", sent);
            });
        } catch (Exception e) {
            log.warn("Report mail job failed: {}", e.getMessage());
        }
    }
}
