package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizProject;

import java.util.List;

@Mapper
public interface BizProjectMapper {

    @Select("SELECT id, tenant_id AS tenantId, name, description, repo_id AS repoId, status, created_at AS createdAt, updated_at AS updatedAt FROM biz_project WHERE id = #{id}")
    BizProject findById(@Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, name, description, repo_id AS repoId, status, created_at AS createdAt, updated_at AS updatedAt FROM biz_project WHERE tenant_id = #{tenantId} AND repo_id = #{repoId}")
    BizProject findByTenantAndRepoId(@Param("tenantId") long tenantId, @Param("repoId") String repoId);

    @Select("SELECT id, tenant_id AS tenantId, name, description, repo_id AS repoId, status, created_at AS createdAt, updated_at AS updatedAt FROM biz_project WHERE tenant_id = #{tenantId} ORDER BY id")
    List<BizProject> listByTenant(@Param("tenantId") long tenantId);

    @Insert("INSERT INTO biz_project (tenant_id, name, description, repo_id, status) VALUES (#{tenantId}, #{name}, #{description}, #{repoId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizProject project);

    @Update("UPDATE biz_project SET name = #{name}, description = #{description}, status = #{status} WHERE id = #{id} AND tenant_id = #{tenantId}")
    int update(BizProject project);

    @Select("SELECT COUNT(*) FROM biz_project WHERE tenant_id = #{tenantId}")
    long countByTenant(@Param("tenantId") long tenantId);

    @Select({
            "<script>",
            "SELECT id, tenant_id AS tenantId, name, description, repo_id AS repoId, status, created_at AS createdAt, updated_at AS updatedAt FROM biz_project WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "ORDER BY id",
            "</script>"
    })
    List<BizProject> listByIds(@Param("ids") List<Long> ids);
}
