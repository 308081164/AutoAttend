package org.example.atuo_attend_backend.admin.dto;

/**
 * 发送短信验证码：purpose 为 login 或 register。
 */
public class AdminSmsSendRequest {

    private String phone;
    /** login | register */
    private String purpose;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
