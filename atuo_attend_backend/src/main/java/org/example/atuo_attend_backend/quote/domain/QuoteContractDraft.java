package org.example.atuo_attend_backend.quote.domain;

import java.time.LocalDateTime;

public class QuoteContractDraft {
    private Long id;
    private Long tenantId;
    private Long quoteResultId;
    private String clientName;
    private String companyName;
    private String templateType;
    private String aiPromptSnapshot;
    private String aiRawResponse;
    private String editedContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getQuoteResultId() { return quoteResultId; }
    public void setQuoteResultId(Long quoteResultId) { this.quoteResultId = quoteResultId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getAiPromptSnapshot() { return aiPromptSnapshot; }
    public void setAiPromptSnapshot(String aiPromptSnapshot) { this.aiPromptSnapshot = aiPromptSnapshot; }
    public String getAiRawResponse() { return aiRawResponse; }
    public void setAiRawResponse(String aiRawResponse) { this.aiRawResponse = aiRawResponse; }
    public String getEditedContent() { return editedContent; }
    public void setEditedContent(String editedContent) { this.editedContent = editedContent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
