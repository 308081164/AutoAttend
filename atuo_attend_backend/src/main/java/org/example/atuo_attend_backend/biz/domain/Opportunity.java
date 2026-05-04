package org.example.atuo_attend_backend.biz.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商机表
 */
public class Opportunity {
    private Long id;
    private Long tenantId;
    private Long customerId;
    private Long quoteProjectId;
    private String name;
    /** 预计金额 */
    private BigDecimal expectedAmount;
    /** 阶段：discovery/qualification/proposal/negotiation/closed_won/closed_lost */
    private String stage;
    /** 赢单概率（百分比 0-100） */
    private Integer winProbability;
    /** 预计成交日期 */
    private LocalDateTime expectedCloseDate;
    /** 负责员工ID */
    private Long assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getQuoteProjectId() { return quoteProjectId; }
    public void setQuoteProjectId(Long quoteProjectId) { this.quoteProjectId = quoteProjectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public Integer getWinProbability() { return winProbability; }
    public void setWinProbability(Integer winProbability) { this.winProbability = winProbability; }
    public LocalDateTime getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(LocalDateTime expectedCloseDate) { this.expectedCloseDate = expectedCloseDate; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
