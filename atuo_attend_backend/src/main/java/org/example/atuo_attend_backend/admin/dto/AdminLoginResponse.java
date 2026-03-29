package org.example.atuo_attend_backend.admin.dto;

public class AdminLoginResponse {

    private String token;
    private long expiresIn;
    /** 协作模块 JWT，管理员登录时一并下发，访问项目协作无需再次登录 */
    private String collabToken;
    private Long tenantId;
    private String tenantName;
    private String slug;
    private String phone;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getCollabToken() {
        return collabToken;
    }

    public void setCollabToken(String collabToken) {
        this.collabToken = collabToken;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

