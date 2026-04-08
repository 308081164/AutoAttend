package org.example.atuo_attend_backend.collab.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.QwenClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.collab.ai.CollabAiTaskResponseParser;
import org.example.atuo_attend_backend.collab.domain.BizAttachment;
import org.example.atuo_attend_backend.collab.domain.BizProjectMember;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final BizUserMapper userMapper;
    private final BizProjectMemberMapper projectMemberMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ZoneId COLLAB_STATS_ZONE = ZoneId.of("Asia/Shanghai");

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public CollabAiTaskController(CollabProjectService projectService,
                                  CollabTableService tableService,
                                  CollabRecordService recordService,
                                  QwenClient qwenClient,
                                  AiAnalysisConfigService configService,
                                  BizAttachmentMapper attachmentMapper,
                                  MinioService minioService,
                                  AiTokenUsageMapper tokenUsageMapper,
                                  BizUserMapper userMapper,
                                  BizProjectMemberMapper projectMemberMapper) {
        this.projectService = projectService;
        this.tableService = tableService;
        this.recordService = recordService;
        this.qwenClient = qwenClient;
        this.configService = configService;
        this.attachmentMapper = attachmentMapper;
        this.minioService = minioService;
        this.tokenUsageMapper = tokenUsageMapper;
        this.userMapper = userMapper;
        this.projectMemberMapper = projectMemberMapper;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @PostMapping("/projects/{projectId}/ai-tasks/preview")
    public ApiResponse<?> preview(@PathVariable long projectId,
                                  @RequestParam(defaultValue = "issue_tracking") String purpose,
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
        BizProjectTable table = tableService.getTableByProjectIdAndPurpose(projectId, purpose);
        if (table == null) return ApiResponse.error(40400, "项目未绑定表格");

        String rawText = body.getRawText() != null ? body.getRawText().trim() : "";
        if (rawText.isEmpty()) {
            return ApiResponse.error(40000, "请输入客户原始描述");
        }

        Map<String, Object> schema = tableService.getTableWithColumns(projectId, purpose);
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
                tokenUsageMapper.insert(tid(), LocalDateTime.now(), "qwen", result.getModel(),
                        result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens(), null, null);
            } catch (Exception e) {
                log.warn("Record Qwen token usage failed: {}", e.getMessage());
            }
        }
        List<AiTaskDraft> drafts;
        try {
            drafts = CollabAiTaskResponseParser.parseDrafts(result.getContent());
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
                                 @RequestParam(defaultValue = "issue_tracking") String purpose,
                                 @RequestBody AiTaskCommitRequest body,
                                 HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        BizProjectTable table = tableService.getTableByProjectIdAndPurpose(projectId, purpose);
        if (table == null) return ApiResponse.error(40400, "项目未绑定表格");
        if (body.getTasks() == null || body.getTasks().isEmpty()) {
            return ApiResponse.error(40000, "没有可插入的任务");
        }
        Map<String, Object> schema = tableService.getTableWithColumns(projectId, purpose);
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
        BizUser actor = userMapper.findById(userId);
        String creatorLabel = buildCreatorDisplayLabel(actor);
        String nowIso = LocalDateTime.now(COLLAB_STATS_ZONE).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Map<String, Long> emailToUserId = buildProjectMemberEmailMap(projectId);

        int created = 0;
        // 处理「同一个项目级附件（record_id=null）被多条记录复用」：第一条记录复用原附件，后续记录克隆一份新附件指向各自 recordId
        Map<Long, Boolean> projectAttachmentBoundOnce = new HashMap<>();
        for (AiTaskDraft t : body.getTasks()) {
            Map<String, Object> fields = draftToRecordFields(t, columnByName, creatorLabel, nowIso, emailToUserId);
            if (!fields.isEmpty()) {
                try {
                    // 附件列先不写入，等 recordId 确定且附件绑定/克隆后再回填，避免 attachmentIds 指向错误记录
                    Long attachmentColId = null;
                    Object attachmentCol = columnByName.get("图像展示");
                    if (attachmentCol instanceof Map) {
                        Object idObj = ((Map<?, ?>) attachmentCol).get("id");
                        if (idObj != null) {
                            attachmentColId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
                            fields.remove("c" + attachmentColId);
                        }
                    }
                    var rec = recordService.createRecord(table.getId(), userId, fields);
                    if (t.getAttachmentIds() != null && !t.getAttachmentIds().isEmpty()) {
                        List<Long> finalIds = new ArrayList<>();
                        for (Long aid : t.getAttachmentIds()) {
                            if (aid == null) continue;
                            BizAttachment att = attachmentMapper.findById(aid);
                            if (att == null) continue;
                            // project 级附件（recordId=null）：第一条记录复用原 id，其余记录克隆
                            if (att.getRecordId() == null) {
                                boolean used = Boolean.TRUE.equals(projectAttachmentBoundOnce.get(aid));
                                if (!used) {
                                    attachmentMapper.updateRecordId(aid, rec.getId());
                                    projectAttachmentBoundOnce.put(aid, true);
                                    finalIds.add(aid);
                                } else {
                                    BizAttachment copy = new BizAttachment();
                                    copy.setProjectId(att.getProjectId());
                                    copy.setRecordId(rec.getId());
                                    copy.setFileName(att.getFileName());
                                    copy.setFileSize(att.getFileSize());
                                    copy.setStorageKey(att.getStorageKey());
                                    copy.setUploadedBy(att.getUploadedBy());
                                    attachmentMapper.insert(copy);
                                    finalIds.add(copy.getId());
                                }
                            } else if (Objects.equals(att.getRecordId(), rec.getId())) {
                                finalIds.add(aid);
                            } else {
                                // 已绑定其他记录的附件：克隆一份给当前记录，避免「移动」导致其他记录丢附件
                                BizAttachment copy = new BizAttachment();
                                copy.setProjectId(att.getProjectId());
                                copy.setRecordId(rec.getId());
                                copy.setFileName(att.getFileName());
                                copy.setFileSize(att.getFileSize());
                                copy.setStorageKey(att.getStorageKey());
                                copy.setUploadedBy(att.getUploadedBy());
                                attachmentMapper.insert(copy);
                                finalIds.add(copy.getId());
                            }
                        }
                        if (attachmentColId != null && !finalIds.isEmpty()) {
                            Map<String, Object> attFields = new HashMap<>();
                            attFields.put("c" + attachmentColId, finalIds);
                            recordService.updateRecord(rec.getId(), attFields);
                        }
                    }
                    created++;
                } catch (IllegalArgumentException ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "无法继续插入";
                    if (created > 0) {
                        return ApiResponse.error(40010, msg + "（本轮已成功插入 " + created + " 条）");
                    }
                    return ApiResponse.error(40000, msg);
                }
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

    private Map<String, Long> buildProjectMemberEmailMap(long projectId) {
        List<BizProjectMember> members = projectMemberMapper.listByProjectId(projectId);
        Map<String, Long> out = new HashMap<>();
        for (BizProjectMember m : members) {
            if (m == null || m.getUserId() == null) continue;
            BizUser u = userMapper.findById(m.getUserId());
            if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
                out.put(u.getEmail().trim().toLowerCase(Locale.ROOT), u.getId());
            }
        }
        return out;
    }

    /**
     * 将一条任务草稿按表列名映射为 createRecord 所需的 fields（key 为 c+列id）。
     * 列名与草稿字段对应：问题描述<-title+description，归属模块/所属模块<-module，重要程度<-importantLevel，
     * 当前状态<-status，验收结果<-acceptResult，图像展示<-attachmentIds（JSON 数组）。
     * 创建人、创建时间始终使用当前登录用户与服务器时间写入（覆盖 AI 可能返回的空值）。
     */
    private Map<String, Object> draftToRecordFields(AiTaskDraft t,
                                                    Map<String, Map<String, Object>> columnByName,
                                                    String creatorLabel,
                                                    String createdAtIso,
                                                    Map<String, Long> emailToUserId) {
        Map<String, Object> fields = new HashMap<>();
        Object col;
        Object idObj;
        long colId;

        col = columnByName.get("负责人");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null
                && t.getOwners() != null && !t.getOwners().isEmpty()
                && emailToUserId != null && !emailToUserId.isEmpty()) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            List<Long> ownerIds = new ArrayList<>();
            for (String o : t.getOwners()) {
                if (o == null || o.isBlank()) continue;
                String key = o.trim().toLowerCase(Locale.ROOT);
                Long uid = emailToUserId.get(key);
                if (uid != null) {
                    ownerIds.add(uid);
                }
            }
            if (!ownerIds.isEmpty()) {
                try {
                    fields.put("c" + colId, objectMapper.writeValueAsString(ownerIds));
                } catch (Exception ignored) {
                }
            }
        }

        col = columnByName.get("问题描述");
        if (col != null && (idObj = ((Map<?, ?>) col).get("id")) != null) {
            colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            String desc = (t.getTitle() != null && !t.getTitle().isBlank() ? t.getTitle().trim() + "\n" : "")
                    + (t.getDescription() != null ? t.getDescription().trim() : "");
            if (!desc.isBlank()) fields.put("c" + colId, desc);
        }
        col = firstColumnByNames(columnByName, List.of("归属模块", "所属模块"));
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

        injectCreatorAndCreatedTime(fields, columnByName, creatorLabel, createdAtIso);
        return fields;
    }

    /** 展示名 + 角色（与协作「创建人」列人工录入习惯一致） */
    private static String buildCreatorDisplayLabel(BizUser u) {
        if (u == null) return "";
        String base = u.getName() != null && !u.getName().isBlank() ? u.getName().trim()
                : (u.getEmail() != null && !u.getEmail().isBlank() ? u.getEmail().trim() : "");
        String role = u.getRole() != null && !u.getRole().isBlank() ? u.getRole().trim() : "";
        if (!base.isEmpty() && !role.isEmpty()) {
            return base + "（" + role + "）";
        }
        if (!base.isEmpty()) return base;
        if (!role.isEmpty()) return role;
        return "用户#" + u.getId();
    }

    private static Map<String, Object> firstColumnByNames(Map<String, Map<String, Object>> columnByName, List<String> names) {
        for (String n : names) {
            if (n == null) continue;
            Map<String, Object> c = columnByName.get(n.trim());
            if (c != null) return c;
        }
        return null;
    }

    private static void injectCreatorAndCreatedTime(Map<String, Object> fields,
                                                    Map<String, Map<String, Object>> columnByName,
                                                    String creatorLabel,
                                                    String createdAtIso) {
        Map<String, Object> col = columnByName.get("创建人");
        if (col != null && creatorLabel != null && !creatorLabel.isBlank()) {
            putColumnField(fields, col, creatorLabel);
        }
        col = columnByName.get("创建时间");
        if (col != null && createdAtIso != null && !createdAtIso.isBlank()) {
            String ct = String.valueOf(col.getOrDefault("columnType", "datetime")).toLowerCase(Locale.ROOT);
            if ("date".equals(ct)) {
                String datePart = createdAtIso.length() >= 10 ? createdAtIso.substring(0, 10) : createdAtIso;
                putColumnField(fields, col, datePart);
            } else {
                putColumnField(fields, col, createdAtIso);
            }
        }
    }

    private static void putColumnField(Map<String, Object> fields, Map<String, Object> col, Object value) {
        if (value == null) return;
        Object idObj = col.get("id");
        if (idObj == null) return;
        long colId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
        fields.put("c" + colId, value);
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

