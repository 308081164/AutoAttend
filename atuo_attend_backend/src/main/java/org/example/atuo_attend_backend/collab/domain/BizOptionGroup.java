package org.example.atuo_attend_backend.collab.domain;

import java.time.LocalDateTime;
import java.util.List;

public class BizOptionGroup {
    private Long id;
    private String name;
    private String options;
    private String scope;
    private Long projectId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
