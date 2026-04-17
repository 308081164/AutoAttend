package org.example.atuo_attend_backend.prototype.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypePenpotJob;

@Mapper
public interface UiPrototypePenpotJobMapper {

    @Insert("""
            INSERT INTO aa_ui_prototype_penpot_job (tenant_id, project_id, status, stage, progress, prompt_snapshot)
            VALUES (#{tenantId}, #{projectId}, #{status}, #{stage}, #{progress}, #{promptSnapshot})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UiPrototypePenpotJob row);

    @Update("""
            UPDATE aa_ui_prototype_penpot_job
            SET status=#{status}, stage=#{stage}, progress=#{progress},
                error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateStage(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                    @Param("status") String status, @Param("stage") String stage, @Param("progress") int progress,
                    @Param("errorMessage") String errorMessage);

    @Update("""
            UPDATE aa_ui_prototype_penpot_job
            SET retry_count = COALESCE(retry_count, 0) + 1, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int incrementRetry(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Update("""
            UPDATE aa_ui_prototype_penpot_job
            SET status='success', stage='done', progress=100, error_message=NULL,
                penpot_file_id=#{penpotFileId}, penpot_preview_url=#{penpotPreviewUrl},
                export_binfile_url=#{exportBinfileUrl},
                updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateSuccess(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                      @Param("penpotFileId") String penpotFileId,
                      @Param("penpotPreviewUrl") String penpotPreviewUrl,
                      @Param("exportBinfileUrl") String exportBinfileUrl);

    @Update("""
            UPDATE aa_ui_prototype_penpot_job
            SET status='failed', error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateFailed(@Param("id") long id, @Param("tenantId") long tenantId, @Param("projectId") long projectId,
                     @Param("errorMessage") String errorMessage);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, status, stage, progress, retry_count AS retryCount,
                   error_message AS errorMessage, prompt_snapshot AS promptSnapshot,
                   penpot_file_id AS penpotFileId, penpot_preview_url AS penpotPreviewUrl,
                   export_binfile_url AS exportBinfileUrl,
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
