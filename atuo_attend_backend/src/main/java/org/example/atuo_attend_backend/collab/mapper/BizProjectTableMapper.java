package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;

import java.util.List;

@Mapper
public interface BizProjectTableMapper {

    @Select("SELECT id, tenant_id AS tenantId, project_id AS projectId, name, purpose, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_project_table WHERE id = #{id}")
    BizProjectTable findById(@Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, project_id AS projectId, name, purpose, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_project_table WHERE project_id = #{projectId} AND purpose = #{purpose}")
    BizProjectTable findByProjectIdAndPurpose(@Param("projectId") long projectId, @Param("purpose") String purpose);

    @Select("SELECT id, tenant_id AS tenantId, project_id AS projectId, name, purpose, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_project_table WHERE project_id = #{projectId} ORDER BY id")
    List<BizProjectTable> listByProjectId(@Param("projectId") long projectId);

    @Insert("INSERT INTO biz_project_table (tenant_id, project_id, name, purpose) VALUES (#{tenantId}, #{projectId}, #{name}, #{purpose})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizProjectTable table);
}
