package org.example.atuo_attend_backend.prototype.dto;

/**
 * Penpot 生成任务：需求正文（用于 LLM 规划）；note 可选作审计备注。
 */
public class UiPrototypePenpotGenerateRequest {
    /** 页面需求 / 设计说明，驱动布局规划；为空则使用 note 或默认占位 */
    private String prompt;
    private String note;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
