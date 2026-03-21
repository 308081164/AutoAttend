package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuoteRiskConfigMapper {

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config WHERE enabled = 1 ORDER BY id")
    List<Map<String, Object>> listEnabled();

    @Select("SELECT id, risk_key AS riskKey, label, default_pct AS defaultPct, enabled FROM biz_quote_risk_config ORDER BY id")
    List<Map<String, Object>> listAll();
}
