package org.example.atuo_attend_backend.quote.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class QuoteResult {
    private Long id;
    private Long tenantId;
    private Long quoteProjectId;
    private BigDecimal totalDays;
    private BigDecimal estimatedDurationDays;
    private BigDecimal baseAmount;
    private BigDecimal riskPctTotal;
    private BigDecimal riskAmount;
    private BigDecimal finalAmount;
    /** 模型输出的总价（调价前基线），与首次计算时 final_amount 一致 */
    private BigDecimal baselineFinalAmount;
    /** 作用于可缩放明细行的商务系数 */
    private BigDecimal priceScaleFactor;
    /** 商务调价后总价（对外展示） */
    private BigDecimal adjustedFinalAmount;
    /** 调价备注 */
    private String priceAdjustNote;
    private int confidenceScore;
    private String auditChecklistJson;
    private String selectedRisksJson;
    private BigDecimal pricePerDayUsed;
    private BigDecimal durationCoefficientUsed;
    private String regionLabelUsed;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getQuoteProjectId() { return quoteProjectId; }
    public void setQuoteProjectId(Long quoteProjectId) { this.quoteProjectId = quoteProjectId; }
    public BigDecimal getTotalDays() { return totalDays; }
    public void setTotalDays(BigDecimal totalDays) { this.totalDays = totalDays; }
    public BigDecimal getEstimatedDurationDays() { return estimatedDurationDays; }
    public void setEstimatedDurationDays(BigDecimal estimatedDurationDays) { this.estimatedDurationDays = estimatedDurationDays; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public BigDecimal getRiskPctTotal() { return riskPctTotal; }
    public void setRiskPctTotal(BigDecimal riskPctTotal) { this.riskPctTotal = riskPctTotal; }
    public BigDecimal getRiskAmount() { return riskAmount; }
    public void setRiskAmount(BigDecimal riskAmount) { this.riskAmount = riskAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public BigDecimal getBaselineFinalAmount() { return baselineFinalAmount; }
    public void setBaselineFinalAmount(BigDecimal baselineFinalAmount) { this.baselineFinalAmount = baselineFinalAmount; }
    public BigDecimal getPriceScaleFactor() { return priceScaleFactor; }
    public void setPriceScaleFactor(BigDecimal priceScaleFactor) { this.priceScaleFactor = priceScaleFactor; }
    public BigDecimal getAdjustedFinalAmount() { return adjustedFinalAmount; }
    public void setAdjustedFinalAmount(BigDecimal adjustedFinalAmount) { this.adjustedFinalAmount = adjustedFinalAmount; }
    public String getPriceAdjustNote() { return priceAdjustNote; }
    public void setPriceAdjustNote(String priceAdjustNote) { this.priceAdjustNote = priceAdjustNote; }
    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getAuditChecklistJson() { return auditChecklistJson; }
    public void setAuditChecklistJson(String auditChecklistJson) { this.auditChecklistJson = auditChecklistJson; }
    public String getSelectedRisksJson() { return selectedRisksJson; }
    public void setSelectedRisksJson(String selectedRisksJson) { this.selectedRisksJson = selectedRisksJson; }
    public BigDecimal getPricePerDayUsed() { return pricePerDayUsed; }
    public void setPricePerDayUsed(BigDecimal pricePerDayUsed) { this.pricePerDayUsed = pricePerDayUsed; }
    public BigDecimal getDurationCoefficientUsed() { return durationCoefficientUsed; }
    public void setDurationCoefficientUsed(BigDecimal durationCoefficientUsed) { this.durationCoefficientUsed = durationCoefficientUsed; }
    public String getRegionLabelUsed() { return regionLabelUsed; }
    public void setRegionLabelUsed(String regionLabelUsed) { this.regionLabelUsed = regionLabelUsed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
