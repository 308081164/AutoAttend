package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteResult;

import java.util.List;

@Mapper
public interface QuoteResultMapper {

    @Insert("""
            INSERT INTO biz_quote_result (quote_project_id, total_days, estimated_duration_days, base_amount, risk_pct_total, risk_amount, final_amount,
                confidence_score, audit_checklist_json, selected_risks_json, price_per_day_used, duration_coefficient_used, region_label_used)
            VALUES (#{quoteProjectId}, #{totalDays}, #{estimatedDurationDays}, #{baseAmount}, #{riskPctTotal}, #{riskAmount}, #{finalAmount},
                #{confidenceScore}, #{auditChecklistJson}, #{selectedRisksJson}, #{pricePerDayUsed}, #{durationCoefficientUsed}, #{regionLabelUsed})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteResult r);

    @Select("SELECT id, quote_project_id AS quoteProjectId, total_days AS totalDays, estimated_duration_days AS estimatedDurationDays, base_amount AS baseAmount, " +
            "risk_pct_total AS riskPctTotal, risk_amount AS riskAmount, final_amount AS finalAmount, " +
            "confidence_score AS confidenceScore, audit_checklist_json AS auditChecklistJson, " +
            "selected_risks_json AS selectedRisksJson, price_per_day_used AS pricePerDayUsed, " +
            "duration_coefficient_used AS durationCoefficientUsed, region_label_used AS regionLabelUsed, created_at AS createdAt FROM biz_quote_result WHERE id = #{id}")
    QuoteResult findById(@Param("id") long id);

    @Select("SELECT id, quote_project_id AS quoteProjectId, total_days AS totalDays, estimated_duration_days AS estimatedDurationDays, base_amount AS baseAmount, " +
            "risk_pct_total AS riskPctTotal, risk_amount AS riskAmount, final_amount AS finalAmount, " +
            "confidence_score AS confidenceScore, audit_checklist_json AS auditChecklistJson, " +
            "selected_risks_json AS selectedRisksJson, price_per_day_used AS pricePerDayUsed, " +
            "duration_coefficient_used AS durationCoefficientUsed, region_label_used AS regionLabelUsed, created_at AS createdAt FROM biz_quote_result " +
            "WHERE quote_project_id = #{projectId} ORDER BY id DESC LIMIT 1")
    QuoteResult findLatestByProjectId(@Param("projectId") long projectId);

    @Select("SELECT id FROM biz_quote_result WHERE quote_project_id = #{projectId}")
    List<Long> listIdsByProjectId(@Param("projectId") long projectId);

    @Delete("DELETE FROM biz_quote_result WHERE quote_project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") long projectId);
}
