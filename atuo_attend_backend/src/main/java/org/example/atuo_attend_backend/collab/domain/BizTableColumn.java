package org.example.atuo_attend_backend.collab.domain;

import java.time.LocalDateTime;

public class BizTableColumn {
    private Long id;
    private Long tableId;
    private String name;
    private String columnType;
    private Long optionGroupId;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColumnType() { return columnType; }
    public void setColumnType(String columnType) { this.columnType = columnType; }
    public Long getOptionGroupId() { return optionGroupId; }
    public void setOptionGroupId(Long optionGroupId) { this.optionGroupId = optionGroupId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
