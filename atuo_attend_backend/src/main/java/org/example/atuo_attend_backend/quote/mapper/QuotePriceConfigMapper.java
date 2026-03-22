package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuotePriceConfigRow;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuotePriceConfigMapper {

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, duration_coefficient AS durationCoefficient, currency, enabled FROM biz_quote_price_config WHERE enabled = 1 ORDER BY id")
    List<Map<String, Object>> listEnabled();

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, duration_coefficient AS durationCoefficient, currency, enabled FROM biz_quote_price_config ORDER BY id")
    List<Map<String, Object>> listAll();

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, duration_coefficient AS durationCoefficient, currency, enabled FROM biz_quote_price_config WHERE id = #{id} LIMIT 1")
    Map<String, Object> findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM biz_quote_price_config WHERE region_label = #{r}")
    int countByRegionLabel(@Param("r") String r);

    @Select("SELECT COUNT(*) FROM biz_quote_price_config WHERE region_label = #{r} AND id <> #{excludeId}")
    int countByRegionLabelExcluding(@Param("r") String r, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM biz_quote_price_config WHERE enabled = 1")
    int countEnabled();

    @Insert("INSERT INTO biz_quote_price_config (region_label, price_per_day, duration_coefficient, currency, enabled) VALUES (#{regionLabel}, #{pricePerDay}, #{durationCoefficient}, #{currency}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuotePriceConfigRow row);

    @Update("UPDATE biz_quote_price_config SET region_label = #{regionLabel}, price_per_day = #{pricePerDay}, duration_coefficient = #{durationCoefficient}, currency = #{currency}, enabled = #{enabled} WHERE id = #{id}")
    int update(QuotePriceConfigRow row);

    @Delete("DELETE FROM biz_quote_price_config WHERE id = #{id}")
    int deleteById(@Param("id") long id);
}
