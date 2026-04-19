package org.example.atuo_attend_backend.lab.dto;

import java.time.LocalDateTime;

public class LabFeedbackListItem {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String content;
    private String imageKey;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
