package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.AdminSession;

@Mapper
public interface AdminSessionMapper {

    @Select("SELECT id, token, user_id AS userId, tenant_id AS tenantId, expires_at AS expiresAt, created_at AS createdAt "
            + "FROM aa_admin_session WHERE token = #{token} AND expires_at > NOW()")
    AdminSession findValidByToken(@Param("token") String token);

    @Insert("INSERT INTO aa_admin_session (token, user_id, tenant_id, expires_at) VALUES (#{token}, #{userId}, #{tenantId}, NOW() + INTERVAL 7200 SECOND)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminSession session);

    @Delete("DELETE FROM aa_admin_session WHERE expires_at < NOW()")
    int deleteExpired();

    @Delete("DELETE FROM aa_admin_session WHERE token = #{token}")
    int deleteByToken(@Param("token") String token);

    @Delete("DELETE FROM aa_admin_session WHERE tenant_id = #{tenantId}")
    int deleteByTenantId(@Param("tenantId") long tenantId);
}
