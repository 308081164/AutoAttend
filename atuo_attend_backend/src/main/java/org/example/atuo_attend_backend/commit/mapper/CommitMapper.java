package org.example.atuo_attend_backend.commit.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.commit.CommitRecord;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface CommitMapper {

    @Insert("""
            INSERT INTO aa_commit(
              repo_full_name, commit_sha, parent_sha, author_name, author_email,
              committed_at, message, files_changed, insertions, deletions, valid_commit, valid_reason
            ) VALUES (
              #{repoFullName}, #{commitSha}, #{parentSha}, #{authorName}, #{authorEmail},
              #{committedAt}, #{message}, #{filesChanged}, #{insertions}, #{deletions}, #{validCommit}, #{validReason}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommitRecord record);

    @Update("""
            UPDATE aa_commit
            SET files_changed = #{filesChanged}, insertions = #{insertions}, deletions = #{deletions}
            WHERE repo_full_name = #{repoFullName} AND commit_sha = #{commitSha}
            """)
    int updateStats(@Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha,
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
            WHERE repo_full_name = #{repoFullName} AND commit_sha = #{commitSha}
            """)
    CommitRecord findOne(@Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha);

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
            WHERE commit_sha = #{commitSha}
            ORDER BY committed_at DESC
            LIMIT 1
            """)
    CommitRecord findOneByCommitSha(@Param("commitSha") String commitSha);

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
            ORDER BY committed_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CommitRecord> listPaged(@Param("offset") int offset, @Param("limit") int limit);

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
            WHERE repo_full_name = #{repoFullName}
            ORDER BY committed_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CommitRecord> listPagedByRepo(@Param("repoFullName") String repoFullName,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM aa_commit")
    long countAll();

    @Select("SELECT COUNT(1) FROM aa_commit WHERE repo_full_name = #{repoFullName}")
    long countByRepo(@Param("repoFullName") String repoFullName);

    /** 按作者邮箱统计提交数（用于员工工作台「我的统计」） */
    @Select("SELECT COUNT(1) FROM aa_commit WHERE author_email = #{authorEmail}")
    long countByAuthorEmail(@Param("authorEmail") String authorEmail);

    /** 按作者邮箱与时间范围统计提交数 */
    @Select("""
            SELECT COUNT(1) FROM aa_commit
            WHERE author_email = #{authorEmail} AND committed_at >= #{since}
            """)
    long countByAuthorEmailSince(@Param("authorEmail") String authorEmail,
                                @Param("since") java.time.OffsetDateTime since);

    @Select("SELECT DISTINCT repo_full_name FROM aa_commit ORDER BY repo_full_name")
    List<String> listDistinctRepos();

    /** 按日统计提交数与代码量，since 起至今（含当日） */
    @Select("""
            SELECT DATE(committed_at) AS day, COUNT(*) AS count,
                   COALESCE(SUM(insertions), 0) AS insertions, COALESCE(SUM(deletions), 0) AS deletions
            FROM aa_commit
            WHERE committed_at >= #{since}
            GROUP BY DATE(committed_at)
            ORDER BY day
            """)
    List<CommitByDay> listCommitsByDay(@Param("since") java.time.OffsetDateTime since);

    @Select("""
            SELECT DATE(committed_at) AS day, COUNT(*) AS count,
                   COALESCE(SUM(insertions), 0) AS insertions, COALESCE(SUM(deletions), 0) AS deletions
            FROM aa_commit
            WHERE committed_at >= #{since} AND repo_full_name = #{repoFullName}
            GROUP BY DATE(committed_at)
            ORDER BY day
            """)
    List<CommitByDay> listCommitsByDayByRepo(@Param("since") java.time.OffsetDateTime since, @Param("repoFullName") String repoFullName);

    /** 各仓库提交数，用于饼图 */
    @Select("""
            SELECT repo_full_name AS repoFullName, COUNT(*) AS count
            FROM aa_commit
            GROUP BY repo_full_name
            ORDER BY count DESC
            """)
    List<RepoCount> listCommitsByRepo();

    /** 去重作者数（按 author_name + author_email） */
    @Select("SELECT COUNT(*) FROM (SELECT 1 FROM aa_commit GROUP BY author_name, author_email) t")
    long countDistinctAuthors();

    @Select("SELECT COUNT(*) FROM (SELECT 1 FROM aa_commit WHERE repo_full_name = #{repoFullName} GROUP BY author_name, author_email) t")
    long countDistinctAuthorsByRepo(@Param("repoFullName") String repoFullName);

    /** 全库作者聚合（用于「全部」时的开发者排名） */
    @Select("""
            SELECT author_name AS authorName, author_email AS authorEmail,
                   COUNT(*) AS commitCount, MAX(committed_at) AS lastCommittedAt
            FROM aa_commit
            GROUP BY author_name, author_email
            ORDER BY commitCount DESC
            LIMIT 50
            """)
    List<AuthorAggregate> aggregateByAuthorAll();

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
            WHERE repo_full_name = #{repoFullName}
            GROUP BY author_name, author_email
            ORDER BY commitCount DESC
            """)
    List<AuthorAggregate> aggregateByAuthor(@Param("repoFullName") String repoFullName);

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
}

