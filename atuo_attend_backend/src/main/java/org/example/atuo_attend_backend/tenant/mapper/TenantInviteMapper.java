package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.TenantInvite;

import java.util.List;

@Mapper
public interface TenantInviteMapper {

    @Insert("INSERT INTO aa_tenant_invite (tenant_id, token, expires_at, max_uses, used_count, note) VALUES (#{tenantId}, #{token}, #{expiresAt}, #{maxUses}, #{usedCount}, #{note})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TenantInvite row);

    @Select("SELECT id, tenant_id AS tenantId, token, expires_at AS expiresAt, max_uses AS maxUses, used_count AS usedCount, note, created_at AS createdAt FROM aa_tenant_invite WHERE token = #{token} LIMIT 1")
    TenantInvite findByToken(@Param("token") String token);

    @Select("SELECT id, tenant_id AS tenantId, token, expires_at AS expiresAt, max_uses AS maxUses, used_count AS usedCount, note, created_at AS createdAt FROM aa_tenant_invite WHERE tenant_id = #{tenantId} ORDER BY id DESC")
    List<TenantInvite> listByTenant(@Param("tenantId") long tenantId);

    @Update("UPDATE aa_tenant_invite SET used_count = used_count + 1 WHERE id = #{id}")
    int incrementUsed(@Param("id") long id);
}
