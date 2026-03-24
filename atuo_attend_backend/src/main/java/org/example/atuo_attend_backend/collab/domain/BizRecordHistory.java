package org.example.atuo_attend_backend.collab.domain;

import java.time.LocalDateTime;

public class BizRecordHistory {
    private Long id;
    private Long projectId;
    private Long tableId;
    private Long recordId;
    private Long columnId;
    private String action;
    private String oldValue;
    private String newValue;
    private Long operatorUserId;
    private String operatorSystemRole;
    private String operatorProjectRole;
    private String source;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
    public String getOperatorSystemRole() { return operatorSystemRole; }
    public void setOperatorSystemRole(String operatorSystemRole) { this.operatorSystemRole = operatorSystemRole; }
    public String getOperatorProjectRole() { return operatorProjectRole; }
    public void setOperatorProjectRole(String operatorProjectRole) { this.operatorProjectRole = operatorProjectRole; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
