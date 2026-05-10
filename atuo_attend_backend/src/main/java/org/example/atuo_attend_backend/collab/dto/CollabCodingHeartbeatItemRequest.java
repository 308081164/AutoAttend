package org.example.atuo_attend_backend.collab.dto;

import java.time.Instant;

/**
 * 单条编码心跳片段（客户端聚合后的活跃秒数）。
 */
public class CollabCodingHeartbeatItemRequest {

    private String clientEventId;
    private long projectId;
    private Instant heartbeatAt;
    private int durationSeconds;
    private String language;
    private String fileFingerprint;

    public String getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(String clientEventId) {
        this.clientEventId = clientEventId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public Instant getHeartbeatAt() {
        return heartbeatAt;
    }

    public void setHeartbeatAt(Instant heartbeatAt) {
        this.heartbeatAt = heartbeatAt;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFileFingerprint() {
        return fileFingerprint;
    }

    public void setFileFingerprint(String fileFingerprint) {
        this.fileFingerprint = fileFingerprint;
    }
}
