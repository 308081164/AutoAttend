package org.example.atuo_attend_backend.agent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.atuo_attend_backend.agent.domain.AgentMessage;

import java.util.List;

/**
 * Agent 消息 Mapper
 */
@Mapper
public interface AgentMessageMapper {

    @Insert("""
            INSERT INTO aa_agent_message (session_id, role, content, content_type,
                attachment_id, attachment_name, input_tokens, output_tokens)
            VALUES (#{sessionId}, #{role}, #{content}, #{contentType},
                #{attachmentId}, #{attachmentName}, #{inputTokens}, #{outputTokens})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentMessage message);

    @Select("""
            SELECT id, session_id AS sessionId, role, content, content_type AS contentType,
                   attachment_id AS attachmentId, attachment_name AS attachmentName,
                   input_tokens AS inputTokens, output_tokens AS outputTokens, created_at AS createdAt
            FROM aa_agent_message
            WHERE session_id = #{sessionId}
            ORDER BY created_at ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AgentMessage> listBySessionId(@Param("sessionId") Long sessionId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*) FROM aa_agent_message WHERE session_id = #{sessionId}
            """)
    int countBySessionId(@Param("sessionId") Long sessionId);
}
