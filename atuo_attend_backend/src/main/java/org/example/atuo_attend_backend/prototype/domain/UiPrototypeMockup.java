package org.example.atuo_attend_backend.prototype.domain;

import java.time.LocalDateTime;

public class UiPrototypeMockup {
    private Long id;
    private Long tenantId;
    private Long projectId;
    private String html;
    private String css;
    private String rawAiContent;
    private String messagesJson;
    private String modelUsed;
    private boolean repaired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }

    public String getCss() { return css; }
    public void setCss(String css) { this.css = css; }

    public String getRawAiContent() { return rawAiContent; }
    public void setRawAiContent(String rawAiContent) { this.rawAiContent = rawAiContent; }

    public String getMessagesJson() { return messagesJson; }
    public void setMessagesJson(String messagesJson) { this.messagesJson = messagesJson; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public boolean isRepaired() { return repaired; }
    public void setRepaired(boolean repaired) { this.repaired = repaired; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

