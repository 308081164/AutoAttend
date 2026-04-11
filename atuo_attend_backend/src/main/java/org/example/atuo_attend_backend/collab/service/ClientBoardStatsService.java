package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.CollabTablePurpose;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 客户阅览看板：从多维表记录聚合仪表盘数据（不落库、不返回完整行数据）。
 */
@Service
public class ClientBoardStatsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DASHBOARD_WEEKS = 8;
    private static final int MAX_RECORDS_SCAN = 2500;
    private static final Pattern SPLIT_WORDS = Pattern.compile("[\\s\\n\\r\\t，。！？、；：\"\"''（）【】《》<>\\[\\]{}|\\\\/]+");

    private static final Set<String> ZH_STOP = Set.of(
            "的", "了", "和", "是", "在", "也", "有", "就", "不", "与", "及", "或", "等", "为", "以", "对", "中", "上", "到", "将", "从",
            "一个", "进行", "通过", "可以", "需要", "问题", "功能", "页面", "系统", "用户", "任务", "项目", "时间", "数据"
    );

    private final CollabTableService tableService;
    private final CollabRecordService recordService;

    public ClientBoardStatsService(CollabTableService tableService, CollabRecordService recordService) {
        this.tableService = tableService;
        this.recordService = recordService;
    }

    public Map<String, Object> buildIssueProgressStats(long projectId) {
        BizProjectTable table = tableService.getTableByProjectIdAndPurpose(projectId, CollabTablePurpose.ISSUE_TRACKING);
        if (table == null) {
            return Map.of("empty", true, "message", "未绑定项目调整表");
        }
        Map<String, Object> schema = tableService.getTableWithColumns(projectId, CollabTablePurpose.ISSUE_TRACKING);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = schema != null && schema.get("columns") instanceof List
                ? (List<Map<String, Object>>) schema.get("columns") : List.of();
        Map<String, Long> colIdByName = new HashMap<>();
        for (Map<String, Object> c : columns) {
            Object n = c.get("name");
            Object id = c.get("id");
            if (n != null && id != null) {
                colIdByName.put(String.valueOf(n).trim(), id instanceof Number ? ((Number) id).longValue() : Long.parseLong(id.toString()));
            }
        }

        long totalCount = recordService.countRecords(table.getId());
        int pageSize = (int) Math.min(MAX_RECORDS_SCAN, Math.max(totalCount, 1));
        List<Map<String, Object>> records = recordService.listRecords(table.getId(), 1, pageSize);

        int resolved = 0;
        for (Map<String, Object> row : records) {
            if (isResolvedIssue(row, colIdByName)) resolved++;
        }
        int unresolved = Math.max(records.size() - resolved, 0);

        List<String> weekLabels = buildWeekLabels(DASHBOARD_WEEKS);
        Map<String, Integer> createdByWeek = new LinkedHashMap<>();
        Map<String, Integer> resolvedByWeek = new LinkedHashMap<>();
        for (String w : weekLabels) {
            createdByWeek.put(w, 0);
            resolvedByWeek.put(w, 0);
        }
        for (Map<String, Object> row : records) {
            LocalDateTime ca = toLdt(row.get("createdAt"));
            if (ca != null) {
                String wk = weekLabel(weekStart(ca.toLocalDate()));
                if (createdByWeek.containsKey(wk)) {
                    createdByWeek.merge(wk, 1, Integer::sum);
                }
            }
            if (isResolvedIssue(row, colIdByName)) {
                LocalDateTime ua = toLdt(row.get("updatedAt"));
                if (ua != null) {
                    String wk = weekLabel(weekStart(ua.toLocalDate()));
                    if (resolvedByWeek.containsKey(wk)) {
                        resolvedByWeek.merge(wk, 1, Integer::sum);
                    }
                }
            }
        }

        Map<String, Integer> importance = new LinkedHashMap<>();
        Long impCol = colIdByName.get("重要程度");
        if (impCol != null) {
            for (Map<String, Object> row : records) {
                Object v = row.get("c" + impCol);
                String key = v == null || String.valueOf(v).isBlank() ? "未分类" : String.valueOf(v).trim();
                importance.merge(key, 1, Integer::sum);
            }
        }

        Map<String, double[]> avgResolve = buildAvgResolveByWeek(records, colIdByName, weekLabels);

        List<Map<String, Object>> wordCloud = buildSimpleWordCloud(records, columns, 40);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("empty", false);
        out.put("totalRecords", totalCount);
        out.put("resolved", resolved);
        out.put("unresolved", unresolved);
        out.put("weekLabels", weekLabels);
        out.put("weeklyCreated", weekLabels.stream().map(createdByWeek::get).collect(Collectors.toList()));
        out.put("weeklyResolved", weekLabels.stream().map(resolvedByWeek::get).collect(Collectors.toList()));
        List<String> impLabels = new ArrayList<>(importance.keySet());
        List<Integer> impCounts = new ArrayList<>();
        for (String k : impLabels) {
            impCounts.add(importance.get(k));
        }
        out.put("importanceLabels", impLabels);
        out.put("importanceCounts", impCounts);
        out.put("avgResolveLabels", weekLabels);
        out.put("avgResolveHours", Arrays.stream(avgResolve.get("avgs")).boxed().collect(Collectors.toList()));
        out.put("avgResolveCounts", Arrays.stream(avgResolve.get("counts")).mapToInt(d -> (int) d).boxed().collect(Collectors.toList()));
        out.put("wordCloud", wordCloud);
        return out;
    }

    private static Map<String, double[]> buildAvgResolveByWeek(List<Map<String, Object>> records,
                                                               Map<String, Long> colIdByName,
                                                               List<String> weekLabels) {
        Map<String, double[]> weekMap = new LinkedHashMap<>();
        for (String w : weekLabels) {
            weekMap.put(w, new double[]{0.0, 0.0});
        }
        for (Map<String, Object> row : records) {
            if (!isResolvedIssue(row, colIdByName)) continue;
            LocalDateTime end = toLdt(row.get("updatedAt"));
            LocalDateTime start = toLdt(row.get("createdAt"));
            if (end == null || start == null) continue;
            long ms = java.time.Duration.between(start, end).toMillis();
            if (ms <= 0 || ms > 366L * 24 * 3600 * 1000) continue;
            double hours = ms / 3600000.0;
            String wk = weekLabel(weekStart(end.toLocalDate()));
            double[] acc = weekMap.get(wk);
            if (acc != null) {
                acc[0] += hours;
                acc[1] += 1;
            }
        }
        int n = weekLabels.size();
        double[] avgs = new double[n];
        double[] counts = new double[n];
        for (int i = 0; i < n; i++) {
            String w = weekLabels.get(i);
            double[] acc = weekMap.get(w);
            counts[i] = acc[1];
            avgs[i] = acc[1] > 0 ? acc[0] / acc[1] : 0;
        }
        Map<String, double[]> out = new HashMap<>();
        out.put("avgs", avgs);
        out.put("counts", counts);
        return out;
    }

    public List<Map<String, Object>> buildFeatureBacklogSummary(long projectId, int limit) {
        BizProjectTable table = tableService.getTableByProjectIdAndPurpose(projectId, CollabTablePurpose.FEATURE_BACKLOG);
        if (table == null) return List.of();
        Map<String, Object> schema = tableService.getTableWithColumns(projectId, CollabTablePurpose.FEATURE_BACKLOG);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = schema != null && schema.get("columns") instanceof List
                ? (List<Map<String, Object>>) schema.get("columns") : List.of();
        Map<String, Long> colIdByName = new HashMap<>();
        for (Map<String, Object> c : columns) {
            Object n = c.get("name");
            Object id = c.get("id");
            if (n != null && id != null) {
                colIdByName.put(String.valueOf(n).trim(), id instanceof Number ? ((Number) id).longValue() : Long.parseLong(id.toString()));
            }
        }
        Long nameCol = colIdByName.get("功能名称");
        Long progCol = colIdByName.get("开发进度");
        if (nameCol == null && progCol == null) return List.of();

        int cap = Math.min(Math.max(limit, 1), 200);
        List<Map<String, Object>> records = recordService.listRecords(table.getId(), 1, cap);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : records) {
            Map<String, Object> line = new LinkedHashMap<>();
            if (nameCol != null) {
                Object v = row.get("c" + nameCol);
                line.put("featureName", v == null ? "" : String.valueOf(v).trim());
            }
            if (progCol != null) {
                Object v = row.get("c" + progCol);
                line.put("progress", v == null ? "" : String.valueOf(v).trim());
            }
            out.add(line);
        }
        return out;
    }

    private static boolean isResolvedIssue(Map<String, Object> row, Map<String, Long> colIdByName) {
        StringBuilder sb = new StringBuilder();
        for (String key : List.of("解决情况", "验收结果", "当前状态")) {
            Long id = colIdByName.get(key);
            if (id == null) continue;
            Object v = row.get("c" + id);
            if (v != null) sb.append(String.valueOf(v)).append(' ');
        }
        String text = sb.toString();
        String[] hitWords = {"已解决", "已完成", "通过任务关闭", "通过", "完成", "关闭", "已验收"};
        for (String w : hitWords) {
            if (text.contains(w)) return true;
        }
        return false;
    }

    private static LocalDate weekStart(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        int diff = dow == DayOfWeek.SUNDAY ? 6 : dow.getValue() - 1;
        return d.minusDays(diff);
    }

    private static String weekLabel(LocalDate d) {
        return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static List<String> buildWeekLabels(int weeks) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate anchor = weekStart(today);
        List<String> labels = new ArrayList<>();
        for (int i = weeks - 1; i >= 0; i--) {
            labels.add(weekLabel(anchor.minusWeeks(i)));
        }
        return labels;
    }

    private static LocalDateTime toLdt(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime) return (LocalDateTime) o;
        if (o instanceof java.sql.Timestamp) return ((java.sql.Timestamp) o).toLocalDateTime();
        try {
            return LocalDateTime.parse(o.toString().replace(' ', 'T').substring(0, Math.min(19, o.toString().length())));
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> buildSimpleWordCloud(List<Map<String, Object>> records,
                                                           List<Map<String, Object>> columns,
                                                           int maxWords) {
        List<Long> textColIds = new ArrayList<>();
        List<String> hints = List.of("问题", "标题", "描述", "备注", "详情", "内容");
        for (Map<String, Object> c : columns) {
            String name = String.valueOf(c.getOrDefault("name", "")).trim();
            String ct = String.valueOf(c.getOrDefault("columnType", "")).toLowerCase(Locale.ROOT);
            if ("attachment".equals(ct) || "multi_user".equals(ct)) continue;
            for (String h : hints) {
                if (name.contains(h)) {
                    Object id = c.get("id");
                    if (id != null) {
                        textColIds.add(id instanceof Number ? ((Number) id).longValue() : Long.parseLong(id.toString()));
                    }
                    break;
                }
            }
        }
        if (textColIds.isEmpty()) {
            for (Map<String, Object> c : columns) {
                String ct = String.valueOf(c.getOrDefault("columnType", "")).toLowerCase(Locale.ROOT);
                if ("text".equals(ct)) {
                    Object id = c.get("id");
                    if (id != null) {
                        textColIds.add(id instanceof Number ? ((Number) id).longValue() : Long.parseLong(id.toString()));
                    }
                }
            }
        }
        if (textColIds.isEmpty()) return List.of();

        StringBuilder blob = new StringBuilder();
        for (Map<String, Object> row : records) {
            for (Long cid : textColIds) {
                Object v = row.get("c" + cid);
                if (v != null) {
                    String s = String.valueOf(v).trim();
                    if (!s.isEmpty()) blob.append(s).append('\n');
                }
            }
        }
        Map<String, Integer> freq = new HashMap<>();
        for (String tok : SPLIT_WORDS.split(blob.toString())) {
            String t = tok.trim();
            if (t.length() < 2) continue;
            if (ZH_STOP.contains(t)) continue;
            freq.merge(t, 1, Integer::sum);
        }
        List<Map<String, Object>> items = freq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(maxWords)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("text", e.getKey());
                    m.put("weight", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        int max = items.isEmpty() ? 1 : (Integer) items.get(0).get("weight");
        for (Map<String, Object> m : items) {
            m.put("max", max);
        }
        return items;
    }
}
