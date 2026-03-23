package org.example.atuo_attend_backend.collab.dto;

/**
 * 记录筛选规则：仅用于前端组合筛选（多条规则 AND）。
 *
 * op 枚举（本版本）：
 * - eq / ne
 * - contains / not_contains
 * - empty / not_empty
 */
public class CollabRecordFilterRule {
    private Long columnId;
    private String op;
    private String value;

    public Long getColumnId() {
        return columnId;
    }

    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

