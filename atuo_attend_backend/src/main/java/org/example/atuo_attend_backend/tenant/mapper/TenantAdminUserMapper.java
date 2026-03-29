package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;

@Mapper
public interface TenantAdminUserMapper {

    @Select("SELECT id, tenant_id AS tenantId, phone, password_hash AS passwordHash, created_at AS createdAt "
            + "FROM aa_tenant_admin_user WHERE id = #{id}")
    TenantAdminUser findById(@Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, phone, password_hash AS passwordHash, created_at AS createdAt "
            + "FROM aa_tenant_admin_user WHERE phone = #{phone}")
    TenantAdminUser findByPhone(@Param("phone") String phone);

    @Insert("INSERT INTO aa_tenant_admin_user (tenant_id, phone, password_hash) VALUES (#{tenantId}, #{phone}, #{passwordHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TenantAdminUser user);

    @Select("SELECT COUNT(*) FROM aa_tenant_admin_user WHERE phone = #{phone}")
    int countByPhone(@Param("phone") String phone);
}
