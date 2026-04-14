package org.example.atuo_attend_backend.collab.dto;

public class CollabLoginRequest {
    private String email;
    private String password;
    /** 可选：邮箱登录时限定租户，默认 1 */
    private Long tenantId;
    /** 成员已绑手机时：短信验证码登录 */
    private String smsCode;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getSmsCode() { return smsCode; }
    public void setSmsCode(String smsCode) { this.smsCode = smsCode; }
}
