package org.example.atuo_attend_backend.admin.dto;

import java.util.List;

public class SetMemberProjectsRequest {
    private List<MemberProjectItem> projects;

    public List<MemberProjectItem> getProjects() { return projects; }
    public void setProjects(List<MemberProjectItem> projects) { this.projects = projects; }
}
