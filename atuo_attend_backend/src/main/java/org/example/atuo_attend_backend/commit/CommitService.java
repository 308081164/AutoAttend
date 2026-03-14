package org.example.atuo_attend_backend.commit;

import org.example.atuo_attend_backend.commit.mapper.CommitDiffMapper;
import org.example.atuo_attend_backend.commit.mapper.CommitMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class CommitService {

    private static final Logger log = LoggerFactory.getLogger(CommitService.class);
    /** 重试次数：首次拉取 + 后续自动重试 */
    private static final int DIFF_FETCH_RETRY_TIMES = 6;
    /** 每次尝试前的等待毫秒数（首次 0，后续逐步延长，温和退避） */
    private static final long[] DIFF_FETCH_BACKOFF_MS = { 0, 1000, 2000, 4000, 8000, 16000 };

    private final CommitMapper commitMapper;
    private final CommitDiffMapper commitDiffMapper;
    private final GithubDiffFetcher githubDiffFetcher;

    public CommitService(CommitMapper commitMapper, CommitDiffMapper commitDiffMapper, GithubDiffFetcher githubDiffFetcher) {
        this.commitMapper = commitMapper;
        this.commitDiffMapper = commitDiffMapper;
        this.githubDiffFetcher = githubDiffFetcher;
    }

    public void saveCommit(String repoFullName,
                           String commitSha,
                           String authorName,
                           String authorEmail,
                           String timestamp,
                           String message,
                           String diffText) {
        CommitRecord record = new CommitRecord();
        record.setRepoFullName(repoFullName);
        record.setCommitSha(commitSha);
        record.setAuthorName(authorName);
        record.setAuthorEmail(authorEmail);
        record.setMessage(message);
        try {
            record.setCommittedAt(OffsetDateTime.parse(timestamp));
        } catch (DateTimeParseException e) {
            record.setCommittedAt(OffsetDateTime.now());
        }
        record.setValidCommit(true);
        record.setValidReason("MVP: always valid");
        try {
            commitMapper.insert(record);
        } catch (Exception ignoreDuplicate) {
            // unique(repo_full_name, commit_sha) 可能重复投递 webhook，忽略即可
        }
        if (diffText != null) {
            long size = diffText.getBytes().length;
            try {
                commitDiffMapper.insert(repoFullName, commitSha, diffText, size);
            } catch (Exception ignoreDuplicate) {
                // diff 同样可能重复插入
            }
        }
    }

    /** 当无法从库或 GitHub 获取 diff 时返回的提示，便于前端展示原因 */
    private static final String DIFF_UNAVAILABLE_PLACEHOLDER =
        "(Diff 暂不可用：请配置服务器环境变量 GITHUB_TOKEN 后重试，或检查网络与 GitHub API 限流。详见部署说明。)";

    /** 仅按 commitSha 查一条记录（用于 getDiff 时未传 repo 的兜底），不拉取 diff。 */
    public Optional<CommitRecord> findAnyCommitBySha(String commitSha) {
        if (commitSha == null || commitSha.isBlank()) return Optional.empty();
        CommitRecord r = commitMapper.findOneByCommitSha(commitSha);
        return r != null ? Optional.of(r) : Optional.empty();
    }

    public Optional<CommitRecord> findCommit(String repoFullName, String commitSha) {
        CommitRecord record = commitMapper.findOne(repoFullName, commitSha);
        if (record == null) {
            return Optional.empty();
        }
        String diffText = commitDiffMapper.findDiffText(repoFullName, commitSha);
        if (diffText == null || diffText.isBlank()) {
            fetchAndSaveDiffWithRetry(repoFullName, commitSha);
            diffText = commitDiffMapper.findDiffText(repoFullName, commitSha);
        }
        if (diffText == null || diffText.isBlank()) {
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            if (trySaveFetchedDiff(repoFullName, commitSha)) {
                diffText = commitDiffMapper.findDiffText(repoFullName, commitSha);
            }
        }
        if (diffText == null || diffText.isBlank()) {
            diffText = DIFF_UNAVAILABLE_PLACEHOLDER;
        }
        record.setDiffText(diffText);
        return Optional.of(record);
    }

    /**
     * 从 GitHub API 拉取该 commit 的 diff 并写入 aa_commit_diff（单次尝试，用于 webhook 等不阻塞场景）。
     */
    public void fetchAndSaveDiff(String repoFullName, String commitSha) {
        trySaveFetchedDiff(repoFullName, commitSha);
    }

    /**
     * 首次拉取失败后自动重试，共 6 次；间隔依次 0、1s、2s、4s、8s、16s 逐步延长，温和退避。
     * 用于查看 diff 时按需补全，提高成功率且不对 GitHub API 造成压力。
     */
    public void fetchAndSaveDiffWithRetry(String repoFullName, String commitSha) {
        for (int i = 0; i < DIFF_FETCH_RETRY_TIMES; i++) {
            if (i > 0 && i < DIFF_FETCH_BACKOFF_MS.length && DIFF_FETCH_BACKOFF_MS[i] > 0) {
                try {
                    Thread.sleep(DIFF_FETCH_BACKOFF_MS[i]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Diff fetch retry interrupted for {}@{}", repoFullName, commitSha);
                    break;
                }
            }
            if (trySaveFetchedDiff(repoFullName, commitSha)) {
                if (i > 0) log.info("Diff fetched on retry {} for {}@{}", i + 1, repoFullName, commitSha);
                return;
            }
        }
        log.debug("Diff still missing after {} retries for {}@{}", DIFF_FETCH_RETRY_TIMES, repoFullName, commitSha);
        log.warn("Diff unavailable after {} retries for {}@{} - check GITHUB_TOKEN, rate limit, or network", DIFF_FETCH_RETRY_TIMES, repoFullName, commitSha);
    }

    private boolean trySaveFetchedDiff(String repoFullName, String commitSha) {
        String diffText = githubDiffFetcher.fetchDiff(repoFullName, commitSha);
        if (diffText == null || diffText.isBlank()) return false;
        long size = diffText.getBytes().length;
        try {
            commitDiffMapper.insert(repoFullName, commitSha, diffText, size);
            return true;
        } catch (Exception ignoreDuplicate) {
            return true; // 已存在视为成功
        }
    }

    public List<CommitRecord> listPaged(int page, int pageSize) {
        int offset = Math.max((page - 1) * pageSize, 0);
        return commitMapper.listPaged(offset, pageSize);
    }

    public long countAll() {
        return commitMapper.countAll();
    }

    public List<CommitRecord> listPagedByRepo(String repoFullName, int page, int pageSize) {
        int offset = Math.max((page - 1) * pageSize, 0);
        return commitMapper.listPagedByRepo(repoFullName, offset, pageSize);
    }

    public long countByRepo(String repoFullName) {
        return commitMapper.countByRepo(repoFullName);
    }

    public List<String> listRepos() {
        return commitMapper.listDistinctRepos();
    }

    public List<CommitMapper.AuthorAggregate> aggregateByAuthor(String repoFullName) {
        return commitMapper.aggregateByAuthor(repoFullName);
    }

    /** 按日统计：最近 N 天，可选按仓库 */
    public List<CommitMapper.CommitByDay> listCommitsByDay(int days, String repoFullName) {
        OffsetDateTime since = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days);
        if (repoFullName != null && !repoFullName.isBlank()) {
            return commitMapper.listCommitsByDayByRepo(since, repoFullName.trim());
        }
        return commitMapper.listCommitsByDay(since);
    }

    /** 各仓库提交数 */
    public List<CommitMapper.RepoCount> listCommitsByRepo() {
        return commitMapper.listCommitsByRepo();
    }

    /** 去重作者数 */
    public long countDistinctAuthors(String repoFullName) {
        if (repoFullName != null && !repoFullName.isBlank()) {
            return commitMapper.countDistinctAuthorsByRepo(repoFullName.trim());
        }
        return commitMapper.countDistinctAuthors();
    }

    /** 全库作者提交排名（Top 50） */
    public List<CommitMapper.AuthorAggregate> aggregateByAuthorAll() {
        return commitMapper.aggregateByAuthorAll();
    }
}

