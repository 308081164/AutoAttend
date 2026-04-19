package org.example.atuo_attend_backend.tenant.dto;

import java.time.LocalDateTime;

/** 推荐分成流水（用于邀请方「积分贡献」列表） */
public class ReferralCommissionRow {
    private Long id;
    private Long sourceTenantId;
    private String sourceTenantName;
    private Integer orderAmountCents;
    private Integer commissionCents;
    private Long subscriptionOrderId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceTenantId() { return sourceTenantId; }
    public void setSourceTenantId(Long sourceTenantId) { this.sourceTenantId = sourceTenantId; }
    public String getSourceTenantName() { return sourceTenantName; }
    public void setSourceTenantName(String sourceTenantName) { this.sourceTenantName = sourceTenantName; }
    public Integer getOrderAmountCents() { return orderAmountCents; }
    public void setOrderAmountCents(Integer orderAmountCents) { this.orderAmountCents = orderAmountCents; }
    public Integer getCommissionCents() { return commissionCents; }
    public void setCommissionCents(Integer commissionCents) { this.commissionCents = commissionCents; }
    public Long getSubscriptionOrderId() { return subscriptionOrderId; }
    public void setSubscriptionOrderId(Long subscriptionOrderId) { this.subscriptionOrderId = subscriptionOrderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
