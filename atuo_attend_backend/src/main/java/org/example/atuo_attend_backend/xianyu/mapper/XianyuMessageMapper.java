package org.example.atuo_attend_backend.xianyu.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.xianyu.domain.XianyuMessage;

import java.util.List;

@Mapper
public interface XianyuMessageMapper {

    @Insert("INSERT INTO aa_xianyu_message (conversation_id, direction, content, msg_type, file_url, sent_at, created_at) " +
            "VALUES (#{conversationId}, #{direction}, #{content}, #{msgType}, #{fileUrl}, #{sentAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuMessage message);

    @Select("SELECT * FROM aa_xianyu_message WHERE conversation_id=#{conversationId} ORDER BY sent_at ASC")
    List<XianyuMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT * FROM aa_xianyu_message WHERE conversation_id=#{conversationId} ORDER BY sent_at DESC LIMIT #{limit}")
    List<XianyuMessage> selectRecentByConversationId(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    @Delete("DELETE FROM aa_xianyu_message WHERE conversation_id=#{conversationId}")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
