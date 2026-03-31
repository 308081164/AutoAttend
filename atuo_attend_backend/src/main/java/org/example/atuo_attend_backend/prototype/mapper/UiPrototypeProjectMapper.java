package org.example.atuo_attend_backend.prototype.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeProject;

import java.util.List;

@Mapper
public interface UiPrototypeProjectMapper {

    @Insert("""
            INSERT INTO aa_ui_prototype_project (tenant_id, name, current_spec_version)
            VALUES (#{tenantId}, #{name}, #{currentSpecVersion})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UiPrototypeProject p);

    @Update("""
            UPDATE aa_ui_prototype_project
            SET name=#{name},
                updated_at=CURRENT_TIMESTAMP
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    int updateName(@Param("tenantId") long tenantId, @Param("id") long id, @Param("name") String name);

    @Update("""
            UPDATE aa_ui_prototype_project
            SET current_spec_version=#{currentSpecVersion},
                updated_at=CURRENT_TIMESTAMP
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    int updateCurrentSpecVersion(@Param("tenantId") long tenantId, @Param("id") long id, @Param("currentSpecVersion") Integer currentSpecVersion);

    @Select("""
            SELECT id, tenant_id AS tenantId, name,
                   current_spec_version AS currentSpecVersion,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_project
            WHERE tenant_id=#{tenantId}
            ORDER BY updated_at DESC, id DESC
            """)
    List<UiPrototypeProject> list(@Param("tenantId") long tenantId);

    @Select("""
            SELECT id, tenant_id AS tenantId, name,
                   current_spec_version AS currentSpecVersion,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_project
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    UiPrototypeProject findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Delete("""
            DELETE FROM aa_ui_prototype_project
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    int deleteById(@Param("tenantId") long tenantId, @Param("id") long id);
}

