package org.example.atuo_attend_backend.platform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Mapper
public interface PlatformOpsMetricsMapper {

    @Select("""
            SELECT COUNT(*) FROM biz_user
            """)
    long countTotalUsers();

    @Select("""
            SELECT COUNT(DISTINCT author_email)
            FROM aa_commit
            WHERE committed_at >= #{start} AND committed_at < #{end}
              AND author_email IS NOT NULL AND TRIM(author_email) <> ''
            """)
    long countDistinctAuthorsBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Select("""
            SELECT
              DATE(committed_at) AS day,
              COUNT(DISTINCT author_email) AS count
            FROM aa_commit
            WHERE committed_at >= #{start} AND committed_at < #{end}
              AND author_email IS NOT NULL AND TRIM(author_email) <> ''
            GROUP BY DATE(committed_at)
            ORDER BY day
            """)
    List<DauTrendRow> listDauTrend(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Select("""
            SELECT
              author_name AS authorName,
              author_email AS authorEmail,
              COUNT(*) AS commitCount,
              MAX(committed_at) AS lastCommittedAt
            FROM aa_commit
            WHERE committed_at >= #{start} AND committed_at < #{end}
              AND author_email IS NOT NULL AND TRIM(author_email) <> ''
            GROUP BY author_name, author_email
            ORDER BY commitCount DESC
            LIMIT #{limit}
            """)
    List<ActiveAuthorRow> listActiveAuthors(@Param("start") OffsetDateTime start,
                                              @Param("end") OffsetDateTime end,
                                              @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM aa_tenant")
    long countTenants();

    @Select("""
            SELECT COUNT(DISTINCT tenant_id)
            FROM aa_ai_analysis_config
            WHERE enabled = 1
              AND api_key IS NOT NULL
              AND TRIM(api_key) <> ''
            """)
    long countAiApiKeyConfiguredTenants();

    @Select("""
            SELECT COUNT(DISTINCT tenant_id)
            FROM aa_system_config
            WHERE config_key = 'github.token'
              AND config_value IS NOT NULL
              AND TRIM(config_value) <> ''
            """)
    long countGithubTokenConfiguredTenants();

    class DauTrendRow {
        private Date day;
        private long count;

        public Date getDay() {
            return day;
        }

        public void setDay(Date day) {
            this.day = day;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    class ActiveAuthorRow {
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

