package org.example.atuo_attend_backend.collab.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * 协作多维表 AI 录入：请求体与任务草稿模型（供协作接口与公开阅览看板复用）。
 */
public final class CollabAiTaskModels {

    private CollabAiTaskModels() {
    }

    public static class AiTaskPreviewRequest {
        private String rawText;
        private List<Long> attachmentIds;

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }

        public List<Long> getAttachmentIds() {
            return attachmentIds;
        }

        public void setAttachmentIds(List<Long> attachmentIds) {
            this.attachmentIds = attachmentIds;
        }
    }

    public static class AiTaskCommitRequest {
        private List<AiTaskDraft> tasks;

        public List<AiTaskDraft> getTasks() {
            return tasks;
        }

        public void setTasks(List<AiTaskDraft> tasks) {
            this.tasks = tasks;
        }
    }

    public static class AiTaskDraft {
        private String title;
        private String description;
        @JsonAlias("important_level")
        private String importantLevel;
        private String status;
        @JsonAlias("accept_result")
        private String acceptResult;
        private String module;
        private List<String> owners;
        @JsonAlias("attachment_ids")
        private List<Long> attachmentIds;

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

        public String getImportantLevel() {
            return importantLevel;
        }

        public void setImportantLevel(String importantLevel) {
            this.importantLevel = importantLevel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAcceptResult() {
            return acceptResult;
        }

        public void setAcceptResult(String acceptResult) {
            this.acceptResult = acceptResult;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public List<String> getOwners() {
            return owners;
        }

        public void setOwners(List<String> owners) {
            this.owners = owners;
        }

        public List<Long> getAttachmentIds() {
            return attachmentIds;
        }

        public void setAttachmentIds(List<Long> attachmentIds) {
            this.attachmentIds = attachmentIds;
        }
    }
}
