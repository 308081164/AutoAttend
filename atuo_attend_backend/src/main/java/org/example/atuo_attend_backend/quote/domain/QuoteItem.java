package org.example.atuo_attend_backend.quote.domain;

import java.math.BigDecimal;

public class QuoteItem {
    private Long id;
    private Long tenantId;
    private Long moduleId;
    private String name;
    private String complexity;
    private int quantity;
    private BigDecimal estimatedDays;
    private int sortOrder;
    /** 不参与等比例商务调价（如第三方固定费用） */
    private Boolean excludedFromScale;
    /** 模型总价分摊到本行的基线金额 */
    private BigDecimal linePriceSnap;
    /** 商务调价后金额 */
    private BigDecimal linePriceAdjusted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getEstimatedDays() { return estimatedDays; }
    public void setEstimatedDays(BigDecimal estimatedDays) { this.estimatedDays = estimatedDays; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getExcludedFromScale() { return excludedFromScale; }
    public void setExcludedFromScale(Boolean excludedFromScale) { this.excludedFromScale = excludedFromScale; }
    public BigDecimal getLinePriceSnap() { return linePriceSnap; }
    public void setLinePriceSnap(BigDecimal linePriceSnap) { this.linePriceSnap = linePriceSnap; }
    public BigDecimal getLinePriceAdjusted() { return linePriceAdjusted; }
    public void setLinePriceAdjusted(BigDecimal linePriceAdjusted) { this.linePriceAdjusted = linePriceAdjusted; }
}
