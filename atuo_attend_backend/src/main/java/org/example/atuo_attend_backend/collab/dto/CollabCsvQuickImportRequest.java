package org.example.atuo_attend_backend.collab.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 快捷引导导入：基于会话中的 CSV 数据，按用户配置映射到多维表字段并落库。
 */
public class CollabCsvQuickImportRequest {

    private String sessionId;
    private List<MappingRule> mappings = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<MappingRule> getMappings() {
        return mappings;
    }

    public void setMappings(List<MappingRule> mappings) {
        this.mappings = mappings;
    }

    public static class MappingRule {
        private Long targetColumnId;
        private List<String> sourceHeaders = new ArrayList<>();
        private String joinWith;

        public Long getTargetColumnId() {
            return targetColumnId;
        }

        public void setTargetColumnId(Long targetColumnId) {
            this.targetColumnId = targetColumnId;
        }

        public List<String> getSourceHeaders() {
            return sourceHeaders;
        }

        public void setSourceHeaders(List<String> sourceHeaders) {
            this.sourceHeaders = sourceHeaders;
        }

        public String getJoinWith() {
            return joinWith;
        }

        public void setJoinWith(String joinWith) {
            this.joinWith = joinWith;
        }
    }
}
