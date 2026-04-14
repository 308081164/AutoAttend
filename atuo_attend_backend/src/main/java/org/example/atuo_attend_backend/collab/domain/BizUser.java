package org.example.atuo_attend_backend.collab.domain;

import java.time.LocalDateTime;

public class BizUser {
    private Long id;
    private Long tenantId;
    private String email;
    private String name;
    private String passwordHash;
    private String role;
    private String avatar;
    private String remarkName;
    private String jobTitle;
    /** E.164 手机号；未填写表示未绑定手机 */
    private String phoneE164;
    /** 绑定的租户管理员账号（同手机号自动关联） */
    private Long linkedTenantAdminUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRemarkName() { return remarkName; }
    public void setRemarkName(String remarkName) { this.remarkName = remarkName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getPhoneE164() { return phoneE164; }
    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }
    public Long getLinkedTenantAdminUserId() { return linkedTenantAdminUserId; }
    public void setLinkedTenantAdminUserId(Long linkedTenantAdminUserId) { this.linkedTenantAdminUserId = linkedTenantAdminUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
