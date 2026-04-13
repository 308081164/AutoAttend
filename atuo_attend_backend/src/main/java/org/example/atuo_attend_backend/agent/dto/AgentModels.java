package org.example.atuo_attend_backend.agent.dto;

import java.util.List;

/**
 * Agent 模块 DTO 集合
 */
public class AgentModels {

    /**
     * 创建会话请求
     */
    public static class CreateSessionRequest {
        private List<BackgroundTextItem> backgroundTexts;
        private List<Long> backgroundAttachmentIds;

        public List<BackgroundTextItem> getBackgroundTexts() {
            return backgroundTexts;
        }

        public void setBackgroundTexts(List<BackgroundTextItem> backgroundTexts) {
            this.backgroundTexts = backgroundTexts;
        }

        public List<Long> getBackgroundAttachmentIds() {
            return backgroundAttachmentIds;
        }

        public void setBackgroundAttachmentIds(List<Long> backgroundAttachmentIds) {
            this.backgroundAttachmentIds = backgroundAttachmentIds;
        }
    }

    /**
     * 背景文本条目
     */
    public static class BackgroundTextItem {
        private String label;
        private String content;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 发送消息请求
     */
    public static class SendMessageRequest {
        private String content;
        private List<Long> attachmentIds;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<Long> getAttachmentIds() {
            return attachmentIds;
        }

        public void setAttachmentIds(List<Long> attachmentIds) {
            this.attachmentIds = attachmentIds;
        }
    }

    /**
     * 确认描述完毕请求
     */
    public static class FinishRequest {
        private Boolean confirmed;

        public Boolean getConfirmed() {
            return confirmed;
        }

        public void setConfirmed(Boolean confirmed) {
            this.confirmed = confirmed;
        }
    }
}
