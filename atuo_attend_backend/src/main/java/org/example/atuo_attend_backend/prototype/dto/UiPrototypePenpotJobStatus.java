package org.example.atuo_attend_backend.prototype.dto;

public class UiPrototypePenpotJobStatus {
    private Long jobId;
    private String status;
    private String errorMessage;
    private String penpotFileId;
    private String penpotPreviewUrl;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getPenpotFileId() { return penpotFileId; }
    public void setPenpotFileId(String penpotFileId) { this.penpotFileId = penpotFileId; }

    public String getPenpotPreviewUrl() { return penpotPreviewUrl; }
    public void setPenpotPreviewUrl(String penpotPreviewUrl) { this.penpotPreviewUrl = penpotPreviewUrl; }
}
