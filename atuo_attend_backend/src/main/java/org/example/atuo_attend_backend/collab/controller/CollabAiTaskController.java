package org.example.atuo_attend_backend.collab.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.QwenClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.collab.domain.BizAttachment;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 协作任务表 AI 录入模式：调用通义·千问多模态，将自然语言 + 截图整理为任务草稿。
 * 当前版本：仅支持文本输入，附件 ID 预留参数暂未参与 prompt。
 */
@RestController
@RequestMapping("/api/collab")
public class CollabAiTaskController {

    private static final Logger log = LoggerFactory.getLogger(CollabAiTaskController.class);

    private final CollabProjectService projectService;
    private final CollabTableService tableService;
    private final CollabRecordService recordService;
    private final QwenClient qwenClient;
    private final AiAnalysisConfigService configService;
    private final BizAttachmentMapper attachmentMapper;
    private final MinioService minioService;
    private final AiTokenUsageMapper tokenUsageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CollabAiTaskController(CollabProjectService projectService,
                                  CollabTableService tableService,
                                  CollabRecordService recordService,
                                  QwenClient qwenClient,
                                  AiAnalysisConfigService configService,
                                  BizAttachmentMapper attachmentMapper,
                                  MinioService minioService,
                                  AiTokenUsageMapper tokenUsageMapper) {
        this.projectService = projectService;
        this.tableService = tableService;
        this.recordService = recordService;
        this.qwenClient = qwenClient;
        this.configService = configService;
        this.attachmentMapper = attachmentMapper;
        this.minioService = minioService;
        this.tokenUsageMapper = tokenUsageMapper;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @PostMapping("/projects/{projectId}/ai-tasks/preview")
    public ApiResponse<?> preview(@PathVariable long projectId,
                                  @RequestBody AiTaskPreviewRequest body,
                                  HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        AiAnalysisConfig qwen = configService.getQwenConfig();
        if (qwen == null || !Boolean.TRUE.equals(qwen.getEnabled()) || qwen.getApiKey() == null || qwen.getApiKey().isBlank()) {
            return ApiResponse.error(40000, "AI 未启用或未配置通义·千问 API Key");
        }
        BizProjectTable table = tableService.getTableByProjectId(projectId);
        if (table == null) return ApiResponse.error(40400, "项目未绑定表格");

        String rawText = body.getRawText() != null ? body.getRawText().trim() : "";
        if (rawText.isEmpty()) {
            return ApiResponse.error(40000, "请输入客户原始描述");
        }

        Map<String, Object> schema = tableService.getTableWithColumns(projectId);
        String systemPrompt = buildSystemPrompt();
        // 文本部分
        String userContent = buildUserContent(rawText, schema, body.getAttachmentIds());
        // 附件图片：从 MinIO 拉取真实文件并转为 Base64 data URL，通过官方接口传给千问（避免内网预签名 URL 无效）
        List<String> imageInputs = buildAttachmentImageDataUrls(body.getAttachmentIds());

        List<QwenClient.ChatMessage> messages = List.of(
                new QwenClient.ChatMessage("system", systemPrompt),
                new QwenClient.ChatMessage("user", userContent, imageInputs)
        );
        QwenClient.ChatResult result = qwenClient.chat(qwen.getApiKey(), qwen.getModel(), messages, true);
        if (result == null) {
            return ApiResponse.error(50000, "AI 返回为空或调用失败");
        }
        if (result.isError()) {
            return ApiResponse.error(50000, "AI 调用失败: " + result.getErrorMessage());
        }
        if (result.getContent() == null || result.getContent().isBlank()) {
            return ApiResponse.error(50000, "AI 返回为空或调用失败");
        }
        if (result.getInputTokens() > 0 || result.getOutputTokens() > 0) {
            try {
                tokenUsageMapper.insert(LocalDateTime.now(), "qwen", result.getModel(),
                        result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens(), null, null);
            } catch (Exception e) {
                log.warn("Record Qwen token usage failed: {}", e.getMessage());
            }
        }
        List<AiTaskDraft> drafts;
        try {
            drafts = parseAiTaskDraftsFromContent(result.getContent());
        } catch (Exception e) {
            log.warn("Parse AI task drafts failed: {} - content length={}", e.getMessage(), result.getContent().length());
            return ApiResponse.error(50000, "解析 AI 返回结果失败，请稍后重试");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", drafts);
        return ApiResponse.ok(data);
    }

    @PostMapping("/projects/{projectId}/ai-tasks/commit")
    public ApiResponse<?> commit(@PathVariable long projectId,
                                 @RequestBody AiTaskCommitRequest body,
                                 HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        BizProjectTable table = tableService.getTableByProjectId(projectId);
        if (table == null) return ApiResponse.error(40400, "项目未绑定表格");
        if (body.getTasks() == null || body.getTasks().isEmpty()) {
            return ApiResponse.error(40000, "没有可插入的任务");
        }
        Map<String, Object> schema = tableService.getTableWithColumns(projectId);
        if (schema == null) return ApiResponse.error(40400, "项目未绑定表格");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) schema.get("columns");
        if (columns == null) columns = List.of();
        Map<String, Map<String, Object>> columnByName = new HashMap<>();
        for (Map<String, Object> c : columns) {
            String name = (String) c.get("name");
            if (name != null && !name.isBlank()) {
                columnByName.put(name.trim(), c);
            }
        }
        int created = 0;
        for (AiTaskDraft t : body.getTasks()) {
            Map<String, Object> fields = draftToRecordFields(t, columnByName);
            if (!fields.isEmpty()) {
                var rec = recordService.createRecord(table.getId(), userId, fields);
                if (t.getAttachmentIds() != null) {
                    for (Long aid : t.getAttachmentIds()) {
                        if (aid == null) continue;
                        attachmentMapper.updateRecordId(aid, rec.getId());
                    }
                }
                created++;
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("createdCount", created);
        return ApiResponse.ok(data);
    }

    private String buildSystemPrompt() {
        return "你是一名资深产品经理，负责将客户的自然语言描述整理成若干条「原子化」的需求/任务。" +
                "每条任务应当清晰、专业、可执行，尽量只做一件事，便于研发在多维表中跟踪。" +
                "请仅输出 JSON 数组，不要输出任何多余文字。";
    }

    @SuppressWarnings("unchecked")
    private String buildUserContent(String rawText, Map<String, Object> schema, List<Long> attachmentIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("下面是客户的原始描述：\n");
        sb.append(rawText).append("\n\n");
        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            sb.append("以下是客户提供的附件（截图/文件），具体图片内容你可以通过消息中的 image_url 部分直接查看：\n");
            for (Long id : attachmentIds) {
                if (id == null) continue;
                BizAttachment att = attachmentMapper.findById(id);
                if (att != null) {
                    sb.append("- 附件ID: ").append(att.getId())
                            .append("，文件名: ").append(att.getFileName())
                            .append("，大小: ").append(att.getFileSize() != null ? att.getFileSize() : 0).append(" 字节\n");
                }
            }
            sb.append("\n");
        }
        sb.append("当前项目的任务表字段大致如下（供你参考任务拆分与命名，不要求逐列填满）：\n");
        Object colsObj = schema != null ? schema.get("columns") : null;
        if (colsObj instanceof List) {
            List<Map<String, Object>> cols = (List<Map<String, Object>>) colsObj;
            for (Map<String, Object> c : cols) {
                sb.append("- 列名: ").append(c.getOrDefault("name", ""))
                        .append("，类型: ").append(c.getOrDefault("columnType", "")).append("\n");
            }
        }
        sb.append("\n请根据以上信息，将需求拆分为若干条任务，输出 JSON 数组，例如：\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"title\": \"简要标题\",\n");
        sb.append("    \"description\": \"详细描述（便于研发理解，实现细节不限于此）\",\n");
        sb.append("    \"importantLevel\": \"严重紧急 或 下一阶段待办 等\",\n");
        sb.append("    \"status\": \"已创建 或 开发中 等\",\n");
        sb.append("    \"acceptResult\": \"未验收 或 待验收 等\",\n");
        sb.append("    \"owners\": [\"owner1@example.com\"],\n");
        sb.append("    \"module\": \"归属模块名称（如 项目协作、登录注册 等）\",\n");
        sb.append("    \"attachmentIds\": []\n");
        sb.append("  }\n");
        sb.append("]\n");
        return sb.toString();
    }

    /**
     * 从通义返回的 content 中解析出任务草稿列表。
     * 兼容：被 markdown 代码块包裹、对象内 items/tasks 数组、或直接 JSON 数组。
     */
    private List<AiTaskDraft> parseAiTaskDraftsFromContent(String content) throws Exception {
        if (content == null || content.isBlank()) return List.of();
        String json = content.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf("\n");
            if (start > 0) json = json.substring(start + 1);
            int end = json.lastIndexOf("```");
            if (end > 0) json = json.substring(0, end).trim();
        }
        int arrStart = json.indexOf('[');
        int arrEnd = json.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            json = json.substring(arrStart, arrEnd + 1);
        }
        json = json.replaceAll(",\\s*]", "]");
        JsonNode root = objectMapper.readTree(json);
        if (root.isArray()) {
            return objectMapper.convertValue(root, new TypeReference<List<AiTaskDraft>>() {});
        }
        if (root.has("items") && root.get("items").isArray()) {
            return objectMapper.convertValue(root.get("items"), new TypeReference<List<AiTaskDraft>>() {});
        }
        if (root.has("tasks") && root.get("tasks").isArray()) {
            return objectMapper.convertValue(root.get("tasks"), new TypeReference<List<AiTaskDraft>>() {});
        }
        throw new IllegalArgumentException("AI 返回中未找到 JSON 数组（需为 [...] 或 { items/tasks: [...] }）");
    }

    /**
     * 将一条任务草稿按表列名映射为 createRecord 所需的 fields（key 为 c+列id）。
     * 列名与草稿字段对应：问题描述<-title+description，归属模块<-module，重要程度<-importantLevel，
     * 当前状态<-status，验收结果<-acceptResult，图像展示<-attachmentIds（JSON 数组）。
     */
    private Map<String, Object> draftToRecordFields(AiTaskDraft t, Map<String, Map<String, Object>> columnByName) {
        Map<String, Object> fields = new HashMap<>();
        Object col;
        Object idObj;
        long colId;
        String colType;

        col = columnByName.get("问题描述");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            String desc = (t.getTitle() != null && !t.getTitle().isBlank() ? t.getTitle().trim() + "\n" : "")
                    + (t.getDescription() != null ? t.getDescription().trim() : "");
            if (!desc.isBlank()) fields.put("c" + colId, desc);
        }
        col = columnByName.get("归属模块");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            if (t.getModule() != null && !t.getModule().isBlank()) fields.put("c" + colId, t.getModule().trim());
        }
        col = columnByName.get("重要程度");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            if (t.getImportantLevel() != null && !t.getImportantLevel().isBlank()) fields.put("c" + colId, t.getImportantLevel().trim());
        }
        col = columnByName.get("当前状态");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            if (t.getStatus() != null && !t.getStatus().isBlank()) fields.put("c" + colId, t.getStatus().trim());
        }
        col = columnByName.get("验收结果");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            if (t.getAcceptResult() != null && !t.getAcceptResult().isBlank()) fields.put("c" + colId, t.getAcceptResult().trim());
        }
        col = columnByName.get("图像展示");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null && t.getAttachmentIds() != null && !t.getAttachmentIds().isEmpty()) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            List<Long> ids = new ArrayList<>();
            for (Long aid : t.getAttachmentIds()) {
                if (aid != null) ids.add(aid);
            }
            if (!ids.isEmpty()) {
                try {
                    fields.put("c" + colId, objectMapper.writeValueAsString(ids));
                } catch (Exception ignored) { }
            }
        }
        return fields;
    }

    /** 将附件中的图片从 MinIO 下载并转为 Base64 data URL，供千问多模态接口使用（不依赖公网 URL） */
    private List<String> buildAttachmentImageDataUrls(List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return List.of();
        List<String> list = new ArrayList<>();
        for (Long id : attachmentIds) {
            if (id == null) continue;
            BizAttachment att = attachmentMapper.findById(id);
            if (att == null || att.getStorageKey() == null || att.getStorageKey().isBlank()) continue;
            String dataUrl = minioService.getObjectAsImageDataUrl(att.getStorageKey(), att.getFileName());
            if (dataUrl != null && !dataUrl.isBlank()) {
                list.add(dataUrl);
            }
        }
        return list;
    }

    public static class AiTaskPreviewRequest {
        private String rawText;
        private List<Long> attachmentIds;

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }

        public List<Long> getAttachmentIds() {
            return attachmentIds;
        }

        public void setAttachmentIds(List<Long> attachmentIds) {
            this.attachmentIds = attachmentIds;
        }
    }

    public static class AiTaskCommitRequest {
        private List<AiTaskDraft> tasks;

        public List<AiTaskDraft> getTasks() {
            return tasks;
        }

        public void setTasks(List<AiTaskDraft> tasks) {
            this.tasks = tasks;
        }
    }

    public static class AiTaskDraft {
        private String title;
        private String description;
        @JsonAlias("important_level")
        private String importantLevel;
        private String status;
        @JsonAlias("accept_result")
        private String acceptResult;
        private String module;
        private List<String> owners;
        @JsonAlias("attachment_ids")
        private List<Long> attachmentIds;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImportantLevel() {
            return importantLevel;
        }

        public void setImportantLevel(String importantLevel) {
            this.importantLevel = importantLevel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAcceptResult() {
            return acceptResult;
        }

        public void setAcceptResult(String acceptResult) {
            this.acceptResult = acceptResult;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public List<String> getOwners() {
            return owners;
        }

        public void setOwners(List<String> owners) {
            this.owners = owners;
        }

        public List<Long> getAttachmentIds() {
            return attachmentIds;
        }

        public void setAttachmentIds(List<Long> attachmentIds) {
            this.attachmentIds = attachmentIds;
        }
    }
}

