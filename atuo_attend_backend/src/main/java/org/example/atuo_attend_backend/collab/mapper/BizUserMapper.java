package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizUser;

import java.util.List;

@Mapper
public interface BizUserMapper {

    @Select("SELECT id, tenant_id AS tenantId, email, name, password_hash AS passwordHash, role, avatar, remark_name AS remarkName, job_title AS jobTitle, created_at AS createdAt, updated_at AS updatedAt FROM biz_user WHERE id = #{id}")
    BizUser findById(@Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, email, name, password_hash AS passwordHash, role, avatar, remark_name AS remarkName, job_title AS jobTitle, created_at AS createdAt, updated_at AS updatedAt FROM biz_user WHERE tenant_id = #{tenantId} AND email = #{email}")
    BizUser findByTenantAndEmail(@Param("tenantId") long tenantId, @Param("email") String email);

    @Select("SELECT id, tenant_id AS tenantId, email, name, password_hash AS passwordHash, role, avatar, remark_name AS remarkName, job_title AS jobTitle, created_at AS createdAt, updated_at AS updatedAt FROM biz_user WHERE tenant_id = #{tenantId} ORDER BY id")
    List<BizUser> listByTenant(@Param("tenantId") long tenantId);

    @Insert("INSERT INTO biz_user (tenant_id, email, name, password_hash, role, avatar, remark_name, job_title) VALUES (#{tenantId}, #{email}, #{name}, #{passwordHash}, #{role}, #{avatar}, #{remarkName}, #{jobTitle})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizUser user);

    @Update("UPDATE biz_user SET name = #{name}, password_hash = #{passwordHash}, role = #{role}, avatar = #{avatar}, remark_name = #{remarkName}, job_title = #{jobTitle} WHERE id = #{id} AND tenant_id = #{tenantId}")
    int update(BizUser user);
}
