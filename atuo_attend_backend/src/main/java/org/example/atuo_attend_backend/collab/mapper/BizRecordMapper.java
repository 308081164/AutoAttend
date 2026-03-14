package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizRecord;

import java.util.List;

@Mapper
public interface BizRecordMapper {

    @Select("SELECT * FROM biz_record WHERE id = #{id}")
    BizRecord findById(@Param("id") long id);

    @Select("SELECT * FROM biz_record WHERE table_id = #{tableId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<BizRecord> listByTableId(@Param("tableId") long tableId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM biz_record WHERE table_id = #{tableId}")
    long countByTableId(@Param("tableId") long tableId);

    @Insert("INSERT INTO biz_record (table_id, created_by) VALUES (#{tableId}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizRecord record);

    @Update("UPDATE biz_record SET created_by = #{createdBy} WHERE id = #{id}")
    int update(BizRecord record);

    @Delete("DELETE FROM biz_record WHERE id = #{id}")
    int deleteById(@Param("id") long id);
}
