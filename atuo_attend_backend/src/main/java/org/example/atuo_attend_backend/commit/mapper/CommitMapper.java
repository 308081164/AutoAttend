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

    @Select("SELECT DISTINCT repo_full_name FROM aa_commit ORDER BY repo_full_name")
    List<String> listDistinctRepos();

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

