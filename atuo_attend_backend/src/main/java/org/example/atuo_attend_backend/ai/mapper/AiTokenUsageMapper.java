package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiTokenUsageMapper {

    @Insert("""
            INSERT INTO aa_ai_token_usage (tenant_id, used_at, provider, model, input_tokens, output_tokens, total_tokens, repo_full_name, commit_sha, usage_source, cost_yuan)
            VALUES (#{tenantId}, #{usedAt}, #{provider}, #{model}, #{inputTokens}, #{outputTokens}, #{totalTokens}, #{repoFullName}, #{commitSha}, #{usageSource}, #{costYuan})
            """)
    int insert(@Param("tenantId") long tenantId, @Param("usedAt") LocalDateTime usedAt, @Param("provider") String provider, @Param("model") String model,
               @Param("inputTokens") int inputTokens, @Param("outputTokens") int outputTokens, @Param("totalTokens") int totalTokens,
               @Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha,
               @Param("usageSource") String usageSource, @Param("costYuan") java.math.BigDecimal costYuan);

    /** 兼容旧调用：默认记为自备 Key、无官方成本 */
    default int insert(long tenantId, LocalDateTime usedAt, String provider, String model,
                       int inputTokens, int outputTokens, int totalTokens,
                       String repoFullName, String commitSha) {
        return insert(tenantId, usedAt, provider, model, inputTokens, outputTokens, totalTokens,
                repoFullName, commitSha, "personal", null);
    }

    @Select("""
            SELECT used_at AS usedAt, provider, model, input_tokens AS inputTokens, output_tokens AS outputTokens,
                   total_tokens AS totalTokens, repo_full_name AS repoFullName, commit_sha AS commitSha
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since}
            ORDER BY used_at DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listSince(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since, @Param("limit") int limit);

    @Select("""
            SELECT used_at AS usedAt, provider, model, input_tokens AS inputTokens, output_tokens AS outputTokens,
                   total_tokens AS totalTokens, repo_full_name AS repoFullName, commit_sha AS commitSha
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since} AND provider = #{provider}
            ORDER BY used_at DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listSinceByProvider(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since, @Param("provider") String provider, @Param("limit") int limit);

    @Select("""
            SELECT COALESCE(SUM(input_tokens), 0) AS inputTokens, COALESCE(SUM(output_tokens), 0) AS outputTokens,
                   COALESCE(SUM(total_tokens), 0) AS totalTokens, COUNT(*) AS callCount
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since}
            """)
    Map<String, Object> sumSince(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since);

    @Select("""
            SELECT COALESCE(SUM(input_tokens), 0) AS inputTokens, COALESCE(SUM(output_tokens), 0) AS outputTokens,
                   COALESCE(SUM(total_tokens), 0) AS totalTokens, COUNT(*) AS callCount
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since} AND provider = #{provider}
            """)
    Map<String, Object> sumSinceByProvider(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since, @Param("provider") String provider);

    @Select("""
            SELECT COUNT(*) AS total FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since} AND provider = #{provider}
            """)
    long countSinceByProvider(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since, @Param("provider") String provider);

    @Select("""
            SELECT used_at AS usedAt, provider, model, input_tokens AS inputTokens, output_tokens AS outputTokens,
                   total_tokens AS totalTokens, repo_full_name AS repoFullName, commit_sha AS commitSha
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since} AND provider = #{provider}
            ORDER BY used_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listSinceByProviderPaged(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since, @Param("provider") String provider,
                                                       @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            SELECT DATE(used_at) AS date, COUNT(*) AS callCount,
                   COALESCE(SUM(input_tokens), 0) AS inputTokens, COALESCE(SUM(output_tokens), 0) AS outputTokens,
                   COALESCE(SUM(total_tokens), 0) AS totalTokens
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since} AND provider = #{provider}
            GROUP BY DATE(used_at) ORDER BY date ASC
            """)
    List<Map<String, Object>> listDailyByProvider(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since, @Param("provider") String provider);

    @Select("""
            SELECT COALESCE(SUM(cost_yuan), 0) AS totalCostYuan
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since} AND usage_source = 'official'
            """)
    java.math.BigDecimal sumOfficialCostSince(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since);

    @Select("""
            SELECT model,
                   COALESCE(SUM(input_tokens), 0) AS inputTokens, COALESCE(SUM(output_tokens), 0) AS outputTokens,
                   COALESCE(SUM(total_tokens), 0) AS totalTokens, COUNT(*) AS callCount
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since} AND usage_source = 'personal'
            GROUP BY model
            ORDER BY totalTokens DESC
            """)
    List<Map<String, Object>> sumByModelPersonalSince(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since);

    /** 监测台：各租户官方池消耗（元），近 N 天 */
    @Select("""
            SELECT tenant_id AS tenantId,
                   COALESCE(SUM(CASE WHEN usage_source = 'official' THEN cost_yuan ELSE 0 END), 0) AS officialCostYuan
            FROM aa_ai_token_usage
            WHERE used_at >= #{since}
            GROUP BY tenant_id
            ORDER BY COALESCE(SUM(CASE WHEN usage_source = 'official' THEN cost_yuan ELSE 0 END), 0) DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> sumOfficialCostByTenantSince(@Param("since") LocalDateTime since, @Param("limit") int limit);

    @Select("""
            SELECT used_at AS usedAt, provider, model, usage_source AS usageSource, cost_yuan AS costYuan,
                   input_tokens AS inputTokens, output_tokens AS outputTokens,
                   total_tokens AS totalTokens, repo_full_name AS repoFullName, commit_sha AS commitSha
            FROM aa_ai_token_usage
            WHERE tenant_id = #{tenantId} AND used_at >= #{since}
            ORDER BY used_at DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listRecentWithBilling(@Param("tenantId") long tenantId, @Param("since") LocalDateTime since, @Param("limit") int limit);
}
