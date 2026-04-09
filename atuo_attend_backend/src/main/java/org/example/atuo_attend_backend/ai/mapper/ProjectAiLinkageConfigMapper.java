package org.example.atuo_attend_backend.ai.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.ai.domain.ProjectAiLinkageConfig;

@Mapper
public interface ProjectAiLinkageConfigMapper {

    @Select("""
            SELECT id, project_id AS projectId, enabled, mode, min_confidence AS minConfidence,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_project_ai_linkage_config
            WHERE project_id = #{projectId}
            LIMIT 1
            """)
    ProjectAiLinkageConfig findByProjectId(@Param("projectId") long projectId);

    @Insert("""
            INSERT INTO aa_project_ai_linkage_config (project_id, enabled, mode, min_confidence)
            VALUES (#{projectId}, #{enabled}, #{mode}, #{minConfidence})
            ON DUPLICATE KEY UPDATE
              enabled = VALUES(enabled),
              mode = VALUES(mode),
              min_confidence = VALUES(min_confidence),
              updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(ProjectAiLinkageConfig cfg);
}

