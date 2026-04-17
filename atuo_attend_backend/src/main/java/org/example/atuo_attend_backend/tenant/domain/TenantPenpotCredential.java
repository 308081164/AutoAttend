package org.example.atuo_attend_backend.tenant.domain;

import java.time.LocalDateTime;

/**
 * 租户专属 Penpot 账号凭证（邮箱与密码、Access Token 均以密文存储）。
 */
public class TenantPenpotCredential {

    private Long tenantId;
    private String penpotEmail;
    private String passwordEnc;
    private String accessTokenEnc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getPenpotEmail() {
        return penpotEmail;
    }

    public void setPenpotEmail(String penpotEmail) {
        this.penpotEmail = penpotEmail;
    }

    public String getPasswordEnc() {
        return passwordEnc;
    }

    public void setPasswordEnc(String passwordEnc) {
        this.passwordEnc = passwordEnc;
    }

    public String getAccessTokenEnc() {
        return accessTokenEnc;
    }

    public void setAccessTokenEnc(String accessTokenEnc) {
        this.accessTokenEnc = accessTokenEnc;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
