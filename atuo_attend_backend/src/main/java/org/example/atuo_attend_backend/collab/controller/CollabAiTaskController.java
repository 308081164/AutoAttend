package org.example.atuo_attend_backend.collab.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.QwenClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.collab.domain.BizAttachment;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 协作任务表 AI 录入模式：调用通义·千问多模态，将自然语言 + 截图整理为任务草稿。
 * 当前版本：仅支持文本输入，附件 ID 预留参数暂未参与 prompt。
 */
@RestController
@RequestMapping("/api/collab")
public class CollabAiTaskController {

    private final CollabProjectService projectService;
    private final CollabTableService tableService;
    private final CollabRecordService recordService;
    private final QwenClient qwenClient;
    private final AiAnalysisConfigService configService;
    private final BizAttachmentMapper attachmentMapper;
    private final MinioService minioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CollabAiTaskController(CollabProjectService projectService,
                                  CollabTableService tableService,
                                  CollabRecordService recordService,
                                  QwenClient qwenClient,
                                  AiAnalysisConfigService configService,
                                  BizAttachmentMapper attachmentMapper,
                                  MinioService minioService) {
        this.projectService = projectService;
        this.tableService = tableService;
        this.recordService = recordService;
        this.qwenClient = qwenClient;
        this.configService = configService;
        this.attachmentMapper = attachmentMapper;
        this.minioService = minioService;
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
        // 图片 URL 列表（10 分钟有效）
        List<String> imageUrls = buildAttachmentUrls(body.getAttachmentIds());

        List<QwenClient.ChatMessage> messages = List.of(
                new QwenClient.ChatMessage("system", systemPrompt),
                new QwenClient.ChatMessage("user", userContent, imageUrls)
        );
        QwenClient.ChatResult result = qwenClient.chat(qwen.getApiKey(), qwen.getModel(), messages, true);
        if (result == null || result.getContent() == null || result.getContent().isBlank()) {
            return ApiResponse.error(50000, "AI 返回为空或调用失败");
        }
        List<AiTaskDraft> drafts;
        try {
            drafts = objectMapper.readValue(result.getContent(), new TypeReference<List<AiTaskDraft>>() {});
        } catch (Exception e) {
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
        int created = 0;
        for (AiTaskDraft t : body.getTasks()) {
            Map<String, Object> fields = new HashMap<>();
            if (t.getDescription() != null && !t.getDescription().isBlank()) {
                fields.put("c_problem_desc", t.getDescription().trim());
            }
            // 其余字段映射暂留给后续迭代，通过前端表单直接保存到记录再补充
            if (!fields.isEmpty()) {
                recordService.createRecord(table.getId(), userId, fields);
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

    private List<String> buildAttachmentUrls(List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return List.of();
        List<String> urls = new ArrayList<>();
        for (Long id : attachmentIds) {
            if (id == null) continue;
            BizAttachment att = attachmentMapper.findById(id);
            if (att == null || att.getStorageKey() == null || att.getStorageKey().isBlank()) continue;
            try {
                String url = minioService.generatePresignedUrl(att.getStorageKey(), 600);
                if (url != null && !url.isBlank()) {
                    urls.add(url);
                }
            } catch (Exception ignored) {
                // 单个附件生成失败不影响整体
            }
        }
        return urls;
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
        private String importantLevel;
        private String status;
        private String acceptResult;
        private String module;
        private List<String> owners;
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

