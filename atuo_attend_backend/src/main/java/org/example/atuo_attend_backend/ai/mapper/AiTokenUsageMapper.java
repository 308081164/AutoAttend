package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiTokenUsageMapper {

    @Insert("""
            INSERT INTO aa_ai_token_usage (used_at, provider, model, input_tokens, output_tokens, total_tokens, repo_full_name, commit_sha)
            VALUES (#{usedAt}, #{provider}, #{model}, #{inputTokens}, #{outputTokens}, #{totalTokens}, #{repoFullName}, #{commitSha})
            """)
    int insert(@Param("usedAt") LocalDateTime usedAt, @Param("provider") String provider, @Param("model") String model,
               @Param("inputTokens") int inputTokens, @Param("outputTokens") int outputTokens, @Param("totalTokens") int totalTokens,
               @Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha);

    @Select("""
            SELECT used_at AS usedAt, provider, model, input_tokens AS inputTokens, output_tokens AS outputTokens,
                   total_tokens AS totalTokens, repo_full_name AS repoFullName, commit_sha AS commitSha
            FROM aa_ai_token_usage
            WHERE used_at >= #{since}
            ORDER BY used_at DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listSince(@Param("since") LocalDateTime since, @Param("limit") int limit);

    @Select("""
            SELECT COALESCE(SUM(input_tokens), 0) AS inputTokens, COALESCE(SUM(output_tokens), 0) AS outputTokens,
                   COALESCE(SUM(total_tokens), 0) AS totalTokens, COUNT(*) AS callCount
            FROM aa_ai_token_usage
            WHERE used_at >= #{since}
            """)
    Map<String, Object> sumSince(@Param("since") LocalDateTime since);
}
