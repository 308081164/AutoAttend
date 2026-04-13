package org.example.atuo_attend_backend.agent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.atuo_attend_backend.agent.domain.AgentSession;

import java.util.List;

/**
 * Agent 会话 Mapper
 */
@Mapper
public interface AgentSessionMapper {

    @Insert("""
            INSERT INTO aa_agent_session (tenant_id, project_id, public_token, status,
                project_context, background_context, background_sources, summary_text,
                ended_at, ended_by, total_messages, total_input_tokens, total_output_tokens)
            VALUES (#{tenantId}, #{projectId}, #{publicToken}, #{status},
                #{projectContext}, #{backgroundContext}, #{backgroundSources}, #{summaryText},
                #{endedAt}, #{endedBy}, #{totalMessages}, #{totalInputTokens}, #{totalOutputTokens})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentSession session);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, public_token AS publicToken,
                   status, project_context AS projectContext, background_context AS backgroundContext,
                   background_sources AS backgroundSources, summary_text AS summaryText,
                   ended_at AS endedAt, ended_by AS endedBy, total_messages AS totalMessages,
                   total_input_tokens AS totalInputTokens, total_output_tokens AS totalOutputTokens,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_agent_session
            WHERE id = #{id}
            """)
    AgentSession findById(@Param("id") Long id);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, public_token AS publicToken,
                   status, project_context AS projectContext, background_context AS backgroundContext,
                   background_sources AS backgroundSources, summary_text AS summaryText,
                   ended_at AS endedAt, ended_by AS endedBy, total_messages AS totalMessages,
                   total_input_tokens AS totalInputTokens, total_output_tokens AS totalOutputTokens,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_agent_session
            WHERE public_token = #{publicToken}
            """)
    AgentSession findByPublicToken(@Param("publicToken") String publicToken);

    @Update("<script>" +
            "UPDATE aa_agent_session SET updated_at = NOW()" +
            "<if test='status != null'>, status = #{status}</if>" +
            "<if test='projectContext != null'>, project_context = #{projectContext}</if>" +
            "<if test='backgroundContext != null'>, background_context = #{backgroundContext}</if>" +
            "<if test='backgroundSources != null'>, background_sources = #{backgroundSources}</if>" +
            "<if test='summaryText != null'>, summary_text = #{summaryText}</if>" +
            "<if test='endedAt != null'>, ended_at = #{endedAt}</if>" +
            "<if test='endedBy != null'>, ended_by = #{endedBy}</if>" +
            "<if test='totalMessages != null'>, total_messages = #{totalMessages}</if>" +
            "<if test='totalInputTokens != null'>, total_input_tokens = #{totalInputTokens}</if>" +
            "<if test='totalOutputTokens != null'>, total_output_tokens = #{totalOutputTokens}</if>" +
            " WHERE id = #{id}" +
            "</script>")
    int update(AgentSession session);

    @Select("""
            SELECT id, tenant_id AS tenantId, project_id AS projectId, public_token AS publicToken,
                   status, project_context AS projectContext, background_context AS backgroundContext,
                   background_sources AS backgroundSources, summary_text AS summaryText,
                   ended_at AS endedAt, ended_by AS endedBy, total_messages AS totalMessages,
                   total_input_tokens AS totalInputTokens, total_output_tokens AS totalOutputTokens,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_agent_session
            WHERE project_id = #{projectId}
            ORDER BY created_at DESC
            """)
    List<AgentSession> listByProjectId(@Param("projectId") Long projectId);
}
