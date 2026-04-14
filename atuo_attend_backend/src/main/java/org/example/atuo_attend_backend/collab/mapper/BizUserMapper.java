package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizUser;

import java.util.List;

@Mapper
public interface BizUserMapper {

    @Select("SELECT id, tenant_id AS tenantId, email, name, password_hash AS passwordHash, role, avatar, remark_name AS remarkName, job_title AS jobTitle, "
            + "phone_e164 AS phoneE164, linked_tenant_admin_user_id AS linkedTenantAdminUserId, "
            + "created_at AS createdAt, updated_at AS updatedAt FROM biz_user WHERE id = #{id}")
    BizUser findById(@Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, email, name, password_hash AS passwordHash, role, avatar, remark_name AS remarkName, job_title AS jobTitle, "
            + "phone_e164 AS phoneE164, linked_tenant_admin_user_id AS linkedTenantAdminUserId, "
            + "created_at AS createdAt, updated_at AS updatedAt FROM biz_user WHERE tenant_id = #{tenantId} AND email = #{email}")
    BizUser findByTenantAndEmail(@Param("tenantId") long tenantId, @Param("email") String email);

    @Select("SELECT id, tenant_id AS tenantId, email, name, password_hash AS passwordHash, role, avatar, remark_name AS remarkName, job_title AS jobTitle, "
            + "phone_e164 AS phoneE164, linked_tenant_admin_user_id AS linkedTenantAdminUserId, "
            + "created_at AS createdAt, updated_at AS updatedAt FROM biz_user WHERE phone_e164 = #{phoneE164}")
    List<BizUser> listByPhoneE164(@Param("phoneE164") String phoneE164);

    @Select("SELECT id FROM biz_user WHERE LOWER(TRIM(email)) = LOWER(TRIM(#{email}))")
    List<Long> listIdsByEmailNormalized(@Param("email") String email);

    @Select("SELECT id, tenant_id AS tenantId, email, name, password_hash AS passwordHash, role, avatar, remark_name AS remarkName, job_title AS jobTitle, "
            + "phone_e164 AS phoneE164, linked_tenant_admin_user_id AS linkedTenantAdminUserId, "
            + "created_at AS createdAt, updated_at AS updatedAt FROM biz_user WHERE tenant_id = #{tenantId} ORDER BY id")
    List<BizUser> listByTenant(@Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) FROM biz_user WHERE tenant_id = #{tenantId}")
    long countByTenant(@Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) FROM biz_user WHERE phone_e164 = #{phoneE164}")
    long countByPhoneE164(@Param("phoneE164") String phoneE164);

    @Insert("INSERT INTO biz_user (tenant_id, email, name, password_hash, role, avatar, remark_name, job_title, phone_e164, linked_tenant_admin_user_id) "
            + "VALUES (#{tenantId}, #{email}, #{name}, #{passwordHash}, #{role}, #{avatar}, #{remarkName}, #{jobTitle}, #{phoneE164}, #{linkedTenantAdminUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizUser user);

    @Update("UPDATE biz_user SET name = #{name}, password_hash = #{passwordHash}, role = #{role}, avatar = #{avatar}, remark_name = #{remarkName}, job_title = #{jobTitle}, "
            + "phone_e164 = #{phoneE164}, linked_tenant_admin_user_id = #{linkedTenantAdminUserId} WHERE id = #{id} AND tenant_id = #{tenantId}")
    int update(BizUser user);

    /** 同一邮箱在各租户下的成员行同步绑定手机 */
    @Update("UPDATE biz_user SET phone_e164 = #{phoneE164}, linked_tenant_admin_user_id = #{linkedAdminId} "
            + "WHERE LOWER(TRIM(email)) = LOWER(TRIM(#{email}))")
    int updatePhoneLinkByEmailNormalized(@Param("email") String email,
                                         @Param("phoneE164") String phoneE164,
                                         @Param("linkedAdminId") Long linkedAdminId);

    @Update("UPDATE biz_user SET password_hash = #{passwordHash} WHERE LOWER(TRIM(email)) = LOWER(TRIM(#{email}))")
    int updatePasswordHashByEmailNormalized(@Param("email") String email, @Param("passwordHash") String passwordHash);
}
