package org.example.atuo_attend_backend.nexus.task;

import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.domain.NexusAutoSyncConfig;
import org.example.atuo_attend_backend.nexus.mapper.NexusAutoSyncConfigMapper;
import org.example.atuo_attend_backend.nexus.mapper.NexusCloudAccountMapper;
import org.example.atuo_attend_backend.nexus.service.NexusSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NexusAutoSyncScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(NexusAutoSyncScheduledTask.class);
    private final NexusCloudAccountMapper accountMapper;
    private final NexusAutoSyncConfigMapper autoSyncConfigMapper;
    private final NexusSyncService syncService;

    private static final AtomicBoolean running = new AtomicBoolean(false);

    public NexusAutoSyncScheduledTask(
            NexusCloudAccountMapper accountMapper,
            NexusAutoSyncConfigMapper autoSyncConfigMapper,
            NexusSyncService syncService
    ) {
        this.accountMapper = accountMapper;
        this.autoSyncConfigMapper = autoSyncConfigMapper;
        this.syncService = syncService;
    }

    @Scheduled(fixedDelayString = "${nexus.scheduler.fixed-delay-ms:60000}")
    public void tick() {
        if (!running.compareAndSet(false, true)) return;
        try {
            Instant now = Instant.now();
            List<NexusCloudAccount> accounts = accountMapper.listAllForProvider("aliyun");
            if (accounts == null || accounts.isEmpty()) return;

            for (NexusCloudAccount a : accounts) {
                if (a == null) continue;
                NexusAutoSyncConfig cfg = autoSyncConfigMapper.findByTenant(a.getTenantId());
                if (cfg == null || !cfg.isEnabled()) continue;

                int intervalSeconds = a.getAutoSyncIntervalSeconds() != null ? a.getAutoSyncIntervalSeconds() : cfg.getGlobalIntervalSeconds();
                LocalDateTime last = a.getLastAutoSyncAt();
                boolean due = last == null || Duration.between(last, LocalDateTime.now()).getSeconds() >= intervalSeconds;
                if (!due) continue;

                log.info("[nexus] auto sync due: tenantId={}, accountId={}, intervalSeconds={}", a.getTenantId(), a.getId(), intervalSeconds);
                syncService.syncAccount(a.getTenantId(), a.getId(), "nexus.sync.auto", null, null);
            }
        } catch (Exception e) {
            log.warn("[nexus] auto sync tick failed: {}", e.getMessage());
        } finally {
            running.set(false);
        }
    }
}

