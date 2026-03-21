package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface QuoteRiskConfigMapper {

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config WHERE enabled = 1 AND risk_key <> 'urgency_rush' ORDER BY id")
    List<Map<String, Object>> listEnabledForCalculator();

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config WHERE enabled = 1 ORDER BY id")
    List<Map<String, Object>> listEnabled();

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config ORDER BY id")
    List<Map<String, Object>> listAll();

    @Update("UPDATE biz_quote_risk_config SET label = #{label}, default_pct = #{defaultPct}, enabled = #{enabled} WHERE id = #{id}")
    int updateRow(@Param("id") long id, @Param("label") String label,
                  @Param("defaultPct") BigDecimal defaultPct, @Param("enabled") int enabled);

    @Select("SELECT COUNT(*) FROM biz_quote_risk_config WHERE id = #{id}")
    int countById(long id);
}
