package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteItem;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface QuoteItemMapper {

    @Insert("INSERT INTO biz_quote_item (module_id, name, complexity, quantity, estimated_days, sort_order) " +
            "VALUES (#{moduleId}, #{name}, #{complexity}, #{quantity}, #{estimatedDays}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteItem item);

    @Update("UPDATE biz_quote_item SET estimated_days = #{days} WHERE id = #{id}")
    int updateEstimatedDays(@Param("id") long id, @Param("days") BigDecimal days);

    @Select("SELECT id, module_id AS moduleId, name, complexity, quantity, estimated_days AS estimatedDays, sort_order AS sortOrder " +
            "FROM biz_quote_item WHERE module_id = #{moduleId} ORDER BY sort_order, id")
    List<QuoteItem> listByModuleId(@Param("moduleId") long moduleId);
}
