package org.example.atuo_attend_backend.tenant.domain;

import java.time.LocalDateTime;

public class Tenant {

    private Long id;
    private String name;
    private String slug;
    /** 套餐档位：free / team / pro（当前权益档位；付费窗口内为已购档位） */
    private String planCode;
    /** 无付费/试用窗口时的回退档位（通常为 free，或由升级链决定） */
    private String billingBaselinePlanCode;
    /** 当前付费或模拟付费权益截止时间；过期后回退到 billingBaselinePlanCode */
    private LocalDateTime subscriptionEndsAt;
    /** active：正常；suspended：暂停（后续可在 Filter 中拦截） */
    private String status;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getBillingBaselinePlanCode() {
        return billingBaselinePlanCode;
    }

    public void setBillingBaselinePlanCode(String billingBaselinePlanCode) {
        this.billingBaselinePlanCode = billingBaselinePlanCode;
    }

    public LocalDateTime getSubscriptionEndsAt() {
        return subscriptionEndsAt;
    }

    public void setSubscriptionEndsAt(LocalDateTime subscriptionEndsAt) {
        this.subscriptionEndsAt = subscriptionEndsAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
