package org.example.atuo_attend_backend.collab.domain;

import java.time.LocalDateTime;

public class BizRecordField {
    private Long id;
    private Long recordId;
    private Long columnId;
    private String valueText;
    private Double valueNumber;
    private LocalDateTime valueDate;
    private String valueJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }
    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }
    public Double getValueNumber() { return valueNumber; }
    public void setValueNumber(Double valueNumber) { this.valueNumber = valueNumber; }
    public LocalDateTime getValueDate() { return valueDate; }
    public void setValueDate(LocalDateTime valueDate) { this.valueDate = valueDate; }
    public String getValueJson() { return valueJson; }
    public void setValueJson(String valueJson) { this.valueJson = valueJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
