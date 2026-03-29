package org.example.atuo_attend_backend.admin.dto;

public class AdminRegisterRequest {

    /** 原始输入，后端规范为 E.164 */
    private String phone;
    private String password;
    /** 组织名称：公司名或个体名 */
    private String orgName;
    /** 租户 slug，小写字母数字与连字符 */
    private String slug;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
