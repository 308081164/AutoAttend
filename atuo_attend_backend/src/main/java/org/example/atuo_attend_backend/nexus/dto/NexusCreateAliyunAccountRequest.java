package org.example.atuo_attend_backend.nexus.dto;

public class NexusCreateAliyunAccountRequest {
    private String displayName;
    private String regionId;
    private String accessKeyId;
    private String accessKeySecret;
    private Integer autoSyncIntervalSeconds;

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

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public Integer getAutoSyncIntervalSeconds() {
        return autoSyncIntervalSeconds;
    }

    public void setAutoSyncIntervalSeconds(Integer autoSyncIntervalSeconds) {
        this.autoSyncIntervalSeconds = autoSyncIntervalSeconds;
    }
}

