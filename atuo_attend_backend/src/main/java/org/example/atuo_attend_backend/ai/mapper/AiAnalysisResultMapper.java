package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;

@Mapper
public interface AiAnalysisResultMapper {

    @Select("""
            SELECT id, repo_full_name AS repoFullName, commit_sha AS commitSha, work_summary AS workSummary, work_type AS workType, main_area AS mainArea,
            is_effective AS isEffective, effective_reason AS effectiveReason, invalid_reason_tag AS invalidReasonTag,
            quality_level AS qualityLevel, quality_comment AS qualityComment, risk_flags AS riskFlags, suggestions,
            raw_response AS rawResponse, prompt_version AS promptVersion, created_at AS createdAt
            FROM aa_ai_analysis_result WHERE repo_full_name = #{repoFullName} AND commit_sha = #{commitSha} LIMIT 1
            """)
    AiAnalysisResult findByRepoAndSha(@Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha);

    @Insert("""
            INSERT INTO aa_ai_analysis_result (repo_full_name, commit_sha, work_summary, work_type, main_area, is_effective, effective_reason, invalid_reason_tag,
            quality_level, quality_comment, risk_flags, suggestions, raw_response, prompt_version)
            VALUES (#{repoFullName}, #{commitSha}, #{workSummary}, #{workType}, #{mainArea}, #{isEffective}, #{effectiveReason}, #{invalidReasonTag},
            #{qualityLevel}, #{qualityComment}, #{riskFlags}, #{suggestions}, #{rawResponse}, #{promptVersion})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiAnalysisResult result);
}
