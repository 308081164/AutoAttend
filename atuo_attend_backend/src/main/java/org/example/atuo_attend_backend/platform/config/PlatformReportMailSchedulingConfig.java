package org.example.atuo_attend_backend.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 日报邮件定时任务专用调度器（cron 从平台配置表读取并可热更新）。
 */
@Configuration
public class PlatformReportMailSchedulingConfig {

    @Bean(name = "platformReportMailTaskScheduler")
    public ThreadPoolTaskScheduler platformReportMailTaskScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(1);
        s.setThreadNamePrefix("report-mail-");
        s.initialize();
        return s;
    }
}
