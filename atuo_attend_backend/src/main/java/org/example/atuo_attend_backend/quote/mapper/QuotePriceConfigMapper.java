package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuotePriceConfigMapper {

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, currency, enabled FROM biz_quote_price_config WHERE enabled = 1 ORDER BY id")
    List<Map<String, Object>> listEnabled();

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, currency, enabled FROM biz_quote_price_config WHERE id = #{id} LIMIT 1")
    Map<String, Object> findById(@Param("id") long id);
}
