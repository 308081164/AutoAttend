package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.*;
import org.example.atuo_attend_backend.collab.dto.CollabRecordFilterRule;
import org.example.atuo_attend_backend.collab.mapper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollabRecordService {

    private final BizRecordMapper recordMapper;
    private final BizRecordFieldMapper fieldMapper;
    private final BizProjectTableMapper tableMapper;
    private final BizTableColumnMapper columnMapper;
    private final BizRecordCommentMapper commentMapper;
    private final BizAttachmentMapper attachmentMapper;
    private final BizProjectMapper projectMapper;
    private final BizProjectMemberMapper projectMemberMapper;
    private final BizUserMapper userMapper;
    private final BizRecordHistoryMapper historyMapper;

    @Value("${app.collab.table.max-records:1500}")
    private int configuredMaxRecordsPerTable;

    /** 钳位后的单表记录上限 */
    private int maxRecordsPerTable = 1500;

    public CollabRecordService(BizRecordMapper recordMapper,
                               BizRecordFieldMapper fieldMapper,
                               BizProjectTableMapper tableMapper,
                               BizTableColumnMapper columnMapper,
                               BizRecordCommentMapper commentMapper,
                               BizAttachmentMapper attachmentMapper,
                               BizProjectMapper projectMapper,
                               BizProjectMemberMapper projectMemberMapper,
                               BizUserMapper userMapper,
                               BizRecordHistoryMapper historyMapper) {
        this.recordMapper = recordMapper;
        this.fieldMapper = fieldMapper;
        this.tableMapper = tableMapper;
        this.columnMapper = columnMapper;
        this.commentMapper = commentMapper;
        this.attachmentMapper = attachmentMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
        this.historyMapper = historyMapper;
    }

    @PostConstruct
    void initMaxRecordsPerTable() {
        int n = configuredMaxRecordsPerTable;
        if (n < 1) {
            n = 1;
        }
        if (n > 1_000_000) {
            n = 1_000_000;
        }
        this.maxRecordsPerTable = n;
    }

    public int getMaxRecordsPerTable() {
        return maxRecordsPerTable;
    }

    /**
     * 插入前校验：当前表记录数不得已达上限。
     */
    public void assertCanCreateRecord(long tableId) {
        long current = recordMapper.countByTableId(tableId);
        if (current >= maxRecordsPerTable) {
            throw new IllegalArgumentException(
                    "该表格记录已达上限（" + maxRecordsPerTable + " 条），请删除部分记录后再试");
        }
    }

    public long getProjectIdByRecordId(long recordId) {
        BizRecord r = recordMapper.findById(recordId);
        if (r == null) return -1;
        BizProjectTable t = tableMapper.findById(r.getTableId());
        return t != null ? t.getProjectId() : -1;
    }

    public List<Map<String, Object>> listRecords(long tableId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<BizRecord> records = recordMapper.listByTableId(tableId, offset, pageSize);
        List<BizTableColumn> columns = columnMapper.listByTableId(tableId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BizRecord rec : records) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", rec.getId());
            row.put("createdBy", rec.getCreatedBy());
            row.put("createdAt", rec.getCreatedAt());
            row.put("updatedAt", rec.getUpdatedAt());
            List<BizRecordField> fields = fieldMapper.listByRecordId(rec.getId());
            Map<Long, BizRecordField> fieldMap = fields.stream().collect(Collectors.toMap(BizRecordField::getColumnId, f -> f));
            for (BizTableColumn col : columns) {
                BizRecordField f = fieldMap.get(col.getId());
                Object val = f == null ? null : getFieldValue(f, col.getColumnType());
                row.put("c" + col.getId(), val);
                row.put("_column_" + col.getId(), col.getName());
            }
            rows.add(row);
        }
        return rows;
    }

    public List<Map<String, Object>> listRecordsFiltered(long tableId, int page, int pageSize, List<CollabRecordFilterRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return listRecords(tableId, page, pageSize);
        }
        List<CollabRecordFilterRule> validated = validateAndNormalizeRules(tableId, rules);

        int offset = (page - 1) * pageSize;
        List<BizRecord> records = recordMapper.listByTableIdWithFilters(tableId, offset, pageSize, validated);

        List<BizTableColumn> columns = columnMapper.listByTableId(tableId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BizRecord rec : records) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", rec.getId());
            row.put("createdBy", rec.getCreatedBy());
            row.put("createdAt", rec.getCreatedAt());
            row.put("updatedAt", rec.getUpdatedAt());
            List<BizRecordField> fields = fieldMapper.listByRecordId(rec.getId());
            Map<Long, BizRecordField> fieldMap = fields.stream().collect(Collectors.toMap(BizRecordField::getColumnId, f -> f));
            for (BizTableColumn col : columns) {
                BizRecordField f = fieldMap.get(col.getId());
                Object val = f == null ? null : getFieldValue(f, col.getColumnType());
                row.put("c" + col.getId(), val);
                row.put("_column_" + col.getId(), col.getName());
            }
            rows.add(row);
        }
        return rows;
    }

    public long countRecordsFiltered(long tableId, List<CollabRecordFilterRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return countRecords(tableId);
        }
        List<CollabRecordFilterRule> validated = validateAndNormalizeRules(tableId, rules);
        return recordMapper.countByTableIdWithFilters(tableId, validated);
    }

    public long countRecords(long tableId) {
        return recordMapper.countByTableId(tableId);
    }

    private Object getFieldValue(BizRecordField f, String columnType) {
        if (f.getValueText() != null) return f.getValueText();
        if (f.getValueNumber() != null) return f.getValueNumber();
        if (f.getValueDate() != null) return f.getValueDate().toString();
        if (f.getValueJson() != null) return f.getValueJson();
        return null;
    }

    private List<CollabRecordFilterRule> validateAndNormalizeRules(long tableId, List<CollabRecordFilterRule> rules) {
        List<BizTableColumn> columns = columnMapper.listByTableId(tableId);
        Map<Long, BizTableColumn> colMap = new HashMap<>();
        for (BizTableColumn c : columns) {
            colMap.put(c.getId(), c);
        }

        Set<String> allowedOps = Set.of("eq", "ne", "contains", "not_contains", "empty", "not_empty");
        Set<String> allowedColNames = Set.of("重要程度", "当前状态", "验收结果");

        List<CollabRecordFilterRule> out = new ArrayList<>();
        for (CollabRecordFilterRule r : rules) {
            if (r == null) continue;
            if (r.getColumnId() == null) throw new IllegalArgumentException("filters.columnId 缺失");
            if (r.getOp() == null || !allowedOps.contains(r.getOp())) throw new IllegalArgumentException("filters.op 非法");

            BizTableColumn col = colMap.get(r.getColumnId());
            if (col == null) throw new IllegalArgumentException("filters.columnId 不存在");
            if (!allowedColNames.contains(col.getName())) throw new IllegalArgumentException("本版本仅支持筛选重要程度/当前状态/验收结果");

            String op = r.getOp();
            if (!"empty".equals(op) && !"not_empty".equals(op)) {
                if (r.getValue() == null || r.getValue().isBlank()) throw new IllegalArgumentException("filters.value 缺失");
            }
            out.add(r);
        }
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public BizRecord createRecord(long tableId, Long createdBy, Map<String, Object> fieldValues) {
        return createRecordWithAudit(tableId, createdBy, fieldValues, createdBy, "api");
    }

    @Transactional(rollbackFor = Exception.class)
    public BizRecord createRecordWithAudit(long tableId, Long createdBy, Map<String, Object> fieldValues, Long operatorUserId, String source) {
        assertCanCreateRecord(tableId);
        BizRecord record = new BizRecord();
        record.setTableId(tableId);
        record.setCreatedBy(createdBy);
        recordMapper.insert(record);

        OperatorMeta operatorMeta = resolveOperatorMeta(tableId, operatorUserId);
        List<BizTableColumn> columns = columnMapper.listByTableId(tableId);
        for (BizTableColumn col : columns) {
            Object val = fieldValues == null ? null : fieldValues.get("c" + col.getId());
            if (val == null) continue;
            BizRecordField rf = new BizRecordField();
            rf.setRecordId(record.getId());
            rf.setColumnId(col.getId());
            setFieldValue(rf, col.getColumnType(), val);
            fieldMapper.insert(rf);
            insertHistory(tableId, record.getId(), col.getId(), "create", null, fieldValueToText(rf, col.getColumnType()), operatorMeta, source);
        }
        return record;
    }

    public BizRecord getRecord(long recordId) {
        return recordMapper.findById(recordId);
    }

    public Map<String, Object> getRecordDetail(long recordId) {
        BizRecord rec = recordMapper.findById(recordId);
        if (rec == null) return null;
        List<BizTableColumn> columns = columnMapper.listByTableId(rec.getTableId());
        List<BizRecordField> fields = fieldMapper.listByRecordId(recordId);
        Map<String, Object> out = new HashMap<>();
        out.put("id", rec.getId());
        out.put("tableId", rec.getTableId());
        out.put("createdBy", rec.getCreatedBy());
        out.put("createdAt", rec.getCreatedAt());
        out.put("updatedAt", rec.getUpdatedAt());
        Map<String, Object> values = new HashMap<>();
        Map<Long, BizRecordField> fieldMap = fields.stream().collect(Collectors.toMap(BizRecordField::getColumnId, f -> f));
        for (BizTableColumn col : columns) {
            BizRecordField f = fieldMap.get(col.getId());
            values.put(col.getName(), f == null ? null : getFieldValue(f, col.getColumnType()));
            values.put("_colId_" + col.getName(), col.getId());
        }
        out.put("fields", values);
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(long recordId, Map<String, Object> fieldValues) {
        updateRecordWithAudit(recordId, fieldValues, null, "api");
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRecordWithAudit(long recordId, Map<String, Object> fieldValues, Long operatorUserId, String source) {
        BizRecord rec = recordMapper.findById(recordId);
        if (rec == null) return;
        OperatorMeta operatorMeta = resolveOperatorMeta(rec.getTableId(), operatorUserId);
        List<BizTableColumn> columns = columnMapper.listByTableId(rec.getTableId());
        for (BizTableColumn col : columns) {
            Object val = fieldValues == null ? null : fieldValues.get("c" + col.getId());
            BizRecordField existing = fieldMapper.findByRecordAndColumn(recordId, col.getId());
            if (val != null) {
                if (existing != null) {
                    String before = fieldValueToText(existing, col.getColumnType());
                    setFieldValue(existing, col.getColumnType(), val);
                    fieldMapper.update(existing);
                    String after = fieldValueToText(existing, col.getColumnType());
                    if (!Objects.equals(before, after)) {
                        insertHistory(rec.getTableId(), recordId, col.getId(), "update", before, after, operatorMeta, source);
                    }
                } else {
                    BizRecordField rf = new BizRecordField();
                    rf.setRecordId(recordId);
                    rf.setColumnId(col.getId());
                    setFieldValue(rf, col.getColumnType(), val);
                    fieldMapper.insert(rf);
                    insertHistory(rec.getTableId(), recordId, col.getId(), "update", null, fieldValueToText(rf, col.getColumnType()), operatorMeta, source);
                }
            }
        }
    }

    private void setFieldValue(BizRecordField rf, String columnType, Object val) {
        if (val == null) return;
        // 只保留当前类型的值，避免 update 后 getFieldValue 仍读到旧列（如 valueText 覆盖 valueJson）
        rf.setValueText(null);
        rf.setValueNumber(null);
        rf.setValueDate(null);
        rf.setValueJson(null);
        if ("text".equals(columnType) || "single_select".equals(columnType)) {
            rf.setValueText(val.toString());
        } else if ("multi_user".equals(columnType) || "attachment".equals(columnType) || "multi_select".equals(columnType)) {
            try {
                rf.setValueJson(val instanceof String ? (String) val : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(val));
            } catch (Exception e) {
                rf.setValueText(val.toString());
            }
        } else if ("datetime".equals(columnType) || "date".equals(columnType)) {
            try {
                rf.setValueDate(LocalDateTime.parse(val.toString()));
            } catch (Exception e) {
                rf.setValueText(val.toString());
            }
        } else if ("number".equals(columnType)) {
            if (val instanceof Number) rf.setValueNumber(((Number) val).doubleValue());
            else try { rf.setValueNumber(Double.parseDouble(val.toString())); } catch (Exception ignored) {}
        } else {
            rf.setValueText(val.toString());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(long recordId) {
        deleteRecordWithAudit(recordId, null, "api");
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecordWithAudit(long recordId, Long operatorUserId, String source) {
        BizRecord rec = recordMapper.findById(recordId);
        if (rec == null) return;
        OperatorMeta operatorMeta = resolveOperatorMeta(rec.getTableId(), operatorUserId);
        List<BizTableColumn> columns = columnMapper.listByTableId(rec.getTableId());
        Map<Long, BizTableColumn> colMap = new HashMap<>();
        for (BizTableColumn c : columns) colMap.put(c.getId(), c);
        for (BizRecordField f : fieldMapper.listByRecordId(recordId)) {
            BizTableColumn col = colMap.get(f.getColumnId());
            String colType = col != null ? col.getColumnType() : "text";
            String oldVal = fieldValueToText(f, colType);
            if (oldVal != null && !oldVal.isBlank()) {
                insertHistory(rec.getTableId(), recordId, f.getColumnId(), "delete", oldVal, null, operatorMeta, source);
            }
        }
        fieldMapper.deleteByRecordId(recordId);
        recordMapper.deleteById(recordId);
    }

    public Map<String, Object> listRecordHistory(long recordId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> items = historyMapper.listByRecordId(recordId, offset, pageSize);
        long total = historyMapper.countByRecordId(recordId);
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        out.put("total", total);
        return out;
    }

    private void insertHistory(long tableId, long recordId, long columnId, String action,
                               String oldValue, String newValue, OperatorMeta operatorMeta, String source) {
        BizProjectTable table = tableMapper.findById(tableId);
        BizRecordHistory h = new BizRecordHistory();
        h.setProjectId(table != null ? table.getProjectId() : null);
        h.setTableId(tableId);
        h.setRecordId(recordId);
        h.setColumnId(columnId);
        h.setAction(action);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        if (operatorMeta != null) {
            h.setOperatorUserId(operatorMeta.userId());
            h.setOperatorSystemRole(operatorMeta.systemRole());
            h.setOperatorProjectRole(operatorMeta.projectRole());
        }
        h.setSource(source != null ? source : "api");
        historyMapper.insert(h);
    }

    private OperatorMeta resolveOperatorMeta(Long tableId, Long operatorUserId) {
        if (operatorUserId == null || tableId == null) return new OperatorMeta(null, null, null);
        BizUser u = userMapper.findById(operatorUserId);
        BizProjectTable table = tableMapper.findById(tableId);
        String projectRole = null;
        if (table != null) {
            BizProjectMember pm = projectMemberMapper.findByProjectAndUser(table.getProjectId(), operatorUserId);
            if (pm != null) projectRole = pm.getRole();
        }
        return new OperatorMeta(operatorUserId, u != null ? u.getRole() : null, projectRole);
    }

    private String fieldValueToText(BizRecordField f, String columnType) {
        Object val = getFieldValue(f, columnType);
        if (val == null) return null;
        if (val instanceof String) return (String) val;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(val);
        } catch (Exception e) {
            return String.valueOf(val);
        }
    }

    private record OperatorMeta(Long userId, String systemRole, String projectRole) {
    }
}
