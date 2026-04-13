package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizProjectClientBoard;

@Mapper
public interface BizProjectClientBoardMapper {

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, enabled, public_token AS publicToken,
            show_progress_dashboard AS showProgressDashboard, show_feature_backlog AS showFeatureBacklog,
            show_ai_table_entry AS showAiTableEntry, ai_purpose AS aiPurpose,
            created_at AS createdAt, updated_at AS updatedAt
            FROM biz_project_client_board WHERE project_id = #{projectId}
            """)
    BizProjectClientBoard findByProjectId(@Param("projectId") long projectId);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, enabled, public_token AS publicToken,
            show_progress_dashboard AS showProgressDashboard, show_feature_backlog AS showFeatureBacklog,
            show_ai_table_entry AS showAiTableEntry, ai_purpose AS aiPurpose,
            created_at AS createdAt, updated_at AS updatedAt
            FROM biz_project_client_board WHERE public_token = #{token}
            """)
    BizProjectClientBoard findByPublicToken(@Param("token") String token);

    @Insert("""
            INSERT INTO biz_project_client_board (tenant_id, project_id, enabled, public_token,
            show_progress_dashboard, show_feature_backlog, show_ai_table_entry, ai_purpose)
            VALUES (#{tenantId}, #{projectId}, #{enabled}, #{publicToken},
            #{showProgressDashboard}, #{showFeatureBacklog}, #{showAiTableEntry}, #{aiPurpose})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizProjectClientBoard row);

    @Update("""
            UPDATE biz_project_client_board SET
            enabled = #{enabled},
            public_token = #{publicToken},
            show_progress_dashboard = #{showProgressDashboard},
            show_feature_backlog = #{showFeatureBacklog},
            show_ai_table_entry = #{showAiTableEntry},
            ai_purpose = #{aiPurpose}
            WHERE id = #{id} AND tenant_id = #{tenantId}
            """)
    int update(BizProjectClientBoard row);

    @Select("""
            SELECT COUNT(*) FROM biz_project_client_board
            WHERE tenant_id = #{tenantId} AND enabled = 1
            """)
    long countEnabledByTenant(@Param("tenantId") long tenantId);
}
