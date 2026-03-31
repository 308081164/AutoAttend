package org.example.atuo_attend_backend.prototype.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeSpec;

import java.util.List;

@Mapper
public interface UiPrototypeSpecMapper {

    @Insert("""
            INSERT INTO aa_ui_prototype_spec (tenant_id, project_id, version, spec_json)
            VALUES (#{tenantId}, #{projectId}, #{version}, #{specJson})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("projectId") long projectId,
               @Param("version") int version,
               @Param("specJson") String specJson);

    @Select("""
            SELECT COALESCE(MAX(version), 0)
            FROM aa_ui_prototype_spec
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int maxVersion(@Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId,
                   version, spec_json AS specJson,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_spec
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            ORDER BY version DESC
            """)
    List<UiPrototypeSpec> listSpecs(@Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId,
                   version, spec_json AS specJson,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_spec
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId} AND version=#{version}
            LIMIT 1
            """)
    UiPrototypeSpec findByVersion(@Param("tenantId") long tenantId, @Param("projectId") long projectId, @Param("version") int version);

    @Delete("""
            DELETE FROM aa_ui_prototype_spec
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int deleteByProject(@Param("tenantId") long tenantId, @Param("projectId") long projectId);
}

