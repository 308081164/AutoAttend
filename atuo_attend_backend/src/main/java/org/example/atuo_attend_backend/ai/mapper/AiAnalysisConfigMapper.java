package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;

@Mapper
public interface AiAnalysisConfigMapper {

    @Select("SELECT id, tenant_id AS tenantId, provider, api_key AS apiKey, enabled, daily_summary_enabled AS dailySummaryEnabled, model, prompt_version AS promptVersion, max_diff_chars AS maxDiffChars, created_at AS createdAt, updated_at AS updatedAt FROM aa_ai_analysis_config WHERE tenant_id = #{tenantId} AND provider = #{provider} LIMIT 1")
    AiAnalysisConfig findByProvider(@Param("tenantId") long tenantId, @Param("provider") String provider);

    @Update("""
            UPDATE aa_ai_analysis_config
            SET api_key = #{apiKey}, enabled = #{enabled}, daily_summary_enabled = #{dailySummaryEnabled}, model = #{model}, prompt_version = #{promptVersion}, max_diff_chars = #{maxDiffChars}, updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND provider = #{provider}
            """)
    int update(@Param("tenantId") long tenantId, @Param("provider") String provider, @Param("apiKey") String apiKey, @Param("enabled") Boolean enabled,
               @Param("dailySummaryEnabled") Boolean dailySummaryEnabled,
               @Param("model") String model, @Param("promptVersion") String promptVersion, @Param("maxDiffChars") Integer maxDiffChars);

    @Insert("INSERT INTO aa_ai_analysis_config (tenant_id, provider, api_key, enabled, daily_summary_enabled, model, prompt_version, max_diff_chars) VALUES (#{tenantId}, #{provider}, #{apiKey}, #{enabled}, #{dailySummaryEnabled}, #{model}, #{promptVersion}, #{maxDiffChars})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiAnalysisConfig config);
}
