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
import org.example.atuo_attend_backend.collab.domain.BizProjectMember;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CSV 分片（每片 ≤30 行数据）→ DeepSeek 清洗为与「AI 录入」一致的任务草稿 JSON，供前端预览后调用 /ai-tasks/commit 落库。
 */
@Service
public class CollabCsvAiImportService {

    private static final Logger log = LoggerFactory.getLogger(CollabCsvAiImportService.class);

    /** 单批送入模型的最大数据行数（不含表头） */
    public static final int MAX_ROWS_PER_CHUNK = 30;
    /** 单文件最大字节 */
    public static final int MAX_FILE_BYTES = 6 * 1024 * 1024;
    /** 最多处理的数据行，防止超大表拖垮服务 */
    public static final int MAX_TOTAL_DATA_ROWS = 3000;

    private final CollabTableService tableService;
    private final BizProjectMemberMapper projectMemberMapper;
    private final BizUserMapper userMapper;
    private final AiAnalysisConfigService aiConfigService;
    private final DeepSeekClient deepSeekClient;
    private final AiTokenUsageMapper tokenUsageMapper;

    public CollabCsvAiImportService(CollabTableService tableService,
                                    BizProjectMemberMapper projectMemberMapper,
                                    BizUserMapper userMapper,
                                    AiAnalysisConfigService aiConfigService,
                                    DeepSeekClient deepSeekClient,
                                    AiTokenUsageMapper tokenUsageMapper) {
        this.tableService = tableService;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
        this.aiConfigService = aiConfigService;
        this.deepSeekClient = deepSeekClient;
        this.tokenUsageMapper = tokenUsageMapper;
    }

    public Map<String, Object> previewFromCsv(long projectId, MultipartFile file) throws Exception {
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

        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在管理后台「AI 配置」中填写 DeepSeek API Key");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";

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

        String schemaText = buildSchemaDescription(schema);
        String memberText = buildMemberLines(projectId);
        String system = buildSystemPrompt();

        List<CollabAiTaskController.AiTaskDraft> merged = new ArrayList<>();
        List<String> chunkErrors = new ArrayList<>();
        int totalChunks = (int) Math.ceil((double) parsed.dataRows().size() / MAX_ROWS_PER_CHUNK);
        int inTok = 0;
        int outTok = 0;

        for (int c = 0; c < totalChunks; c++) {
            int from = c * MAX_ROWS_PER_CHUNK;
            int to = Math.min(from + MAX_ROWS_PER_CHUNK, parsed.dataRows().size());
            List<String[]> chunk = parsed.dataRows().subList(from, to);
            String userMsg = buildChunkUserMessage(c + 1, totalChunks, parsed.header(), chunk);

            List<DeepSeekClient.ChatMessage> messages = List.of(
                    new DeepSeekClient.ChatMessage("system", system),
                    new DeepSeekClient.ChatMessage("user", schemaText + "\n\n" + memberText + "\n\n" + userMsg)
            );
            DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true, 8192);
            if (result == null || result.getContent() == null || result.getContent().isBlank()) {
                chunkErrors.add("第 " + (c + 1) + "/" + totalChunks + " 批：AI 无返回");
                continue;
            }
            inTok += result.getInputTokens();
            outTok += result.getOutputTokens();
            try {
                List<CollabAiTaskController.AiTaskDraft> part = CollabAiTaskResponseParser.parseDrafts(result.getContent());
                if (part.isEmpty()) {
                    chunkErrors.add("第 " + (c + 1) + "/" + totalChunks + " 批：未解析出任务");
                } else {
                    merged.addAll(part);
                }
            } catch (Exception e) {
                log.warn("CSV AI chunk parse failed: {}", e.getMessage());
                chunkErrors.add("第 " + (c + 1) + "/" + totalChunks + " 批：解析失败");
            }
        }

        if (inTok > 0 || outTok > 0) {
            try {
                tokenUsageMapper.insert(LocalDateTime.now(), "deepseek", model, inTok, outTok, inTok + outTok, null, "collab_csv_import");
            } catch (Exception e) {
                log.warn("Record DeepSeek token usage failed: {}", e.getMessage());
            }
        }

        if (merged.isEmpty()) {
            String hint = chunkErrors.isEmpty() ? "" : String.join("；", chunkErrors);
            throw new IllegalStateException("未能生成任何任务草稿" + (hint.isEmpty() ? "" : "：" + hint));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", merged);
        data.put("csvRowCount", parsed.dataRows().size());
        data.put("chunkCount", totalChunks);
        data.put("draftCount", merged.size());
        if (!chunkErrors.isEmpty()) {
            data.put("warnings", chunkErrors);
        }
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("inputTokens", inTok);
        usage.put("outputTokens", outTok);
        usage.put("model", model);
        data.put("usage", usage);
        return data;
    }

    private CsvParseResult parseCsv(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        // UTF-8 BOM
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

    private String buildChunkUserMessage(int chunkIndex, int totalChunks, String[] header, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("【本批 CSV 数据】第 ").append(chunkIndex).append("/").append(totalChunks).append(" 批，共 ")
                .append(rows.size()).append(" 行（每批最多 ").append(MAX_ROWS_PER_CHUNK).append(" 行）。\n");
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
}
