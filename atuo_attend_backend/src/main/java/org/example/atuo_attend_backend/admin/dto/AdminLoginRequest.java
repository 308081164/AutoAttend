package org.example.atuo_attend_backend.admin.dto;

/**
 * 管理员登录：手机号（E.164 或国内 11 位）+ 密码。
 * {@code username} 为兼容旧客户端保留，若填写则与 {@code phone} 二选一（优先 phone）。
 */
public class AdminLoginRequest {

    private String phone;
    private String username;
    private String password;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /** 实际用于登录的原始账号串 */
    public String resolveLoginAccount() {
        if (phone != null && !phone.isBlank()) {
            return phone;
        }
        return username;
    }
}
