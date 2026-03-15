package org.example.atuo_attend_backend.admin.dto;

public class UpdateMemberRequest {
    private String name;
    private String remarkName;
    private String jobTitle;
    private String avatar;
    private String role;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemarkName() { return remarkName; }
    public void setRemarkName(String remarkName) { this.remarkName = remarkName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
