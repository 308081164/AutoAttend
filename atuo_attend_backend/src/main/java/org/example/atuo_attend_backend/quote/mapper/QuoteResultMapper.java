package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteResult;

import java.util.List;

@Mapper
public interface QuoteResultMapper {

    @Insert("""
            INSERT INTO biz_quote_result (tenant_id, quote_project_id, total_days, estimated_duration_days, base_amount, risk_pct_total, risk_amount, final_amount,
                baseline_final_amount, price_scale_factor, adjusted_final_amount, price_adjust_note,
                confidence_score, audit_checklist_json, selected_risks_json, price_per_day_used, duration_coefficient_used, region_label_used)
            VALUES (#{tenantId}, #{quoteProjectId}, #{totalDays}, #{estimatedDurationDays}, #{baseAmount}, #{riskPctTotal}, #{riskAmount}, #{finalAmount},
                #{baselineFinalAmount}, #{priceScaleFactor}, #{adjustedFinalAmount}, #{priceAdjustNote},
                #{confidenceScore}, #{auditChecklistJson}, #{selectedRisksJson}, #{pricePerDayUsed}, #{durationCoefficientUsed}, #{regionLabelUsed})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteResult r);

    @Select("SELECT id, tenant_id AS tenantId, quote_project_id AS quoteProjectId, total_days AS totalDays, estimated_duration_days AS estimatedDurationDays, base_amount AS baseAmount, " +
            "risk_pct_total AS riskPctTotal, risk_amount AS riskAmount, final_amount AS finalAmount, " +
            "baseline_final_amount AS baselineFinalAmount, price_scale_factor AS priceScaleFactor, adjusted_final_amount AS adjustedFinalAmount, price_adjust_note AS priceAdjustNote, " +
            "confidence_score AS confidenceScore, audit_checklist_json AS auditChecklistJson, " +
            "selected_risks_json AS selectedRisksJson, price_per_day_used AS pricePerDayUsed, " +
            "duration_coefficient_used AS durationCoefficientUsed, region_label_used AS regionLabelUsed, created_at AS createdAt FROM biz_quote_result WHERE tenant_id = #{tenantId} AND id = #{id}")
    QuoteResult findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, quote_project_id AS quoteProjectId, total_days AS totalDays, estimated_duration_days AS estimatedDurationDays, base_amount AS baseAmount, " +
            "risk_pct_total AS riskPctTotal, risk_amount AS riskAmount, final_amount AS finalAmount, " +
            "baseline_final_amount AS baselineFinalAmount, price_scale_factor AS priceScaleFactor, adjusted_final_amount AS adjustedFinalAmount, price_adjust_note AS priceAdjustNote, " +
            "confidence_score AS confidenceScore, audit_checklist_json AS auditChecklistJson, " +
            "selected_risks_json AS selectedRisksJson, price_per_day_used AS pricePerDayUsed, " +
            "duration_coefficient_used AS durationCoefficientUsed, region_label_used AS regionLabelUsed, created_at AS createdAt FROM biz_quote_result " +
            "WHERE tenant_id = #{tenantId} AND quote_project_id = #{projectId} ORDER BY id DESC LIMIT 1")
    QuoteResult findLatestByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Select("SELECT id FROM biz_quote_result WHERE tenant_id = #{tenantId} AND quote_project_id = #{projectId}")
    List<Long> listIdsByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Delete("DELETE FROM biz_quote_result WHERE tenant_id = #{tenantId} AND quote_project_id = #{projectId}")
    int deleteByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Update("UPDATE biz_quote_result SET final_amount = #{finalAmount}, baseline_final_amount = #{baselineFinalAmount}, " +
            "price_scale_factor = #{priceScaleFactor}, adjusted_final_amount = #{adjustedFinalAmount}, price_adjust_note = #{priceAdjustNote} " +
            "WHERE tenant_id = #{tenantId} AND id = #{id}")
    int updatePriceAdjustment(@Param("tenantId") long tenantId, @Param("id") long id,
                              @Param("finalAmount") java.math.BigDecimal finalAmount,
                              @Param("baselineFinalAmount") java.math.BigDecimal baselineFinalAmount,
                              @Param("priceScaleFactor") java.math.BigDecimal priceScaleFactor,
                              @Param("adjustedFinalAmount") java.math.BigDecimal adjustedFinalAmount,
                              @Param("priceAdjustNote") String priceAdjustNote);
}
