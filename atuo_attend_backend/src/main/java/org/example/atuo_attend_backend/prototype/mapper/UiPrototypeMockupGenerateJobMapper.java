package org.example.atuo_attend_backend.prototype.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockupGenerateJob;

@Mapper
public interface UiPrototypeMockupGenerateJobMapper {

    @Insert("""
            INSERT INTO aa_ui_prototype_mockup_generate_job (tenant_id, project_id, status, prompt_snapshot, model)
            VALUES (#{tenantId}, #{projectId}, #{status}, #{promptSnapshot}, #{model})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UiPrototypeMockupGenerateJob row);

    @Update("""
            UPDATE aa_ui_prototype_mockup_generate_job
            SET status=#{status}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateStatus(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                     @Param("status") String status);

    @Update("""
            UPDATE aa_ui_prototype_mockup_generate_job
            SET status='success', error_message=NULL, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateSuccess(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Update("""
            UPDATE aa_ui_prototype_mockup_generate_job
            SET status='failed', error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateFailed(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                     @Param("errorMessage") String errorMessage);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, status,
                   error_message AS errorMessage,
                   prompt_snapshot AS promptSnapshot,
                   model,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_mockup_generate_job
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            LIMIT 1
            """)
    UiPrototypeMockupGenerateJob findById(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Delete("""
            DELETE FROM aa_ui_prototype_mockup_generate_job
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int deleteByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);
}

