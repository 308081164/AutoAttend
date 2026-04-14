package org.example.atuo_attend_backend.tenant.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Tenant {

    private Long id;
    private String name;
    private String slug;
    /** 推荐方租户 ID（被邀请注册或兑换时写入） */
    private Long referrerTenantId;
    /** 是否已使用邀请码兑换过尝鲜权益（每租户一次） */
    private Boolean inviteCodeRedeemed;
    /** 会员积分（1 积分可抵 1 元模拟购买） */
    private Integer memberPoints;
    /** 是否已使用过尝鲜版首月优惠价 */
    private Boolean teamFirstMonthUsed;
    /** 套餐档位：free / team / pro（当前权益档位；付费窗口内为已购档位） */
    private String planCode;
    /** 无付费/试用窗口时的回退档位（通常为 free，或由升级链决定） */
    private String billingBaselinePlanCode;
    /** 当前付费或模拟付费权益截止时间；过期后回退到 billingBaselinePlanCode */
    private LocalDateTime subscriptionEndsAt;
    /** active：正常；suspended：暂停（后续可在 Filter 中拦截） */
    private String status;
    /** 官方 API 池余额（元），注册赠送与兑换码增加，调用扣减 */
    private BigDecimal officialApiCnyBalance;
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

    public Long getReferrerTenantId() {
        return referrerTenantId;
    }

    public void setReferrerTenantId(Long referrerTenantId) {
        this.referrerTenantId = referrerTenantId;
    }

    public Boolean getInviteCodeRedeemed() {
        return inviteCodeRedeemed;
    }

    public void setInviteCodeRedeemed(Boolean inviteCodeRedeemed) {
        this.inviteCodeRedeemed = inviteCodeRedeemed;
    }

    public Integer getMemberPoints() {
        return memberPoints;
    }

    public void setMemberPoints(Integer memberPoints) {
        this.memberPoints = memberPoints;
    }

    public Boolean getTeamFirstMonthUsed() {
        return teamFirstMonthUsed;
    }

    public void setTeamFirstMonthUsed(Boolean teamFirstMonthUsed) {
        this.teamFirstMonthUsed = teamFirstMonthUsed;
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

    public BigDecimal getOfficialApiCnyBalance() {
        return officialApiCnyBalance;
    }

    public void setOfficialApiCnyBalance(BigDecimal officialApiCnyBalance) {
        this.officialApiCnyBalance = officialApiCnyBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
