package org.example.atuo_attend_backend.collab.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.collab.ai.CollabAiTaskResponseParser;
import org.example.atuo_attend_backend.collab.controller.CollabAiTaskController;
import org.example.atuo_attend_backend.collab.dto.CollabCsvQuickImportRequest;
import org.example.atuo_attend_backend.collab.domain.BizProjectMember;
import org.example.atuo_attend_backend.collab.domain.BizTableColumn;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CSV 在<strong>本服务器</strong>解析并分片；每片单独 HTTP 请求调用一次 DeepSeek，避免 Nginx 504。
 */
@Service
public class CollabCsvAiImportService {

    private static final Logger log = LoggerFactory.getLogger(CollabCsvAiImportService.class);

    /** 配置项未加载时的默认单批行数（不含表头）；运行中实际值见 {@link #maxRowsPerChunk} */
    public static final int DEFAULT_MAX_ROWS_PER_CHUNK = 12;
    /** 允许的配置上限（单次 prompt 过大仍易触发网关超时） */
    public static final int HARD_CAP_ROWS_PER_CHUNK = 30;
    /** 单文件最大字节 */
    public static final int MAX_FILE_BYTES = 6 * 1024 * 1024;
    /** 最多处理的数据行 */
    public static final int MAX_TOTAL_DATA_ROWS = 3000;

    private final CollabTableService tableService;
    private final BizProjectMemberMapper projectMemberMapper;
    private final BizUserMapper userMapper;
    private final AiAnalysisConfigService aiConfigService;
    private final DeepSeekClient deepSeekClient;
    private final AiTokenUsageMapper tokenUsageMapper;
    private final CollabCsvAiImportSessionStore sessionStore;
    private final CollabRecordService recordService;

    @Value("${app.collab.csv-ai-import.max-rows-per-chunk:12}")
    private int configuredMaxRowsPerChunk;

    /** 钳位后的单批行数，由 {@link #initChunkConfig()} 赋值 */
    private int maxRowsPerChunk = DEFAULT_MAX_ROWS_PER_CHUNK;

    @PostConstruct
    void initChunkConfig() {
        int n = configuredMaxRowsPerChunk;
        if (n < 5) {
            n = 5;
        }
        if (n > HARD_CAP_ROWS_PER_CHUNK) {
            n = HARD_CAP_ROWS_PER_CHUNK;
        }
        this.maxRowsPerChunk = n;
        log.info("collab CSV AI import: max-rows-per-chunk={}", maxRowsPerChunk);
    }

    public CollabCsvAiImportService(CollabTableService tableService,
                                    BizProjectMemberMapper projectMemberMapper,
                                    BizUserMapper userMapper,
                                    AiAnalysisConfigService aiConfigService,
                                    DeepSeekClient deepSeekClient,
                                    AiTokenUsageMapper tokenUsageMapper,
                                    CollabCsvAiImportSessionStore sessionStore,
                                    CollabRecordService recordService) {
        this.tableService = tableService;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
        this.aiConfigService = aiConfigService;
        this.deepSeekClient = deepSeekClient;
        this.tokenUsageMapper = tokenUsageMapper;
        this.sessionStore = sessionStore;
        this.recordService = recordService;
    }

    /**
     * 上传 CSV，仅在服务端解析并缓存，不调用 AI。返回 sessionId 与总批次数。
     */
    public Map<String, Object> createImportSession(long userId, long projectId, MultipartFile file) throws Exception {
        validateFileBasics(file);
        Map<String, Object> schema = tableService.getTableWithColumns(projectId);
        if (schema == null) {
            throw new IllegalStateException("项目未绑定表格");
        }
        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在管理后台「AI 配置」中填写 DeepSeek API Key");
        }

        CsvParseResult parsed = parseCsv(file);
        if (parsed.header().length == 0) {
            throw new IllegalArgumentException("CSV 无表头");
        }
        if (parsed.dataRows().isEmpty()) {
            throw new IllegalArgumentException("CSV 无数据行");
        }
        if (parsed.dataRows().size() > MAX_TOTAL_DATA_ROWS) {
            throw new IllegalArgumentException("数据行过多（上限 " + MAX_TOTAL_DATA_ROWS + "），请拆分文件");
        }

        CollabCsvAiImportSessionStore.Session session = new CollabCsvAiImportSessionStore.Session();
        session.setUserId(userId);
        session.setProjectId(projectId);
        session.setHeader(parsed.header());
        session.setDataRows(parsed.dataRows());
        session.setSchemaText(buildSchemaDescription(schema));
        session.setMemberText(buildMemberLines(projectId));
        session.setSystemPrompt(buildSystemPrompt());
        session.setRowsPerChunk(maxRowsPerChunk);

        String sessionId = sessionStore.create(session);
        int chunksTotal = session.getTotalChunks();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("csvRowCount", parsed.dataRows().size());
        data.put("chunksTotal", chunksTotal);
        data.put("chunkSize", maxRowsPerChunk);
        return data;
    }

    /**
     * 处理第 chunkIndex 批（0 起），单次 DeepSeek 调用。
     */
    public Map<String, Object> processChunk(long userId, long projectId, String sessionId, int chunkIndex) throws Exception {
        CollabCsvAiImportSessionStore.Session session = sessionStore.get(sessionId);
        if (session == null) {
            throw new IllegalStateException("会话已过期或无效，请重新上传 CSV");
        }
        if (session.getUserId() != userId || session.getProjectId() != projectId) {
            throw new IllegalStateException("无权访问该导入会话");
        }

        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在管理后台「AI 配置」中填写 DeepSeek API Key");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";

        List<String[]> allRows = session.getDataRows();
        int totalChunks = session.getTotalChunks();
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("chunkIndex 超出范围（0～" + (totalChunks - 1) + "）");
        }

        int step = session.getRowsPerChunk() > 0 ? session.getRowsPerChunk() : maxRowsPerChunk;
        int from = chunkIndex * step;
        int to = Math.min(from + step, allRows.size());
        List<String[]> chunk = allRows.subList(from, to);
        String userMsg = buildChunkUserMessage(chunkIndex + 1, totalChunks, step, session.getHeader(), chunk);

        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", session.getSystemPrompt()),
                new DeepSeekClient.ChatMessage("user", session.getSchemaText() + "\n\n" + session.getMemberText() + "\n\n" + userMsg)
        );
        /* 单批输出 JSON 通常远小于 8k；略降上限可缩短模型生成时间与网关等待 */
        DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true, 4096);

        List<String> warnings = new ArrayList<>();
        List<CollabAiTaskController.AiTaskDraft> items = new ArrayList<>();

        if (result == null || result.getContent() == null || result.getContent().isBlank()) {
            warnings.add("第 " + (chunkIndex + 1) + "/" + totalChunks + " 批：AI 无返回");
        } else {
            if (result.getInputTokens() > 0 || result.getOutputTokens() > 0) {
                try {
                    tokenUsageMapper.insert(LocalDateTime.now(), "deepseek", model,
                            result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens(),
                            null, "collab_csv_chunk:" + chunkIndex);
                } catch (Exception e) {
                    log.warn("Record DeepSeek token usage failed: {}", e.getMessage());
                }
            }
            try {
                items = CollabAiTaskResponseParser.parseDrafts(result.getContent());
                if (items.isEmpty()) {
                    warnings.add("第 " + (chunkIndex + 1) + "/" + totalChunks + " 批：未解析出任务");
                }
            } catch (Exception e) {
                log.warn("CSV AI chunk parse failed: {}", e.getMessage());
                warnings.add("第 " + (chunkIndex + 1) + "/" + totalChunks + " 批：解析失败");
            }
        }

        boolean finished = (chunkIndex >= totalChunks - 1);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chunkIndex", chunkIndex);
        data.put("chunksTotal", totalChunks);
        data.put("items", items);
        data.put("finished", finished);
        if (!warnings.isEmpty()) {
            data.put("warnings", warnings);
        }
        Map<String, Object> usage = new LinkedHashMap<>();
        if (result != null) {
            usage.put("inputTokens", result.getInputTokens());
            usage.put("outputTokens", result.getOutputTokens());
            usage.put("model", model);
        }
        data.put("usage", usage);
        return data;
    }

    /**
     * 快捷引导导入：创建会话并返回表头 + 若干样例行。
     */
    public Map<String, Object> createQuickImportSession(long userId, long projectId, MultipartFile file) throws Exception {
        validateFileBasics(file);
        Map<String, Object> schema = tableService.getTableWithColumns(projectId);
        if (schema == null) {
            throw new IllegalStateException("项目未绑定表格");
        }
        CsvParseResult parsed = parseCsv(file);
        if (parsed.header().length == 0) {
            throw new IllegalArgumentException("CSV 无表头");
        }
        if (parsed.dataRows().isEmpty()) {
            throw new IllegalArgumentException("CSV 无数据行");
        }
        if (parsed.dataRows().size() > MAX_TOTAL_DATA_ROWS) {
            throw new IllegalArgumentException("数据行过多（上限 " + MAX_TOTAL_DATA_ROWS + "），请拆分文件");
        }

        CollabCsvAiImportSessionStore.Session session = new CollabCsvAiImportSessionStore.Session();
        session.setUserId(userId);
        session.setProjectId(projectId);
        session.setHeader(parsed.header());
        session.setDataRows(parsed.dataRows());
        String sessionId = sessionStore.create(session);

        List<Map<String, String>> sampleRows = new ArrayList<>();
        int sampleCount = Math.min(5, parsed.dataRows().size());
        for (int i = 0; i < sampleCount; i++) {
            sampleRows.add(toRowMap(parsed.header(), parsed.dataRows().get(i)));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("csvRowCount", parsed.dataRows().size());
        data.put("headers", Arrays.asList(parsed.header()));
        data.put("sampleRows", sampleRows);
        return data;
    }

    /**
     * 快捷引导导入：依据映射规则将会话中的 CSV 行写入多维表。
     */
    public Map<String, Object> commitQuickImport(long userId, long projectId, CollabCsvQuickImportRequest body) {
        if (body == null || body.getSessionId() == null || body.getSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId 缺失");
        }
        CollabCsvAiImportSessionStore.Session session = sessionStore.get(body.getSessionId());
        if (session == null) {
            throw new IllegalStateException("会话已过期或无效，请重新上传 CSV");
        }
        if (session.getUserId() != userId || session.getProjectId() != projectId) {
            throw new IllegalStateException("无权访问该导入会话");
        }
        List<CollabCsvQuickImportRequest.MappingRule> rules = body.getMappings() != null ? body.getMappings() : List.of();
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("请至少配置一条映射规则");
        }

        Map<String, Object> schema = tableService.getTableWithColumns(projectId);
        if (schema == null) {
            throw new IllegalStateException("项目未绑定表格");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) schema.get("columns");
        if (columns == null || columns.isEmpty()) {
            throw new IllegalStateException("表格列定义为空");
        }
        Map<Long, Map<String, Object>> colById = new HashMap<>();
        for (Map<String, Object> c : columns) {
            Object idObj = c.get("id");
            if (idObj == null) continue;
            long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            colById.put(id, c);
        }
        Map<String, Integer> headerIndex = buildHeaderIndex(session.getHeader());

        List<RuleSpec> specs = new ArrayList<>();
        for (CollabCsvQuickImportRequest.MappingRule r : rules) {
            if (r == null || r.getTargetColumnId() == null) continue;
            long colId = r.getTargetColumnId();
            Map<String, Object> col = colById.get(colId);
            if (col == null) {
                throw new IllegalArgumentException("目标列不存在: " + colId);
            }
            List<Integer> indexes = new ArrayList<>();
            if (r.getSourceHeaders() != null) {
                for (String h : r.getSourceHeaders()) {
                    if (h == null || h.isBlank()) continue;
                    Integer idx = headerIndex.get(h.trim());
                    if (idx != null) indexes.add(idx);
                }
            }
            if (indexes.isEmpty()) continue;
            String joinWith = r.getJoinWith() != null ? r.getJoinWith() : "\n";
            String columnType = String.valueOf(col.getOrDefault("columnType", "text"));
            specs.add(new RuleSpec(colId, columnType, indexes, joinWith));
        }
        if (specs.isEmpty()) {
            throw new IllegalArgumentException("映射规则无效：请选择来源 CSV 列");
        }

        var table = tableService.getTableByProjectId(projectId);
        if (table == null) {
            throw new IllegalStateException("项目未绑定表格");
        }

        int created = 0;
        int skipped = 0;
        List<String[]> rows = session.getDataRows() != null ? session.getDataRows() : List.of();
        for (String[] row : rows) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (RuleSpec spec : specs) {
                Object value = buildMappedValue(spec, row);
                if (value != null) {
                    fields.put("c" + spec.targetColumnId(), value);
                }
            }
            if (fields.isEmpty()) {
                skipped++;
                continue;
            }
            try {
                recordService.createRecord(table.getId(), userId, fields);
                created++;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException((e.getMessage() != null ? e.getMessage() : "导入失败") + "（已成功导入 " + created + " 条）");
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("createdCount", created);
        data.put("skippedCount", skipped);
        data.put("totalRows", rows.size());
        return data;
    }

    /** 导入完成或放弃时释放内存（也可依赖会话 TTL 自动清理） */
    public void discardSession(long userId, long projectId, String sessionId) {
        CollabCsvAiImportSessionStore.Session session = sessionStore.get(sessionId);
        if (session != null && session.getUserId() == userId && session.getProjectId() == projectId) {
            sessionStore.remove(sessionId);
        }
    }

    private void validateFileBasics(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 CSV 文件");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("文件过大，请控制在 " + (MAX_FILE_BYTES / 1024 / 1024) + "MB 以内");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!original.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("仅支持 .csv 文件");
        }
    }

    private CsvParseResult parseCsv(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        int start = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            start = 3;
        }
        String text = new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
        if (text.isBlank()) {
            return new CsvParseResult(new String[0], List.of());
        }
        try (StringReader sr = new StringReader(text);
             CSVParser parser = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).setTrim(true).build().parse(sr)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                return new CsvParseResult(new String[0], List.of());
            }
            CSVRecord headerRec = records.get(0);
            int width = headerRec.size();
            String[] header = new String[width];
            for (int i = 0; i < width; i++) {
                header[i] = safeCell(headerRec, i);
            }
            List<String[]> dataRows = new ArrayList<>();
            for (int r = 1; r < records.size(); r++) {
                CSVRecord row = records.get(r);
                if (isRowEmpty(row, width)) {
                    continue;
                }
                String[] cells = new String[width];
                for (int i = 0; i < width; i++) {
                    cells[i] = safeCell(row, i);
                }
                dataRows.add(cells);
            }
            return new CsvParseResult(header, dataRows);
        }
    }

    private static String safeCell(CSVRecord row, int i) {
        if (row == null || i < 0 || i >= row.size()) {
            return "";
        }
        String v = row.get(i);
        return v != null ? v.trim() : "";
    }

    private static boolean isRowEmpty(CSVRecord row, int width) {
        for (int i = 0; i < width && i < row.size(); i++) {
            String v = row.get(i);
            if (v != null && !v.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> buildHeaderIndex(String[] header) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (header == null) return m;
        for (int i = 0; i < header.length; i++) {
            String h = header[i] != null ? header[i].trim() : "";
            if (!h.isEmpty() && !m.containsKey(h)) {
                m.put(h, i);
            }
        }
        return m;
    }

    private static Map<String, String> toRowMap(String[] header, String[] row) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            String key = header[i];
            String val = (row != null && i < row.length && row[i] != null) ? row[i] : "";
            out.put(key, val);
        }
        return out;
    }

    private static Object buildMappedValue(RuleSpec spec, String[] row) {
        List<String> parts = new ArrayList<>();
        for (Integer idx : spec.sourceIndexes()) {
            if (idx == null || idx < 0) continue;
            String v = (row != null && idx < row.length && row[idx] != null) ? row[idx].trim() : "";
            if (!v.isEmpty()) parts.add(v);
        }
        if (parts.isEmpty()) return null;

        String columnType = spec.columnType();
        if ("multi_select".equals(columnType) || "multi_user".equals(columnType) || "attachment".equals(columnType)) {
            List<String> arr = new ArrayList<>();
            for (String p : parts) {
                String[] seg = p.split("[,，;；|、\\s]+");
                for (String s : seg) {
                    if (s != null && !s.isBlank()) arr.add(s.trim());
                }
            }
            return arr.isEmpty() ? null : arr;
        }
        if ("number".equals(columnType)) {
            String joined = String.join(spec.joinWith(), parts).trim();
            if (joined.isEmpty()) return null;
            try {
                return Double.parseDouble(joined);
            } catch (Exception e) {
                return null;
            }
        }
        return String.join(spec.joinWith(), parts).trim();
    }

    private String buildMemberLines(long projectId) {
        StringBuilder sb = new StringBuilder();
        sb.append("【项目负责人候选】owners 仅可填下列邮箱（小写匹配）；无法匹配则留空数组。\n");
        List<BizProjectMember> members = projectMemberMapper.listByProjectId(projectId);
        if (members == null || members.isEmpty()) {
            sb.append("（暂无项目成员）\n");
            return sb.toString();
        }
        for (BizProjectMember m : members) {
            if (m == null) continue;
            BizUser u = userMapper.findById(m.getUserId());
            if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
                sb.append("- ").append(u.getEmail().trim());
                if (u.getName() != null && !u.getName().isBlank()) {
                    sb.append(" （").append(u.getName().trim()).append("）");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildSchemaDescription(Map<String, Object> schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前项目任务表结构】请把 CSV 每行映射为一条任务，列名可能与下表不一致，请按语义对齐。\n");
        Object colsObj = schema.get("columns");
        if (colsObj instanceof List) {
            for (Object o : (List<?>) colsObj) {
                if (!(o instanceof Map)) continue;
                Map<String, Object> c = (Map<String, Object>) o;
                sb.append("- 列名: ").append(c.getOrDefault("name", ""))
                        .append("，类型: ").append(c.getOrDefault("columnType", ""));
                Object og = c.get("optionGroup");
                if (og instanceof Map) {
                    Map<?, ?> ogm = (Map<?, ?>) og;
                    Object opts = ogm.get("options");
                    if (opts instanceof List) {
                        List<?> optList = (List<?>) opts;
                        if (!optList.isEmpty()) {
                            appendOptionLabels(sb, optList);
                        }
                    }
                }
                sb.append("\n");
            }
        }
        sb.append("\n输出要求：仅输出一个 JSON 对象，形如 {\"items\":[...]} ，不要 Markdown。items 中每条字段：\n");
        sb.append("title, description, module, importantLevel, status, acceptResult, owners(邮箱数组), attachmentIds(恒为[])\n");
        sb.append("单选字段必须从对应列的可选值中选最近似的一项；无法判断时用中性值或略写进 description。\n");
        return sb.toString();
    }

    private static void appendOptionLabels(StringBuilder sb, List<?> optList) {
        List<String> parts = new ArrayList<>();
        for (Object x : optList) {
            if (x != null) {
                parts.add(String.valueOf(x));
            }
        }
        if (!parts.isEmpty()) {
            sb.append("，可选值: ").append(String.join("、", parts));
        }
    }

    private String buildSystemPrompt() {
        return "你是数据清洗与需求整理助手。用户上传的 CSV 列名、顺序可能与系统多维表不一致。"
                + "你将收到表结构说明、项目成员邮箱、以及一批 CSV 数据行（Markdown 表）。"
                + "请将本批每一行转换为一条任务对象：字段语义对齐到系统列（问题描述=title+description，归属模块=module，重要程度=importantLevel，当前状态=status，验收结果=acceptResult）。"
                + "attachmentIds 始终为 []。严格输出 JSON 对象 {\"items\":[...]} ，无其它文字。";
    }

    private String buildChunkUserMessage(int chunkIndex, int totalChunks, int rowsPerChunkHint, String[] header, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("【本批 CSV 数据】第 ").append(chunkIndex).append("/").append(totalChunks).append(" 批，共 ")
                .append(rows.size()).append(" 行（每批最多 ").append(rowsPerChunkHint).append(" 行）。\n");
        sb.append("CSV 表头: ");
        sb.append(String.join(" | ", header));
        sb.append("\n\n数据（Markdown 表，一行对应 CSV 一条数据）：\n");
        sb.append("| ");
        sb.append(String.join(" | ", header));
        sb.append(" |\n");
        sb.append("|");
        sb.append(" --- |".repeat(header.length));
        sb.append("\n");
        for (String[] row : rows) {
            sb.append("| ");
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < header.length; i++) {
                String cell = i < row.length ? row[i] : "";
                cells.add(escapeMarkdownCell(cell));
            }
            sb.append(String.join(" | ", cells));
            sb.append(" |\n");
        }
        sb.append("\n请为本批每一行生成一条 items 元素，顺序与行一致；不要合并行、不要丢行。");
        return sb.toString();
    }

    private static String escapeMarkdownCell(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }

    private record CsvParseResult(String[] header, List<String[]> dataRows) {
    }

    private record RuleSpec(long targetColumnId, String columnType, List<Integer> sourceIndexes, String joinWith) {
    }
}
