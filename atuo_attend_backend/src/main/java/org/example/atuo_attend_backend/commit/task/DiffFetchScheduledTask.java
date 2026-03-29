package org.example.atuo_attend_backend.commit.task;

import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.commit.mapper.CommitMapper;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务：扫描尚未有 Diff 的提交，自动拉取并写入 aa_commit_diff。
 * 用于补全 webhook 拉取失败或历史无 diff 的提交，减轻「查看 Diff」时的按需拉取压力。
 */
@Component
public class DiffFetchScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(DiffFetchScheduledTask.class);

    private static final int FETCH_LIMIT = 20;

    private final CommitService commitService;
    private final TenantMapper tenantMapper;

    @Value("${app.diff-fetch.schedule.max-per-run:5}")
    private int maxPerRun;

    public DiffFetchScheduledTask(CommitService commitService, TenantMapper tenantMapper) {
        this.commitService = commitService;
        this.tenantMapper = tenantMapper;
    }

    @Scheduled(fixedDelayString = "${app.diff-fetch.schedule.fixed-delay-ms:900000}")
    public void runDiffFetch() {
        for (Tenant t : tenantMapper.listAll()) {
            TenantContext.runWithTenantId(t.getId(), () -> runDiffFetchForCurrentTenant());
        }
    }

    private void runDiffFetchForCurrentTenant() {
        int limit = maxPerRun <= 0 ? 5 : Math.min(maxPerRun, 20);
        List<CommitMapper.CommitId> withoutDiff = commitService.listCommitsWithoutDiff(FETCH_LIMIT);
        if (withoutDiff == null || withoutDiff.isEmpty()) {
            log.trace("Diff fetch schedule: no commits without diff");
            return;
        }
        int fetched = 0;
        for (CommitMapper.CommitId c : withoutDiff) {
            if (fetched >= limit) break;
            String repo = c.getRepoFullName();
            String sha = c.getCommitSha();
            if (repo == null || sha == null) continue;
            try {
                commitService.fetchAndSaveDiff(repo, sha);
                fetched++;
                log.info("Diff fetch scheduled: {}@{}", repo, sha.length() >= 7 ? sha.substring(0, 7) : sha);
            } catch (Exception e) {
                log.warn("Diff fetch failed for {}@{}: {}", repo, sha, e.getMessage());
            }
        }
        if (fetched > 0) {
            log.info("Diff fetch schedule finished: {} commit(s) fetched this round", fetched);
        }
    }
}
