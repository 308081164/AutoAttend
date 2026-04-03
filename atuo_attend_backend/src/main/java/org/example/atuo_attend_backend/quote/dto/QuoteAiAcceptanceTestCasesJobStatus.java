package org.example.atuo_attend_backend.quote.dto;

import java.util.List;
import java.util.Map;

public class QuoteAiAcceptanceTestCasesJobStatus {
    private Long jobId;
    private String status;
    private String errorMessage;
    private List<Map<String, Object>> acceptanceTestCases;
    private Map<String, Object> usage;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<Map<String, Object>> getAcceptanceTestCases() { return acceptanceTestCases; }
    public void setAcceptanceTestCases(List<Map<String, Object>> acceptanceTestCases) { this.acceptanceTestCases = acceptanceTestCases; }

    public Map<String, Object> getUsage() { return usage; }
    public void setUsage(Map<String, Object> usage) { this.usage = usage; }
}

