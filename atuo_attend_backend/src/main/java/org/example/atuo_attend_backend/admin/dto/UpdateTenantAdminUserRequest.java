package org.example.atuo_attend_backend.admin.dto;

/**
 * 至少提供一项；手机号与协作侧 biz_user.email（管理员影子账号）会同步。
 */
public class UpdateTenantAdminUserRequest {

    private String phone;
    private String newPassword;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
