package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.ai.domain.ProjectDailySummary;
import org.example.atuo_attend_backend.ai.dto.ProjectDailySummaryListItem;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProjectDailySummaryMapper {

    @Select("""
            SELECT id, tenant_id AS tenantId, repo_full_name AS repoFullName, summary_date AS summaryDate, title, content,
                   commit_count AS commitCount, model, status, error_message AS errorMessage,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_project_daily_summary WHERE tenant_id = #{tenantId} AND id = #{id} LIMIT 1
            """)
    ProjectDailySummary findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("""
            SELECT id, tenant_id AS tenantId, repo_full_name AS repoFullName, summary_date AS summaryDate, title, content,
                   commit_count AS commitCount, model, status, error_message AS errorMessage,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_project_daily_summary
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName} AND summary_date = #{summaryDate} LIMIT 1
            """)
    ProjectDailySummary findByRepoAndDate(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName,
                                         @Param("summaryDate") LocalDate summaryDate);

    @Select("""
            SELECT id, repo_full_name AS repoFullName, summary_date AS summaryDate, title,
                   commit_count AS commitCount, model, status, updated_at AS updatedAt
            FROM aa_project_daily_summary
            WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName}
            ORDER BY summary_date DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ProjectDailySummaryListItem> listByRepoPaged(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM aa_project_daily_summary WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName}")
    long countByRepo(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName);

    @Insert("""
            INSERT INTO aa_project_daily_summary (tenant_id, repo_full_name, summary_date, title, content, commit_count, model, status, error_message)
            VALUES (#{tenantId}, #{repoFullName}, #{summaryDate}, #{title}, #{content}, #{commitCount}, #{model}, #{status}, #{errorMessage})
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                content = VALUES(content),
                commit_count = VALUES(commit_count),
                model = VALUES(model),
                status = VALUES(status),
                error_message = VALUES(error_message),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("tenantId") long tenantId,
               @Param("repoFullName") String repoFullName,
               @Param("summaryDate") LocalDate summaryDate,
               @Param("title") String title,
               @Param("content") String content,
               @Param("commitCount") int commitCount,
               @Param("model") String model,
               @Param("status") String status,
               @Param("errorMessage") String errorMessage);
}
