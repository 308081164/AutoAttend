package org.example.atuo_attend_backend.admin.model;

public class AdminUser {

    private Long userId;
    private String username;
    private String role;
    private String phone;
    private Long tenantId;
    private String tenantName;
    private String slug;
    /** 是否可发布项目信息（外包信息发布模块） */
    private Boolean canPublishProjectInfo;

    public AdminUser() {
    }

    public AdminUser(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Boolean getCanPublishProjectInfo() {
        return canPublishProjectInfo;
    }

    public void setCanPublishProjectInfo(Boolean canPublishProjectInfo) {
        this.canPublishProjectInfo = canPublishProjectInfo;
    }
}

