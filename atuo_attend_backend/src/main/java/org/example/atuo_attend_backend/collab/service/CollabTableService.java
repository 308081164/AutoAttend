package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.CollabTablePurpose;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.domain.BizTableColumn;
import org.example.atuo_attend_backend.collab.mapper.BizProjectTableMapper;
import org.example.atuo_attend_backend.collab.mapper.BizOptionGroupMapper;
import org.example.atuo_attend_backend.collab.mapper.BizTableColumnMapper;
import org.example.atuo_attend_backend.collab.domain.BizOptionGroup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollabTableService {

    private final BizProjectTableMapper tableMapper;
    private final BizTableColumnMapper columnMapper;
    private final BizOptionGroupMapper optionGroupMapper;

    public CollabTableService(BizProjectTableMapper tableMapper,
                              BizTableColumnMapper columnMapper,
                              BizOptionGroupMapper optionGroupMapper) {
        this.tableMapper = tableMapper;
        this.columnMapper = columnMapper;
        this.optionGroupMapper = optionGroupMapper;
    }

    public BizProjectTable getTableByProjectId(long projectId) {
        return tableMapper.findByProjectIdAndPurpose(projectId, CollabTablePurpose.ISSUE_TRACKING);
    }

    public BizProjectTable getTableByProjectIdAndPurpose(long projectId, String purpose) {
        if (purpose == null || purpose.isBlank()) {
            purpose = CollabTablePurpose.ISSUE_TRACKING;
        }
        return tableMapper.findByProjectIdAndPurpose(projectId, purpose);
    }

    public List<Map<String, Object>> listTableSummaries(long projectId) {
        List<BizProjectTable> tables = tableMapper.listByProjectId(projectId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizProjectTable t : tables) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("purpose", t.getPurpose());
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> getTableWithColumns(long projectId) {
        return getTableWithColumns(projectId, CollabTablePurpose.ISSUE_TRACKING);
    }

    public Map<String, Object> getTableWithColumns(long projectId, String purpose) {
        if (purpose == null || purpose.isBlank()) {
            purpose = CollabTablePurpose.ISSUE_TRACKING;
        }
        BizProjectTable table = tableMapper.findByProjectIdAndPurpose(projectId, purpose);
        if (table == null) return null;
        List<BizTableColumn> columns = columnMapper.listByTableId(table.getId());
        List<BizOptionGroup> optionGroups = optionGroupMapper.listByProject(projectId);

        Map<Long, BizOptionGroup> groupMap = new HashMap<>();
        for (BizOptionGroup g : optionGroups) {
            groupMap.put(g.getId(), g);
        }

        List<Map<String, Object>> columnList = new ArrayList<>();
        for (BizTableColumn c : columns) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("columnType", c.getColumnType());
            m.put("sortOrder", c.getSortOrder());
            if (c.getOptionGroupId() != null) {
                BizOptionGroup og = groupMap.get(c.getOptionGroupId());
                if (og != null) {
                    m.put("optionGroup", Map.of("id", og.getId(), "name", og.getName(), "options", parseOptionsJson(og.getOptions())));
                }
            }
            columnList.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", table.getId());
        result.put("projectId", projectId);
        result.put("name", table.getName());
        result.put("purpose", table.getPurpose());
        result.put("columns", columnList);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseOptionsJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
