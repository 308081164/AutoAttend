package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizRecordField;

import java.util.List;

@Mapper
public interface BizRecordFieldMapper {

    @Select("SELECT * FROM biz_record_field WHERE record_id = #{recordId}")
    List<BizRecordField> listByRecordId(@Param("recordId") long recordId);

    @Select("SELECT * FROM biz_record_field WHERE record_id = #{recordId} AND column_id = #{columnId}")
    BizRecordField findByRecordAndColumn(@Param("recordId") long recordId, @Param("columnId") long columnId);

    @Insert("INSERT INTO biz_record_field (record_id, column_id, value_text, value_number, value_date, value_json) VALUES (#{recordId}, #{columnId}, #{valueText}, #{valueNumber}, #{valueDate}, #{valueJson})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizRecordField field);

    @Update("UPDATE biz_record_field SET value_text = #{valueText}, value_number = #{valueNumber}, value_date = #{valueDate}, value_json = #{valueJson} WHERE id = #{id}")
    int update(BizRecordField field);

    @Delete("DELETE FROM biz_record_field WHERE record_id = #{recordId}")
    int deleteByRecordId(@Param("recordId") long recordId);
}
