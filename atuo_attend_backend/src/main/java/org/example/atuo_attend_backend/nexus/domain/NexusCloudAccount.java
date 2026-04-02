package org.example.atuo_attend_backend.nexus.domain;

import java.time.LocalDateTime;

public class NexusCloudAccount {
    private Long id;
    private Long tenantId;
    private String provider;
    private String displayName;
    private String regionId;
    private String accessKeyIdEnc;
    private String accessKeySecretEnc;
    private Integer autoSyncIntervalSeconds;
    private LocalDateTime lastAutoSyncAt;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getAccessKeyIdEnc() {
        return accessKeyIdEnc;
    }

    public void setAccessKeyIdEnc(String accessKeyIdEnc) {
        this.accessKeyIdEnc = accessKeyIdEnc;
    }

    public String getAccessKeySecretEnc() {
        return accessKeySecretEnc;
    }

    public void setAccessKeySecretEnc(String accessKeySecretEnc) {
        this.accessKeySecretEnc = accessKeySecretEnc;
    }

    public Integer getAutoSyncIntervalSeconds() {
        return autoSyncIntervalSeconds;
    }

    public void setAutoSyncIntervalSeconds(Integer autoSyncIntervalSeconds) {
        this.autoSyncIntervalSeconds = autoSyncIntervalSeconds;
    }

    public LocalDateTime getLastAutoSyncAt() {
        return lastAutoSyncAt;
    }

    public void setLastAutoSyncAt(LocalDateTime lastAutoSyncAt) {
        this.lastAutoSyncAt = lastAutoSyncAt;
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

