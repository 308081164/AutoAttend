package org.example.atuo_attend_backend.collab.domain;

import java.time.LocalDateTime;

public class BizProjectClientBoard {
    private Long id;
    private Long tenantId;
    private Long projectId;
    private Boolean enabled;
    private String publicToken;
    private Boolean showProgressDashboard;
    private Boolean showFeatureBacklog;
    private Boolean showAiTableEntry;
    private String aiPurpose;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
    public Boolean getShowProgressDashboard() { return showProgressDashboard; }
    public void setShowProgressDashboard(Boolean showProgressDashboard) { this.showProgressDashboard = showProgressDashboard; }
    public Boolean getShowFeatureBacklog() { return showFeatureBacklog; }
    public void setShowFeatureBacklog(Boolean showFeatureBacklog) { this.showFeatureBacklog = showFeatureBacklog; }
    public Boolean getShowAiTableEntry() { return showAiTableEntry; }
    public void setShowAiTableEntry(Boolean showAiTableEntry) { this.showAiTableEntry = showAiTableEntry; }
    public String getAiPurpose() { return aiPurpose; }
    public void setAiPurpose(String aiPurpose) { this.aiPurpose = aiPurpose; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
