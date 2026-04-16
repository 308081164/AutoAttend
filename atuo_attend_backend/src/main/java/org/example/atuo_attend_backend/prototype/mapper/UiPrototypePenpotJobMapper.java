package org.example.atuo_attend_backend.prototype.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypePenpotJob;

@Mapper
public interface UiPrototypePenpotJobMapper {

    @Insert("""
            INSERT INTO aa_ui_prototype_penpot_job (tenant_id, project_id, status, prompt_snapshot)
            VALUES (#{tenantId}, #{projectId}, #{status}, #{promptSnapshot})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UiPrototypePenpotJob row);

    @Update("""
            UPDATE aa_ui_prototype_penpot_job
            SET status=#{status}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateStatus(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                     @Param("status") String status);

    @Update("""
            UPDATE aa_ui_prototype_penpot_job
            SET status='success', error_message=NULL,
                penpot_file_id=#{penpotFileId}, penpot_preview_url=#{penpotPreviewUrl},
                updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateSuccess(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                      @Param("penpotFileId") String penpotFileId, @Param("penpotPreviewUrl") String penpotPreviewUrl);

    @Update("""
            UPDATE aa_ui_prototype_penpot_job
            SET status='failed', error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateFailed(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                     @Param("errorMessage") String errorMessage);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, status,
                   error_message AS errorMessage, prompt_snapshot AS promptSnapshot,
                   penpot_file_id AS penpotFileId, penpot_preview_url AS penpotPreviewUrl,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_penpot_job
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            LIMIT 1
            """)
    UiPrototypePenpotJob findById(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Delete("""
            DELETE FROM aa_ui_prototype_penpot_job
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int deleteByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);
}
