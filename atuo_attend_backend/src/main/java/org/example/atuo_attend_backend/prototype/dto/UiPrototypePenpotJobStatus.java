package org.example.atuo_attend_backend.prototype.dto;

public class UiPrototypePenpotJobStatus {
    private Long jobId;
    private String status;
    private String stage;
    private Integer progress;
    private Integer retryCount;
    private String errorMessage;
    private String penpotFileId;
    private String penpotPreviewUrl;
    private String exportBinfileUrl;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

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

    public String getPenpotFileId() { return penpotFileId; }
    public void setPenpotFileId(String penpotFileId) { this.penpotFileId = penpotFileId; }

    public String getPenpotPreviewUrl() { return penpotPreviewUrl; }
    public void setPenpotPreviewUrl(String penpotPreviewUrl) { this.penpotPreviewUrl = penpotPreviewUrl; }

    public String getExportBinfileUrl() { return exportBinfileUrl; }
    public void setExportBinfileUrl(String exportBinfileUrl) { this.exportBinfileUrl = exportBinfileUrl; }
}
