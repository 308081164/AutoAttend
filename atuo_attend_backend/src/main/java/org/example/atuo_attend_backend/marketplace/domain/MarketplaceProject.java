package org.example.atuo_attend_backend.marketplace.domain;

import java.time.LocalDateTime;

public class MarketplaceProject {

    private Long id;
    private Long tenantId;
    private Long publisherUserId;
    private String title;
    private String description;
    /** JSON array：MinIO key，需求配图 */
    private String requirementImagesJson;
    /** JSON array string */
    private String techStackJson;
    private String budgetRange;
    private String duration;
    private String location;
    private String contactType;
    private String contactValueEnc;
    /** 联系方式二维码/配图 MinIO key */
    private String contactAttachmentStorageKey;
    private String status;
    private String rejectReason;
    private Integer viewCount;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    /** 长期有效：上架后 expire_time 为空 */
    private Boolean effectiveNeverExpires;
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

    public Long getPublisherUserId() {
        return publisherUserId;
    }

    public void setPublisherUserId(Long publisherUserId) {
        this.publisherUserId = publisherUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirementImagesJson() {
        return requirementImagesJson;
    }

    public void setRequirementImagesJson(String requirementImagesJson) {
        this.requirementImagesJson = requirementImagesJson;
    }

    public String getTechStackJson() {
        return techStackJson;
    }

    public void setTechStackJson(String techStackJson) {
        this.techStackJson = techStackJson;
    }

    public String getBudgetRange() {
        return budgetRange;
    }

    public void setBudgetRange(String budgetRange) {
        this.budgetRange = budgetRange;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContactType() {
        return contactType;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    public String getContactValueEnc() {
        return contactValueEnc;
    }

    public void setContactValueEnc(String contactValueEnc) {
        this.contactValueEnc = contactValueEnc;
    }

    public String getContactAttachmentStorageKey() {
        return contactAttachmentStorageKey;
    }

    public void setContactAttachmentStorageKey(String contactAttachmentStorageKey) {
        this.contactAttachmentStorageKey = contactAttachmentStorageKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public Boolean getEffectiveNeverExpires() {
        return effectiveNeverExpires;
    }

    public void setEffectiveNeverExpires(Boolean effectiveNeverExpires) {
        this.effectiveNeverExpires = effectiveNeverExpires;
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
