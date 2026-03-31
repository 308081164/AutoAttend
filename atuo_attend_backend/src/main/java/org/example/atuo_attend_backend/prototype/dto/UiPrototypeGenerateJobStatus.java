package org.example.atuo_attend_backend.prototype.dto;

public class UiPrototypeGenerateJobStatus {
    private Long jobId;
    private String status;
    private Integer specVersion;
    private String errorMessage;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSpecVersion() { return specVersion; }
    public void setSpecVersion(Integer specVersion) { this.specVersion = specVersion; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
