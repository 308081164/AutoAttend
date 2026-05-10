package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.ai.domain.ProjectAiLinkageConfig;

@Mapper
public interface ProjectAiLinkageConfigMapper {

    @Select("""
            SELECT id, project_id AS projectId, automation_mode AS automationMode,
                   min_confidence AS minConfidence,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_project_ai_linkage_config
            WHERE project_id = #{projectId}
            LIMIT 1
            """)
    ProjectAiLinkageConfig findByProjectId(@Param("projectId") long projectId);

    @Insert("""
            INSERT INTO aa_project_ai_linkage_config (project_id, automation_mode, min_confidence)
            VALUES (#{projectId}, #{automationMode}, #{minConfidence})
            ON DUPLICATE KEY UPDATE
              automation_mode = VALUES(automation_mode),
              min_confidence = VALUES(min_confidence),
              updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(ProjectAiLinkageConfig cfg);
}
