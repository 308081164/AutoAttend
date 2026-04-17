package org.example.atuo_attend_backend.prototype.domain;

import java.time.LocalDateTime;

public class UiPrototypePenpotJob {
    private Long id;
    private Long tenantId;
    private Long projectId;
    private String status;
    /** planning | creating | writing | exporting | done */
    private String stage;
    private Integer progress;
    private Integer retryCount;
    private String errorMessage;
    private String promptSnapshot;
    private String penpotFileId;
    private String penpotPreviewUrl;
    private String exportBinfileUrl;
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

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getPromptSnapshot() { return promptSnapshot; }
    public void setPromptSnapshot(String promptSnapshot) { this.promptSnapshot = promptSnapshot; }

    public String getPenpotFileId() { return penpotFileId; }
    public void setPenpotFileId(String penpotFileId) { this.penpotFileId = penpotFileId; }

    public String getPenpotPreviewUrl() { return penpotPreviewUrl; }
    public void setPenpotPreviewUrl(String penpotPreviewUrl) { this.penpotPreviewUrl = penpotPreviewUrl; }

    public String getExportBinfileUrl() { return exportBinfileUrl; }
    public void setExportBinfileUrl(String exportBinfileUrl) { this.exportBinfileUrl = exportBinfileUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
