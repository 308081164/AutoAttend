package org.example.atuo_attend_backend.admin.dto;

public class MemberProjectItem {
    private Long projectId;
    private String role; // member | admin

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
