package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.ai.domain.TokenRedeemCode;

import java.util.List;
import java.util.Map;

@Mapper
public interface TokenRedeemCodeMapper {

    @Insert("""
            INSERT INTO aa_token_redeem_code (code_hash, grant_cny, max_uses, used_count, expires_at, note)
            VALUES (#{codeHash}, #{grantCny}, #{maxUses}, 0, #{expiresAt}, #{note})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TokenRedeemCode row);

    @Select("""
            SELECT id, code_hash AS codeHash, grant_cny AS grantCny, max_uses AS maxUses, used_count AS usedCount,
                   expires_at AS expiresAt, note, created_at AS createdAt
            FROM aa_token_redeem_code WHERE id = #{id}
            """)
    Map<String, Object> findById(@Param("id") long id);

    @Select("""
            SELECT id, code_hash AS codeHash, grant_cny AS grantCny, max_uses AS maxUses, used_count AS usedCount,
                   expires_at AS expiresAt, note, created_at AS createdAt
            FROM aa_token_redeem_code
            WHERE code_hash = #{codeHash}
            """)
    Map<String, Object> findByCodeHash(@Param("codeHash") String codeHash);

    @Select("""
            SELECT id, code_hash AS codeHash, grant_cny AS grantCny, max_uses AS maxUses, used_count AS usedCount,
                   expires_at AS expiresAt, note, created_at AS createdAt
            FROM aa_token_redeem_code ORDER BY id DESC LIMIT #{limit}
            """)
    List<Map<String, Object>> listRecent(@Param("limit") int limit);

    @Update("""
            UPDATE aa_token_redeem_code SET used_count = used_count + 1
            WHERE id = #{id}
              AND (expires_at IS NULL OR expires_at > NOW())
              AND used_count < max_uses
            """)
    int tryIncrementUse(@Param("id") long id);

    @Insert("""
            INSERT INTO aa_token_redeem_log (code_id, tenant_id, grant_cny)
            VALUES (#{codeId}, #{tenantId}, #{grantCny})
            """)
    int insertLog(@Param("codeId") long codeId, @Param("tenantId") long tenantId,
                  @Param("grantCny") java.math.BigDecimal grantCny);
}
