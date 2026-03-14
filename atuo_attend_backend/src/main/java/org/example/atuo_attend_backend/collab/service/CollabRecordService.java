package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.*;
import org.example.atuo_attend_backend.collab.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CollabRecordService(BizRecordMapper recordMapper,
                               BizRecordFieldMapper fieldMapper,
                               BizProjectTableMapper tableMapper,
                               BizTableColumnMapper columnMapper,
                               BizRecordCommentMapper commentMapper,
                               BizAttachmentMapper attachmentMapper,
                               BizProjectMapper projectMapper) {
        this.recordMapper = recordMapper;
        this.fieldMapper = fieldMapper;
        this.tableMapper = tableMapper;
        this.columnMapper = columnMapper;
        this.commentMapper = commentMapper;
        this.attachmentMapper = attachmentMapper;
        this.projectMapper = projectMapper;
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

    @Transactional(rollbackFor = Exception.class)
    public BizRecord createRecord(long tableId, Long createdBy, Map<String, Object> fieldValues) {
        BizRecord record = new BizRecord();
        record.setTableId(tableId);
        record.setCreatedBy(createdBy);
        recordMapper.insert(record);

        List<BizTableColumn> columns = columnMapper.listByTableId(tableId);
        for (BizTableColumn col : columns) {
            Object val = fieldValues == null ? null : fieldValues.get("c" + col.getId());
            if (val == null) continue;
            BizRecordField rf = new BizRecordField();
            rf.setRecordId(record.getId());
            rf.setColumnId(col.getId());
            setFieldValue(rf, col.getColumnType(), val);
            fieldMapper.insert(rf);
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
        BizRecord rec = recordMapper.findById(recordId);
        if (rec == null) return;
        List<BizTableColumn> columns = columnMapper.listByTableId(rec.getTableId());
        for (BizTableColumn col : columns) {
            Object val = fieldValues == null ? null : fieldValues.get("c" + col.getId());
            BizRecordField existing = fieldMapper.findByRecordAndColumn(recordId, col.getId());
            if (val != null) {
                if (existing != null) {
                    setFieldValue(existing, col.getColumnType(), val);
                    fieldMapper.update(existing);
                } else {
                    BizRecordField rf = new BizRecordField();
                    rf.setRecordId(recordId);
                    rf.setColumnId(col.getId());
                    setFieldValue(rf, col.getColumnType(), val);
                    fieldMapper.insert(rf);
                }
            }
        }
    }

    private void setFieldValue(BizRecordField rf, String columnType, Object val) {
        if (val == null) return;
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
        fieldMapper.deleteByRecordId(recordId);
        recordMapper.deleteById(recordId);
    }
}
