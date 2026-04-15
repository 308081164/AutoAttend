package org.example.atuo_attend_backend.commit.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.commit.CommitRecord;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface CommitMapper {

    @Insert("""
            INSERT INTO aa_commit(
              tenant_id, repo_full_name, commit_sha, parent_sha, author_name, author_email,
              committed_at, message, files_changed, insertions, deletions, valid_commit, valid_reason
            ) VALUES (
              #{tenantId}, #{repoFullName}, #{commitSha}, #{parentSha}, #{authorName}, #{authorEmail},
              #{committedAt}, #{message}, #{filesChanged}, #{insertions}, #{deletions}, #{validCommit}, #{validReason}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommitRecord record);

    @Update("""
            UPDATE aa_commit
            SET files_changed = #{filesChanged}, insertions = #{insertions}, deletions = #{deletions}
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName} AND commit_sha = #{commitSha}
            """)
    int updateStats(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha,
                   @Param("filesChanged") int filesChanged, @Param("insertions") int insertions, @Param("deletions") int deletions);

    @Select("""
            SELECT
              repo_full_name AS repoFullName,
              commit_sha AS commitSha,
              parent_sha AS parentSha,
              author_name AS authorName,
              author_email AS authorEmail,
              committed_at AS committedAt,
              message AS message,
              files_changed AS filesChanged,
              insertions AS insertions,
              deletions AS deletions,
              valid_commit AS validCommit,
              valid_reason AS validReason
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName} AND commit_sha = #{commitSha}
            """)
    CommitRecord findOne(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha);

    /** 仅按 commit_sha 查一条（用于 getDiff 时前端未传 repoFullName 的兜底） */
    @Select("""
            SELECT
              repo_full_name AS repoFullName,
              commit_sha AS commitSha,
              parent_sha AS parentSha,
              author_name AS authorName,
              author_email AS authorEmail,
              committed_at AS committedAt,
              message AS message,
              files_changed AS filesChanged,
              insertions AS insertions,
              deletions AS deletions,
              valid_commit AS validCommit,
              valid_reason AS validReason
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND commit_sha = #{commitSha}
            ORDER BY committed_at DESC
            LIMIT 1
            """)
    CommitRecord findOneByCommitSha(@Param("tenantId") long tenantId, @Param("commitSha") String commitSha);

    @Select("""
            SELECT
              repo_full_name AS repoFullName,
              commit_sha AS commitSha,
              parent_sha AS parentSha,
              author_name AS authorName,
              author_email AS authorEmail,
              committed_at AS committedAt,
              message AS message,
              files_changed AS filesChanged,
              insertions AS insertions,
              deletions AS deletions,
              valid_commit AS validCommit,
              valid_reason AS validReason
            FROM aa_commit
            WHERE tenant_id = #{tenantId}
            ORDER BY committed_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CommitRecord> listPaged(@Param("tenantId") long tenantId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            SELECT
              repo_full_name AS repoFullName,
              commit_sha AS commitSha,
              parent_sha AS parentSha,
              author_name AS authorName,
              author_email AS authorEmail,
              committed_at AS committedAt,
              message AS message,
              files_changed AS filesChanged,
              insertions AS insertions,
              deletions AS deletions,
              valid_commit AS validCommit,
              valid_reason AS validReason
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName}
            ORDER BY committed_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CommitRecord> listPagedByRepo(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /** 查询尚未有 diff 的提交（用于定时任务补拉），按提交时间倒序，最多返回 limit 条 */
    @Select("""
            SELECT c.repo_full_name AS repoFullName, c.commit_sha AS commitSha
            FROM aa_commit c
            LEFT JOIN aa_commit_diff d ON c.tenant_id = d.tenant_id AND c.repo_full_name = d.repo_full_name AND c.commit_sha = d.commit_sha
            WHERE c.tenant_id = #{tenantId} AND d.repo_full_name IS NULL
            ORDER BY c.committed_at DESC
            LIMIT #{limit}
            """)
    List<CommitId> listCommitsWithoutDiff(@Param("tenantId") long tenantId, @Param("limit") int limit);

    class CommitId {
        private String repoFullName;
        private String commitSha;
        public String getRepoFullName() { return repoFullName; }
        public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
        public String getCommitSha() { return commitSha; }
        public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    }

    @Select("SELECT COUNT(1) FROM aa_commit WHERE tenant_id = #{tenantId}")
    long countAll(@Param("tenantId") long tenantId);

    @Select("SELECT COUNT(1) FROM aa_commit WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName}")
    long countByRepo(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName);

    /** 按作者邮箱统计提交数（用于员工工作台「我的统计」） */
    @Select("SELECT COUNT(1) FROM aa_commit WHERE tenant_id = #{tenantId} AND author_email = #{authorEmail}")
    long countByAuthorEmail(@Param("tenantId") long tenantId, @Param("authorEmail") String authorEmail);

    /** 按作者邮箱与时间范围统计提交数 */
    @Select("""
            SELECT COUNT(1) FROM aa_commit
            WHERE tenant_id = #{tenantId} AND author_email = #{authorEmail} AND committed_at >= #{since}
            """)
    long countByAuthorEmailSince(@Param("tenantId") long tenantId, @Param("authorEmail") String authorEmail,
                                @Param("since") java.time.OffsetDateTime since);

    /** 在 since 起至今（含当日）有提交行为的去重作者邮箱集合（author_email 去重） */
    @Select("""
            SELECT DISTINCT author_email
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND committed_at >= #{since}
              AND author_email IS NOT NULL AND author_email <> ''
            """)
    List<String> listDistinctAuthorEmailsSince(@Param("tenantId") long tenantId,
                                                 @Param("since") java.time.OffsetDateTime since);

    @Select("SELECT DISTINCT repo_full_name FROM aa_commit WHERE tenant_id = #{tenantId} ORDER BY repo_full_name")
    List<String> listDistinctRepos(@Param("tenantId") long tenantId);

    /** 在 [start, end) 时间范围内有提交的仓库列表 */
    @Select("""
            SELECT DISTINCT repo_full_name FROM aa_commit
            WHERE tenant_id = #{tenantId} AND committed_at >= #{start} AND committed_at < #{end}
            ORDER BY repo_full_name
            """)
    List<String> listReposWithCommitsBetween(@Param("tenantId") long tenantId, @Param("start") java.time.OffsetDateTime start,
                                            @Param("end") java.time.OffsetDateTime end);

    /** 某仓库在 [start, end) 内的提交，按时间正序 */
    @Select("""
            SELECT
              repo_full_name AS repoFullName,
              commit_sha AS commitSha,
              parent_sha AS parentSha,
              author_name AS authorName,
              author_email AS authorEmail,
              committed_at AS committedAt,
              message AS message,
              files_changed AS filesChanged,
              insertions AS insertions,
              deletions AS deletions,
              valid_commit AS validCommit,
              valid_reason AS validReason
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName} AND committed_at >= #{start} AND committed_at < #{end}
            ORDER BY committed_at ASC, commit_sha ASC
            """)
    List<CommitRecord> listCommitsByRepoBetween(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName,
                                                @Param("start") java.time.OffsetDateTime start,
                                                @Param("end") java.time.OffsetDateTime end);

    /** 某仓库在 [start, end) 内出现过的去重作者邮箱列表（仅非空） */
    @Select("""
            SELECT DISTINCT author_email
            FROM aa_commit
            WHERE tenant_id = #{tenantId}
              AND repo_full_name = #{repoFullName}
              AND committed_at >= #{start} AND committed_at < #{end}
              AND author_email IS NOT NULL AND author_email <> ''
            ORDER BY author_email
            """)
    List<String> listDistinctAuthorEmailsByRepoBetween(@Param("tenantId") long tenantId,
                                                       @Param("repoFullName") String repoFullName,
                                                       @Param("start") java.time.OffsetDateTime start,
                                                       @Param("end") java.time.OffsetDateTime end);

    /** 按日统计提交数与代码量，since 起至今（含当日） */
    @Select("""
            SELECT DATE(committed_at) AS day, COUNT(*) AS count,
                   COALESCE(SUM(insertions), 0) AS insertions, COALESCE(SUM(deletions), 0) AS deletions
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND committed_at >= #{since}
            GROUP BY DATE(committed_at)
            ORDER BY day
            """)
    List<CommitByDay> listCommitsByDay(@Param("tenantId") long tenantId, @Param("since") java.time.OffsetDateTime since);

    @Select("""
            SELECT DATE(committed_at) AS day, COUNT(*) AS count,
                   COALESCE(SUM(insertions), 0) AS insertions, COALESCE(SUM(deletions), 0) AS deletions
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND committed_at >= #{since} AND repo_full_name = #{repoFullName}
            GROUP BY DATE(committed_at)
            ORDER BY day
            """)
    List<CommitByDay> listCommitsByDayByRepo(@Param("tenantId") long tenantId, @Param("since") java.time.OffsetDateTime since, @Param("repoFullName") String repoFullName);

    /** 各仓库提交数，用于饼图 */
    @Select("""
            SELECT repo_full_name AS repoFullName, COUNT(*) AS count
            FROM aa_commit
            WHERE tenant_id = #{tenantId}
            GROUP BY repo_full_name
            ORDER BY count DESC
            """)
    List<RepoCount> listCommitsByRepo(@Param("tenantId") long tenantId);

    /** 去重作者数（按 author_name + author_email） */
    @Select("SELECT COUNT(*) FROM (SELECT 1 FROM aa_commit WHERE tenant_id = #{tenantId} GROUP BY author_name, author_email) t")
    long countDistinctAuthors(@Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) FROM (SELECT 1 FROM aa_commit WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName} GROUP BY author_name, author_email) t")
    long countDistinctAuthorsByRepo(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName);

    /** 全库作者聚合（用于「全部」时的开发者排名） */
    @Select("""
            SELECT author_name AS authorName, author_email AS authorEmail,
                   COUNT(*) AS commitCount, MAX(committed_at) AS lastCommittedAt
            FROM aa_commit
            WHERE tenant_id = #{tenantId}
            GROUP BY author_name, author_email
            ORDER BY commitCount DESC
            LIMIT 50
            """)
    List<AuthorAggregate> aggregateByAuthorAll(@Param("tenantId") long tenantId);

    class CommitByDay {
        private java.util.Date day;
        private long count;
        private long insertions;
        private long deletions;
        public java.util.Date getDay() { return day; }
        public void setDay(java.util.Date day) { this.day = day; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public long getInsertions() { return insertions; }
        public void setInsertions(long insertions) { this.insertions = insertions; }
        public long getDeletions() { return deletions; }
        public void setDeletions(long deletions) { this.deletions = deletions; }
    }

    class RepoCount {
        private String repoFullName;
        private long count;
        public String getRepoFullName() { return repoFullName; }
        public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    @Select("""
            SELECT
              author_name AS authorName,
              author_email AS authorEmail,
              COUNT(*) AS commitCount,
              MAX(committed_at) AS lastCommittedAt
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName}
            GROUP BY author_name, author_email
            ORDER BY commitCount DESC
            """)
    List<AuthorAggregate> aggregateByAuthor(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName);

    /** 全库作者在 [start, end) 内的提交数排名 */
    @Select("""
            SELECT author_name AS authorName, author_email AS authorEmail,
                   COUNT(*) AS commitCount, MAX(committed_at) AS lastCommittedAt
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND committed_at >= #{start} AND committed_at < #{end}
            GROUP BY author_name, author_email
            ORDER BY commitCount DESC
            LIMIT 50
            """)
    List<AuthorAggregate> aggregateByAuthorAllBetween(@Param("tenantId") long tenantId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    /** 单仓库作者在 [start, end) 内的提交数排名 */
    @Select("""
            SELECT author_name AS authorName, author_email AS authorEmail,
                   COUNT(*) AS commitCount, MAX(committed_at) AS lastCommittedAt
            FROM aa_commit
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName}
              AND committed_at >= #{start} AND committed_at < #{end}
            GROUP BY author_name, author_email
            ORDER BY commitCount DESC
            LIMIT 50
            """)
    List<AuthorAggregate> aggregateByAuthorBetween(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName,
                                                   @Param("start") OffsetDateTime start,
                                                   @Param("end") OffsetDateTime end);

    class AuthorAggregate {
        private String authorName;
        private String authorEmail;
        private long commitCount;
        private OffsetDateTime lastCommittedAt;

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public String getAuthorEmail() {
            return authorEmail;
        }

        public void setAuthorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
        }

        public long getCommitCount() {
            return commitCount;
        }

        public void setCommitCount(long commitCount) {
            this.commitCount = commitCount;
        }

        public OffsetDateTime getLastCommittedAt() {
            return lastCommittedAt;
        }

        public void setLastCommittedAt(OffsetDateTime lastCommittedAt) {
            this.lastCommittedAt = lastCommittedAt;
        }
    }

    /** 作者提交数，限定在指定 biz_project.id 集合（按仓库 JOIN） */
    @Select("""
            <script>
            SELECT COUNT(1) FROM aa_commit c
            INNER JOIN biz_project p ON p.repo_id = c.repo_full_name AND p.tenant_id = c.tenant_id
            WHERE c.tenant_id = #{tenantId} AND c.author_email = #{authorEmail}
            AND p.id IN
            <foreach collection="projectIds" item="pid" open="(" separator="," close=")">
            #{pid}
            </foreach>
            </script>
            """)
    long countByAuthorEmailInProjectIds(@Param("tenantId") long tenantId,
                                        @Param("authorEmail") String authorEmail,
                                        @Param("projectIds") List<Long> projectIds);

    @Select("""
            <script>
            SELECT COUNT(1) FROM aa_commit c
            INNER JOIN biz_project p ON p.repo_id = c.repo_full_name AND p.tenant_id = c.tenant_id
            WHERE c.tenant_id = #{tenantId} AND c.author_email = #{authorEmail}
            AND c.committed_at >= #{since}
            AND p.id IN
            <foreach collection="projectIds" item="pid" open="(" separator="," close=")">
            #{pid}
            </foreach>
            </script>
            """)
    long countByAuthorEmailSinceInProjectIds(@Param("tenantId") long tenantId,
                                            @Param("authorEmail") String authorEmail,
                                            @Param("since") OffsetDateTime since,
                                            @Param("projectIds") List<Long> projectIds);
}

