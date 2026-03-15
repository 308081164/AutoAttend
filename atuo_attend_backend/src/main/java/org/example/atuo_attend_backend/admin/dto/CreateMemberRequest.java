package org.example.atuo_attend_backend.admin.dto;

public class CreateMemberRequest {
    private String email;
    private String name;
    private String password;
    private String remarkName;
    private String jobTitle;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRemarkName() { return remarkName; }
    public void setRemarkName(String remarkName) { this.remarkName = remarkName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
}
