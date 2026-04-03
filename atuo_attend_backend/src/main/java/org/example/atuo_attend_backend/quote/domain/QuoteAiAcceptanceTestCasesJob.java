package org.example.atuo_attend_backend.quote.domain;

import java.time.LocalDateTime;

public class QuoteAiAcceptanceTestCasesJob {
    private Long id;
    private Long tenantId;
    private Long quoteProjectId;
    private String status;
    private String requestSnapshot;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getQuoteProjectId() { return quoteProjectId; }
    public void setQuoteProjectId(Long quoteProjectId) { this.quoteProjectId = quoteProjectId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestSnapshot() { return requestSnapshot; }
    public void setRequestSnapshot(String requestSnapshot) { this.requestSnapshot = requestSnapshot; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

