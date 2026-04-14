package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;

import java.util.List;

@Mapper
public interface TenantAdminUserMapper {

    @Select("SELECT id, tenant_id AS tenantId, phone, password_hash AS passwordHash, "
            + "sms_login_onboarded AS smsLoginOnboarded, created_at AS createdAt "
            + "FROM aa_tenant_admin_user WHERE id = #{id}")
    TenantAdminUser findById(@Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, phone, password_hash AS passwordHash, "
            + "sms_login_onboarded AS smsLoginOnboarded, created_at AS createdAt "
            + "FROM aa_tenant_admin_user WHERE phone = #{phone}")
    TenantAdminUser findByPhone(@Param("phone") String phone);

    @Insert("INSERT INTO aa_tenant_admin_user (tenant_id, phone, password_hash) VALUES (#{tenantId}, #{phone}, #{passwordHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TenantAdminUser user);

    @Select("SELECT COUNT(*) FROM aa_tenant_admin_user WHERE phone = #{phone}")
    int countByPhone(@Param("phone") String phone);

    @Select("SELECT COUNT(*) FROM aa_tenant_admin_user WHERE phone = #{phone} AND id <> #{excludeId}")
    int countByPhoneExcludingId(@Param("phone") String phone, @Param("excludeId") long excludeId);

    @Select("SELECT id, tenant_id AS tenantId, phone, password_hash AS passwordHash, "
            + "sms_login_onboarded AS smsLoginOnboarded, created_at AS createdAt "
            + "FROM aa_tenant_admin_user WHERE tenant_id = #{tenantId} ORDER BY id")
    List<TenantAdminUser> listByTenantId(@Param("tenantId") long tenantId);

    @Update("UPDATE aa_tenant_admin_user SET phone = #{phone} WHERE id = #{id} AND tenant_id = #{tenantId}")
    int updatePhone(@Param("id") long id, @Param("tenantId") long tenantId, @Param("phone") String phone);

    @Update("UPDATE aa_tenant_admin_user SET password_hash = #{passwordHash} WHERE id = #{id} AND tenant_id = #{tenantId}")
    int updatePasswordHash(@Param("id") long id, @Param("tenantId") long tenantId, @Param("passwordHash") String passwordHash);

    @Update("UPDATE aa_tenant_admin_user SET sms_login_onboarded = #{onboarded} WHERE id = #{id}")
    int updateSmsLoginOnboarded(@Param("id") long id, @Param("onboarded") boolean onboarded);
}
