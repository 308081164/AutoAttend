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

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config WHERE tenant_id = #{tenantId} AND enabled = 1 AND risk_key <> 'urgency_rush' ORDER BY id")
    List<Map<String, Object>> listEnabledForCalculator(@Param("tenantId") long tenantId);

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config WHERE tenant_id = #{tenantId} AND enabled = 1 ORDER BY id")
    List<Map<String, Object>> listEnabled(@Param("tenantId") long tenantId);

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config WHERE tenant_id = #{tenantId} ORDER BY id")
    List<Map<String, Object>> listAll(@Param("tenantId") long tenantId);

    @Update("UPDATE biz_quote_risk_config SET label = #{label}, default_pct = #{defaultPct}, enabled = #{enabled} WHERE tenant_id = #{tenantId} AND id = #{id}")
    int updateRow(@Param("tenantId") long tenantId, @Param("id") long id, @Param("label") String label,
                  @Param("defaultPct") BigDecimal defaultPct, @Param("enabled") int enabled);

    @Select("SELECT COUNT(*) FROM biz_quote_risk_config WHERE tenant_id = #{tenantId} AND id = #{id}")
    int countById(@Param("tenantId") long tenantId, @Param("id") long id);
}
