package org.example.atuo_attend_backend.collab.dto;

/**
 * 协作端发送短信：purpose 为 login（成员已绑手机）或 bind_phone（绑定手机）。
 */
public class CollabSmsSendRequest {
    private String phone;
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
