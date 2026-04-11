package org.example.atuo_attend_backend.nexus.dto;

public class NexusAccountSettingsRequest {
    private String displayName;
    /** null 表示沿用全局默认间隔 */
    private Integer autoSyncIntervalSeconds;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getAutoSyncIntervalSeconds() {
        return autoSyncIntervalSeconds;
    }

    public void setAutoSyncIntervalSeconds(Integer autoSyncIntervalSeconds) {
        this.autoSyncIntervalSeconds = autoSyncIntervalSeconds;
    }
}
