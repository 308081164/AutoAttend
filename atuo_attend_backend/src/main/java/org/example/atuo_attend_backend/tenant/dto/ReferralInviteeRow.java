package org.example.atuo_attend_backend.tenant.dto;

import java.time.LocalDateTime;

/** 被当前租户邀请注册的组织（联合创始人列表） */
public class ReferralInviteeRow {
    private Long tenantId;
    private String name;
    private String slug;
    private LocalDateTime createdAt;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
