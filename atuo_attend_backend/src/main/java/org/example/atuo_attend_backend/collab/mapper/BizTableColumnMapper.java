package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizTableColumn;

import java.util.List;

@Mapper
public interface BizTableColumnMapper {

    @Select("SELECT * FROM biz_table_column WHERE id = #{id}")
    BizTableColumn findById(@Param("id") long id);

    @Select("SELECT * FROM biz_table_column WHERE table_id = #{tableId} ORDER BY sort_order, id")
    List<BizTableColumn> listByTableId(@Param("tableId") long tableId);

    @Insert("INSERT INTO biz_table_column (table_id, name, column_type, option_group_id, sort_order) VALUES (#{tableId}, #{name}, #{columnType}, #{optionGroupId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizTableColumn column);

    @Update("UPDATE biz_table_column SET name = #{name}, column_type = #{columnType}, option_group_id = #{optionGroupId}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(BizTableColumn column);
}
