package org.example.atuo_attend_backend.prototype.dto;

/**
 * Penpot 异步任务：可选备注（审计）；画布创建为占位，后续可接 LLM 写入。
 */
public class UiPrototypePenpotGenerateRequest {
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
