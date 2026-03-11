package org.example.atuo_attend_backend.commit;

import org.example.atuo_attend_backend.commit.mapper.CommitDiffMapper;
import org.example.atuo_attend_backend.commit.mapper.CommitMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class CommitService {

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

    public Optional<CommitRecord> findCommit(String repoFullName, String commitSha) {
        CommitRecord record = commitMapper.findOne(repoFullName, commitSha);
        if (record == null) {
            return Optional.empty();
        }
        String diffText = commitDiffMapper.findDiffText(repoFullName, commitSha);
        if (diffText == null || diffText.isBlank()) {
            fetchAndSaveDiff(repoFullName, commitSha);
            diffText = commitDiffMapper.findDiffText(repoFullName, commitSha);
        }
        record.setDiffText(diffText);
        return Optional.of(record);
    }

    /**
     * 从 GitHub API 拉取该 commit 的 diff 并写入 aa_commit_diff，供查看与后续 AI 分析使用。
     */
    public void fetchAndSaveDiff(String repoFullName, String commitSha) {
        String diffText = githubDiffFetcher.fetchDiff(repoFullName, commitSha);
        if (diffText == null || diffText.isBlank()) return;
        long size = diffText.getBytes().length;
        try {
            commitDiffMapper.insert(repoFullName, commitSha, diffText, size);
        } catch (Exception ignoreDuplicate) {
            // 已存在则忽略
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
}

