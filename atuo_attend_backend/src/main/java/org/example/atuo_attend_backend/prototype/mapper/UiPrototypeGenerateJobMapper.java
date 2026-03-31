package org.example.atuo_attend_backend.prototype.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeGenerateJob;

@Mapper
public interface UiPrototypeGenerateJobMapper {

    @Insert("""
            INSERT INTO aa_ui_prototype_generate_job (tenant_id, project_id, status, prompt_snapshot)
            VALUES (#{tenantId}, #{projectId}, #{status}, #{promptSnapshot})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UiPrototypeGenerateJob row);

    @Update("""
            UPDATE aa_ui_prototype_generate_job
            SET status=#{status}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateStatus(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                     @Param("status") String status);

    @Update("""
            UPDATE aa_ui_prototype_generate_job
            SET status='success', spec_version=#{specVersion}, error_message=NULL, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateSuccess(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                      @Param("specVersion") int specVersion);

    @Update("""
            UPDATE aa_ui_prototype_generate_job
            SET status='failed', error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateFailed(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                     @Param("errorMessage") String errorMessage);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, status,
                   error_message AS errorMessage, spec_version AS specVersion,
                   prompt_snapshot AS promptSnapshot,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_generate_job
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            LIMIT 1
            """)
    UiPrototypeGenerateJob findById(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId);
}
