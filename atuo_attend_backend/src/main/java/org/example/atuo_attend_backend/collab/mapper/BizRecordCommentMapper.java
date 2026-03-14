package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizRecordComment;

import java.util.List;

@Mapper
public interface BizRecordCommentMapper {

    @Select("SELECT * FROM biz_record_comment WHERE record_id = #{recordId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<BizRecordComment> listByRecordId(@Param("recordId") long recordId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM biz_record_comment WHERE record_id = #{recordId}")
    long countByRecordId(@Param("recordId") long recordId);

    @Insert("INSERT INTO biz_record_comment (record_id, user_id, content) VALUES (#{recordId}, #{userId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizRecordComment comment);
}
