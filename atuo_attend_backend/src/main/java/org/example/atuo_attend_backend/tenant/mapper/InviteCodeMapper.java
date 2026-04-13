package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.InviteCode;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InviteCodeMapper {

    @Insert("""
            INSERT INTO aa_invite_code (code, created_by, referrer_tenant_id, creator_user_id, expires_at, max_uses, disabled)
            VALUES (#{code}, #{createdBy}, #{referrerTenantId}, #{creatorUserId}, #{expiresAt}, #{maxUses}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InviteCode row);

    /**
     * 有效邀请码：未禁用、未过期、仍有可用次数。
     */
    @Select("""
            SELECT id, code, created_by AS createdBy, referrer_tenant_id AS referrerTenantId,
                   creator_user_id AS creatorUserId, expires_at AS expiresAt,
                   max_uses AS maxUses, used_count AS usedCount, disabled, created_at AS createdAt
            FROM aa_invite_code
            WHERE UPPER(TRIM(#{code})) = code AND disabled = 0
              AND (expires_at IS NULL OR expires_at > NOW())
              AND (max_uses IS NULL OR used_count < max_uses)
            """)
    InviteCode findActiveByCode(@Param("code") String code);

    @Select("""
            SELECT id, code, created_by AS createdBy, referrer_tenant_id AS referrerTenantId,
                   creator_user_id AS creatorUserId, expires_at AS expiresAt,
                   max_uses AS maxUses, used_count AS usedCount, disabled, created_at AS createdAt
            FROM aa_invite_code WHERE referrer_tenant_id = #{tenantId} AND created_by = 'user' AND disabled = 0
            ORDER BY id DESC LIMIT 1
            """)
    InviteCode findUserCodeForTenant(@Param("tenantId") long tenantId);

    @Select("""
            SELECT id, code, created_by AS createdBy, referrer_tenant_id AS referrerTenantId,
                   creator_user_id AS creatorUserId, expires_at AS expiresAt,
                   max_uses AS maxUses, used_count AS usedCount, disabled, created_at AS createdAt
            FROM aa_invite_code WHERE referrer_tenant_id = #{tenantId} AND created_by = 'platform'
            ORDER BY id DESC LIMIT 50
            """)
    List<InviteCode> listPlatformCodesForTenant(@Param("tenantId") long tenantId);

    /**
     * 成功使用一次（注册绑定或兑换）：原子递增，防止超额。
     */
    @Update("""
            UPDATE aa_invite_code SET used_count = used_count + 1
            WHERE id = #{id} AND disabled = 0
              AND (expires_at IS NULL OR expires_at > NOW())
              AND (max_uses IS NULL OR used_count < max_uses)
            """)
    int tryIncrementUse(@Param("id") long id);
}
