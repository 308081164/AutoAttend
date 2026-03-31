package org.example.atuo_attend_backend.prototype.domain;

import java.time.LocalDateTime;

public class UiPrototypeGenerateJob {
    private Long id;
    private Long tenantId;
    private Long projectId;
    private String status;
    private String errorMessage;
    private Integer specVersion;
    private String promptSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getSpecVersion() { return specVersion; }
    public void setSpecVersion(Integer specVersion) { this.specVersion = specVersion; }

    public String getPromptSnapshot() { return promptSnapshot; }
    public void setPromptSnapshot(String promptSnapshot) { this.promptSnapshot = promptSnapshot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
