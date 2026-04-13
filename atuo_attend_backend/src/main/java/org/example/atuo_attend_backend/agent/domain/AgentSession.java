package org.example.atuo_attend_backend.agent.domain;

import java.time.LocalDateTime;

/**
 * Agent 会话实体
 */
public class AgentSession {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private String publicToken;
    private String status;
    private String projectContext;
    private String backgroundContext;
    private String backgroundSources;
    private String summaryText;
    private LocalDateTime endedAt;
    private String endedBy;
    private Integer totalMessages;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProjectContext() {
        return projectContext;
    }

    public void setProjectContext(String projectContext) {
        this.projectContext = projectContext;
    }

    public String getBackgroundContext() {
        return backgroundContext;
    }

    public void setBackgroundContext(String backgroundContext) {
        this.backgroundContext = backgroundContext;
    }

    public String getBackgroundSources() {
        return backgroundSources;
    }

    public void setBackgroundSources(String backgroundSources) {
        this.backgroundSources = backgroundSources;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public String getEndedBy() {
        return endedBy;
    }

    public void setEndedBy(String endedBy) {
        this.endedBy = endedBy;
    }

    public Integer getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(Integer totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Long getTotalInputTokens() {
        return totalInputTokens;
    }

    public void setTotalInputTokens(Long totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public Long getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public void setTotalOutputTokens(Long totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
