package org.example.atuo_attend_backend.tenant.task;

import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时将已过期的模拟订阅窗口回退到基线档位。
 */
@Component
public class SubscriptionBillingScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionBillingScheduledTask.class);

    private final TenantMapper tenantMapper;

    public SubscriptionBillingScheduledTask(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @Scheduled(fixedDelayString = "${app.subscription.expiry-check-ms:60000}")
    public void revertExpired() {
        try {
            int n = tenantMapper.revertAllExpiredSubscriptions();
            if (n > 0) {
                log.info("[billing] reverted expired subscription windows: {}", n);
            }
        } catch (Exception e) {
            log.warn("[billing] expiry revert failed: {}", e.getMessage());
        }
    }
}
