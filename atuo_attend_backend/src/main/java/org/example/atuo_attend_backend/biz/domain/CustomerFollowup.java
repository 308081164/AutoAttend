package org.example.atuo_attend_backend.biz.domain;

import java.time.LocalDateTime;

/**
 * 客户跟进记录表
 */
public class CustomerFollowup {
    private Long id;
    private Long tenantId;
    private Long customerId;
    /** 跟进方式：phone/wechat/visit/email/other */
    private String method;
    /** 跟进内容 */
    private String content;
    /** 跟进人ID */
    private Long operatorId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
