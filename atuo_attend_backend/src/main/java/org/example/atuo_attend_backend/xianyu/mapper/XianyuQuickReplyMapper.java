package org.example.atuo_attend_backend.xianyu.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.xianyu.domain.XianyuQuickReply;

import java.util.List;

@Mapper
public interface XianyuQuickReplyMapper {

    @Insert("INSERT INTO aa_xianyu_quick_reply (tenant_id, title, content, sort_order, created_at, updated_at) " +
            "VALUES (#{tenantId}, #{title}, #{content}, #{sortOrder}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuQuickReply reply);

    @Update("UPDATE aa_xianyu_quick_reply SET title=#{title}, content=#{content}, sort_order=#{sortOrder}, updated_at=NOW() WHERE id=#{id}")
    int update(XianyuQuickReply reply);

    @Delete("DELETE FROM aa_xianyu_quick_reply WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT * FROM aa_xianyu_quick_reply WHERE id=#{id}")
    XianyuQuickReply selectById(@Param("id") Long id);

    @Select("SELECT * FROM aa_xianyu_quick_reply WHERE tenant_id=#{tenantId} ORDER BY sort_order ASC, created_at ASC")
    List<XianyuQuickReply> selectByTenantId(@Param("tenantId") Long tenantId);
}
