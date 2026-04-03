package org.example.atuo_attend_backend.quote.dto;

public class QuoteAiContractGenerateJobStatus {
    private Long jobId;
    private String status;
    private String errorMessage;
    private String editedContent;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getEditedContent() { return editedContent; }
    public void setEditedContent(String editedContent) { this.editedContent = editedContent; }
}

