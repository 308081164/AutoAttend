package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisJob;

@Mapper
public interface AiAnalysisJobMapper {

    @Select("""
            SELECT id, tenant_id AS tenantId, repo_full_name AS repoFullName, commit_sha AS commitSha, status, model, prompt_version AS promptVersion,
            retry_count AS retryCount, last_error AS lastError, created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ai_analysis_job WHERE tenant_id = #{tenantId} AND repo_full_name = #{repoFullName} AND commit_sha = #{commitSha} LIMIT 1
            """)
    AiAnalysisJob findByRepoAndSha(@Param("tenantId") long tenantId, @Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha);

    @Insert("""
            INSERT INTO aa_ai_analysis_job (tenant_id, repo_full_name, commit_sha, status, model, prompt_version, retry_count, last_error)
            VALUES (#{tenantId}, #{repoFullName}, #{commitSha}, #{status}, #{model}, #{promptVersion}, #{retryCount}, #{lastError})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiAnalysisJob job);

    @Update("UPDATE aa_ai_analysis_job SET status = #{status}, model = #{model}, prompt_version = #{promptVersion}, retry_count = #{retryCount}, last_error = #{lastError}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND tenant_id = #{tenantId}")
    int update(AiAnalysisJob job);
}
