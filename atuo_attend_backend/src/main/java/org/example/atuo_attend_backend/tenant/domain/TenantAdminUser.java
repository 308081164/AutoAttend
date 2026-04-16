package org.example.atuo_attend_backend.tenant.domain;

import java.time.LocalDateTime;

public class TenantAdminUser {

    private Long id;
    private Long tenantId;
    private String phone;
    private String passwordHash;
    /** 是否已完成至少一次短信验证码登录（与仅验证码登录流程对齐） */
    private Boolean smsLoginOnboarded;
    /** 是否可在租户内发布「项目信息」（外包信息发布模块） */
    private Boolean canPublishProjectInfo;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getSmsLoginOnboarded() {
        return smsLoginOnboarded;
    }

    public void setSmsLoginOnboarded(Boolean smsLoginOnboarded) {
        this.smsLoginOnboarded = smsLoginOnboarded;
    }

    public Boolean getCanPublishProjectInfo() {
        return canPublishProjectInfo;
    }

    public void setCanPublishProjectInfo(Boolean canPublishProjectInfo) {
        this.canPublishProjectInfo = canPublishProjectInfo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
