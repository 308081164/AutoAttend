package org.example.atuo_attend_backend.prototype.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockup;

@Mapper
public interface UiPrototypeMockupMapper {

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId,
                   html, css,
                   raw_ai_content AS rawAiContent,
                   messages_json AS messagesJson,
                   model_used AS modelUsed,
                   repaired,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_ui_prototype_mockup
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            LIMIT 1
            """)
    UiPrototypeMockup findByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Insert("""
            INSERT INTO aa_ui_prototype_mockup
              (tenant_id, project_id, html, css, raw_ai_content, messages_json, model_used, repaired)
            VALUES
              (#{tenantId}, #{projectId}, #{html}, #{css}, #{rawAiContent}, #{messagesJson}, #{modelUsed}, #{repaired})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UiPrototypeMockup row);

    @Update("""
            UPDATE aa_ui_prototype_mockup
            SET html=#{html},
                css=#{css},
                raw_ai_content=#{rawAiContent},
                messages_json=#{messagesJson},
                model_used=#{modelUsed},
                repaired=#{repaired},
                updated_at=CURRENT_TIMESTAMP
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int updateByProjectId(UiPrototypeMockup row);

    @Delete("""
            DELETE FROM aa_ui_prototype_mockup
            WHERE tenant_id=#{tenantId} AND project_id=#{projectId}
            """)
    int deleteByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);
}

