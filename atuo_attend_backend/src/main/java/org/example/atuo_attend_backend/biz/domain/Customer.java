package org.example.atuo_attend_backend.biz.domain;

import java.time.LocalDateTime;

/**
 * 客户表
 */
public class Customer {
    private Long id;
    private Long tenantId;
    private String name;
    private String company;
    private String phone;
    private String email;
    /** 来源渠道：xianyu/taobao/project_corner/self */
    private String source;
    /** 阶段：lead/intent/quoted/negotiating/won/lost */
    private String stage;
    /** 自定义标签 JSON */
    private String tags;
    /** 负责员工ID */
    private Long assignedTo;
    private LocalDateTime lastContactedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public LocalDateTime getLastContactedAt() { return lastContactedAt; }
    public void setLastContactedAt(LocalDateTime lastContactedAt) { this.lastContactedAt = lastContactedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
