package org.example.atuo_attend_backend.report.domain;

import java.time.LocalDateTime;

public class ProjectReportConfig {
    private Long id;
    private Long projectId;
    private String repoFullName;
    private Boolean enabled;
    private Boolean sendToManagers;
    private Boolean sendToDevelopers;
    /** JSON array string */
    private String managerEmails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getRepoFullName() {
        return repoFullName;
    }

    public void setRepoFullName(String repoFullName) {
        this.repoFullName = repoFullName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getSendToManagers() {
        return sendToManagers;
    }

    public void setSendToManagers(Boolean sendToManagers) {
        this.sendToManagers = sendToManagers;
    }

    public Boolean getSendToDevelopers() {
        return sendToDevelopers;
    }

    public void setSendToDevelopers(Boolean sendToDevelopers) {
        this.sendToDevelopers = sendToDevelopers;
    }

    public String getManagerEmails() {
        return managerEmails;
    }

    public void setManagerEmails(String managerEmails) {
        this.managerEmails = managerEmails;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

