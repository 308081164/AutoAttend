package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuotePriceConfigRow;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuotePriceConfigMapper {

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, duration_coefficient AS durationCoefficient, currency, enabled FROM biz_quote_price_config WHERE tenant_id = #{tenantId} AND enabled = 1 ORDER BY id")
    List<Map<String, Object>> listEnabled(@Param("tenantId") long tenantId);

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, duration_coefficient AS durationCoefficient, currency, enabled FROM biz_quote_price_config WHERE tenant_id = #{tenantId} ORDER BY id")
    List<Map<String, Object>> listAll(@Param("tenantId") long tenantId);

    @Select("SELECT id, region_label AS regionLabel, price_per_day AS pricePerDay, duration_coefficient AS durationCoefficient, currency, enabled FROM biz_quote_price_config WHERE tenant_id = #{tenantId} AND id = #{id} LIMIT 1")
    Map<String, Object> findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("SELECT COUNT(*) FROM biz_quote_price_config WHERE tenant_id = #{tenantId} AND region_label = #{r}")
    int countByRegionLabel(@Param("tenantId") long tenantId, @Param("r") String r);

    @Select("SELECT COUNT(*) FROM biz_quote_price_config WHERE tenant_id = #{tenantId} AND region_label = #{r} AND id <> #{excludeId}")
    int countByRegionLabelExcluding(@Param("tenantId") long tenantId, @Param("r") String r, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM biz_quote_price_config WHERE tenant_id = #{tenantId} AND enabled = 1")
    int countEnabled(@Param("tenantId") long tenantId);

    @Insert("INSERT INTO biz_quote_price_config (tenant_id, region_label, price_per_day, duration_coefficient, currency, enabled) VALUES (#{tenantId}, #{regionLabel}, #{pricePerDay}, #{durationCoefficient}, #{currency}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuotePriceConfigRow row);

    @Update("UPDATE biz_quote_price_config SET region_label = #{regionLabel}, price_per_day = #{pricePerDay}, duration_coefficient = #{durationCoefficient}, currency = #{currency}, enabled = #{enabled} WHERE tenant_id = #{tenantId} AND id = #{id}")
    int update(QuotePriceConfigRow row);

    @Delete("DELETE FROM biz_quote_price_config WHERE tenant_id = #{tenantId} AND id = #{id}")
    int deleteById(@Param("tenantId") long tenantId, @Param("id") long id);
}
