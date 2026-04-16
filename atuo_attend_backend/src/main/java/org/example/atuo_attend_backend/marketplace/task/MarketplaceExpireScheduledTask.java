package org.example.atuo_attend_backend.marketplace.task;

import org.example.atuo_attend_backend.marketplace.MarketplaceProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 到期自动关闭「招募中」项目。
 */
@Component
public class MarketplaceExpireScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceExpireScheduledTask.class);

    private final MarketplaceProjectService marketplaceProjectService;

    public MarketplaceExpireScheduledTask(MarketplaceProjectService marketplaceProjectService) {
        this.marketplaceProjectService = marketplaceProjectService;
    }

    @Scheduled(cron = "0 5 * * * *")
    public void run() {
        try {
            int n = marketplaceProjectService.runExpireJobReturn();
            if (n > 0) {
                log.info("Marketplace expire job closed {} projects", n);
            }
        } catch (Exception e) {
            log.warn("Marketplace expire job failed: {}", e.toString());
        }
    }
}
