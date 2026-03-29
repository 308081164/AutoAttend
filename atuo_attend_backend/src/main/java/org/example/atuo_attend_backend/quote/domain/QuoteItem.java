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
}
