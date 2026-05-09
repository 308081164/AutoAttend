package org.example.atuo_attend_backend.xianyu.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.xianyu.domain.XianyuConversation;

import java.util.List;

@Mapper
public interface XianyuConversationMapper {

    @Insert("INSERT INTO aa_xianyu_conversation (account_id, peer_id, peer_nickname, peer_avatar, last_message, last_message_at, unread_count, status, created_at, updated_at) " +
            "VALUES (#{accountId}, #{peerId}, #{peerNickname}, #{peerAvatar}, #{lastMessage}, #{lastMessageAt}, #{unreadCount}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuConversation conversation);

    @Update("UPDATE aa_xianyu_conversation SET peer_nickname=#{peerNickname}, peer_avatar=#{peerAvatar}, " +
            "last_message=#{lastMessage}, last_message_at=#{lastMessageAt}, unread_count=#{unreadCount}, " +
            "status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int update(XianyuConversation conversation);

    @Update("UPDATE aa_xianyu_conversation SET unread_count=unread_count+1, last_message=#{lastMessage}, " +
            "last_message_at=#{lastMessageAt}, updated_at=NOW() WHERE id=#{id}")
    int updateNewMessage(@Param("id") Long id, @Param("lastMessage") String lastMessage, @Param("lastMessageAt") java.time.LocalDateTime lastMessageAt);

    @Update("UPDATE aa_xianyu_conversation SET unread_count=0, updated_at=NOW() WHERE id=#{id}")
    int markAsRead(@Param("id") Long id);

    @Delete("DELETE FROM aa_xianyu_conversation WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT * FROM aa_xianyu_conversation WHERE id=#{id}")
    XianyuConversation selectById(@Param("id") Long id);

    @Select("SELECT * FROM aa_xianyu_conversation WHERE account_id=#{accountId} ORDER BY last_message_at DESC")
    List<XianyuConversation> selectByAccountId(@Param("accountId") Long accountId);

    @Select("SELECT * FROM aa_xianyu_conversation WHERE account_id=#{accountId} AND status='active' ORDER BY last_message_at DESC")
    List<XianyuConversation> selectActiveByAccountId(@Param("accountId") Long accountId);

    @Select("SELECT COUNT(*) FROM aa_xianyu_conversation WHERE account_id=#{accountId} AND unread_count>0")
    int countUnreadByAccountId(@Param("accountId") Long accountId);
}
