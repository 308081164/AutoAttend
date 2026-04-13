package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.InviteCode;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InviteCodeMapper {

    @Insert("""
            INSERT INTO aa_invite_code (code, created_by, referrer_tenant_id, creator_user_id, expires_at, disabled)
            VALUES (#{code}, #{createdBy}, #{referrerTenantId}, #{creatorUserId}, #{expiresAt}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InviteCode row);

    @Select("""
            SELECT id, code, created_by AS createdBy, referrer_tenant_id AS referrerTenantId,
                   creator_user_id AS creatorUserId, expires_at AS expiresAt, disabled, created_at AS createdAt
            FROM aa_invite_code WHERE UPPER(TRIM(#{code})) = code AND disabled = 0
            """)
    InviteCode findActiveByCode(@Param("code") String code);

    @Select("""
            SELECT id, code, created_by AS createdBy, referrer_tenant_id AS referrerTenantId,
                   creator_user_id AS creatorUserId, expires_at AS expiresAt, disabled, created_at AS createdAt
            FROM aa_invite_code WHERE referrer_tenant_id = #{tenantId} AND created_by = 'user' AND disabled = 0
            ORDER BY id DESC LIMIT 1
            """)
    InviteCode findUserCodeForTenant(@Param("tenantId") long tenantId);

    @Select("""
            SELECT id, code, created_by AS createdBy, referrer_tenant_id AS referrerTenantId,
                   creator_user_id AS creatorUserId, expires_at AS expiresAt, disabled, created_at AS createdAt
            FROM aa_invite_code WHERE referrer_tenant_id = #{tenantId} AND created_by = 'platform'
            ORDER BY id DESC LIMIT 20
            """)
    List<InviteCode> listPlatformCodesForTenant(@Param("tenantId") long tenantId);
}
