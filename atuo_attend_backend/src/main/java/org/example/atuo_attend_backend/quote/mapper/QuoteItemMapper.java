package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteItem;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface QuoteItemMapper {

    @Insert("INSERT INTO biz_quote_item (tenant_id, module_id, name, complexity, quantity, estimated_days, sort_order, excluded_from_scale, line_price_snap, line_price_adjusted) " +
            "VALUES (#{tenantId}, #{moduleId}, #{name}, #{complexity}, #{quantity}, #{estimatedDays}, #{sortOrder}, " +
            "#{excludedFromScale}, #{linePriceSnap}, #{linePriceAdjusted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteItem item);

    @Update("UPDATE biz_quote_item SET estimated_days = #{days} WHERE tenant_id = #{tenantId} AND id = #{id}")
    int updateEstimatedDays(@Param("tenantId") long tenantId, @Param("id") long id, @Param("days") BigDecimal days);

    @Select("SELECT id, tenant_id AS tenantId, module_id AS moduleId, name, complexity, quantity, estimated_days AS estimatedDays, sort_order AS sortOrder, " +
            "excluded_from_scale AS excludedFromScale, line_price_snap AS linePriceSnap, line_price_adjusted AS linePriceAdjusted " +
            "FROM biz_quote_item WHERE tenant_id = #{tenantId} AND module_id = #{moduleId} ORDER BY sort_order, id")
    List<QuoteItem> listByModuleId(@Param("tenantId") long tenantId, @Param("moduleId") long moduleId);

    @Delete("DELETE FROM biz_quote_item WHERE tenant_id = #{tenantId} AND module_id = #{moduleId}")
    int deleteByModuleId(@Param("tenantId") long tenantId, @Param("moduleId") long moduleId);

    @Update("UPDATE biz_quote_item SET line_price_snap = #{snap}, line_price_adjusted = #{adj} WHERE tenant_id = #{tenantId} AND id = #{id}")
    int updateLinePrices(@Param("tenantId") long tenantId, @Param("id") long id,
                         @Param("snap") BigDecimal snap, @Param("adj") BigDecimal adj);
}
