package org.example.atuo_attend_backend.collab.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSV 解析结果暂存在本机内存，供分批次调用 DeepSeek（避免单次 HTTP 请求过长导致网关 504）。
 */
@Component
public class CollabCsvAiImportSessionStore {

    /** 会话存活时间（解析完成后前端会连续请求，给足余量） */
    private static final long TTL_MS = 25 * 60 * 1000L;

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public String create(Session session) {
        String id = UUID.randomUUID().toString().replace("-", "");
        long now = System.currentTimeMillis();
        session.setCreatedAtMs(now);
        session.setExpiresAtMs(now + TTL_MS);
        sessions.put(id, session);
        return id;
    }

    public Session get(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        Session s = sessions.get(sessionId);
        if (s == null) {
            return null;
        }
        if (System.currentTimeMillis() > s.getExpiresAtMs()) {
            sessions.remove(sessionId);
            return null;
        }
        return s;
    }

    public void remove(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void sweepExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> now > e.getValue().getExpiresAtMs());
    }

    public static final class Session {
        private long userId;
        private long projectId;
        private long createdAtMs;
        private long expiresAtMs;
        private String[] header;
        private List<String[]> dataRows;
        /** 已预计算，避免每片重复拼表结构 */
        private String schemaText;
        private String memberText;
        private String systemPrompt;

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public long getProjectId() {
            return projectId;
        }

        public void setProjectId(long projectId) {
            this.projectId = projectId;
        }

        public long getCreatedAtMs() {
            return createdAtMs;
        }

        public void setCreatedAtMs(long createdAtMs) {
            this.createdAtMs = createdAtMs;
        }

        public long getExpiresAtMs() {
            return expiresAtMs;
        }

        public void setExpiresAtMs(long expiresAtMs) {
            this.expiresAtMs = expiresAtMs;
        }

        public String[] getHeader() {
            return header;
        }

        public void setHeader(String[] header) {
            this.header = header;
        }

        public List<String[]> getDataRows() {
            return dataRows;
        }

        public void setDataRows(List<String[]> dataRows) {
            this.dataRows = dataRows;
        }

        public String getSchemaText() {
            return schemaText;
        }

        public void setSchemaText(String schemaText) {
            this.schemaText = schemaText;
        }

        public String getMemberText() {
            return memberText;
        }

        public void setMemberText(String memberText) {
            this.memberText = memberText;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public int getTotalChunks() {
            if (dataRows == null || dataRows.isEmpty()) {
                return 0;
            }
            return (int) Math.ceil((double) dataRows.size() / CollabCsvAiImportService.MAX_ROWS_PER_CHUNK);
        }
    }
}
