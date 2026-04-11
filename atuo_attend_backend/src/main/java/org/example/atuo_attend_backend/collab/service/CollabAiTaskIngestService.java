package org.example.atuo_attend_backend.collab.service;

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
import org.example.atuo_attend_backend.collab.dto.CollabAiTaskModels;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 多维表 AI 录入：预览与落库（协作端与公开客户阅览看板共用，Token 计入指定 tenantId）。
 */
@Service
public class CollabAiTaskIngestService {

    private static final Logger log = LoggerFactory.getLogger(CollabAiTaskIngestService.class);
    private static final ZoneId COLLAB_STATS_ZONE = ZoneId.of("Asia/Shanghai");

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

    public CollabAiTaskIngestService(CollabTableService tableService,
                                     CollabRecordService recordService,
                                     QwenClient qwenClient,
                                     AiAnalysisConfigService configService,
                                     BizAttachmentMapper attachmentMapper,
                                     MinioService minioService,
                                     AiTokenUsageMapper tokenUsageMapper,
                                     BizUserMapper userMapper,
                                     BizProjectMemberMapper projectMemberMapper) {
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

    public ApiResponse<?> preview(long tenantId, long projectId, String purpose, CollabAiTaskModels.AiTaskPreviewRequest body) {
        AiAnalysisConfig qwen = configService.getQwenConfig();
        if (qwen == null || !Boolean.TRUE.equals(qwen.getEnabled()) || qwen.getApiKey() == null || qwen.getApiKey().isBlank()) {
            return ApiResponse.error(40000, "AI 未启用或未配置通义·千问 API Key（由当前企业空间配置，消耗企业 API 额度）");
        }
        BizProjectTable table = tableService.getTableByProjectIdAndPurpose(projectId, purpose);
        if (table == null) return ApiResponse.error(40400, "项目未绑定表格");

        String rawText = body.getRawText() != null ? body.getRawText().trim() : "";
        if (rawText.isEmpty()) {
            return ApiResponse.error(40000, "请输入描述内容");
        }

        Map<String, Object> schema = tableService.getTableWithColumns(projectId, purpose);
        String userContent = buildUserContent(rawText, schema, body.getAttachmentIds());
        List<String> imageInputs = buildAttachmentImageDataUrls(body.getAttachmentIds());

        List<QwenClient.ChatMessage> messages = List.of(
                new QwenClient.ChatMessage("system", buildSystemPrompt()),
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
                tokenUsageMapper.insert(tenantId, LocalDateTime.now(), "qwen", result.getModel(),
                        result.getInputTokens(), result.getOutputTokens(), result.getTotalTokens(), null, null);
            } catch (Exception e) {
                log.warn("Record Qwen token usage failed: {}", e.getMessage());
            }
        }
        List<CollabAiTaskModels.AiTaskDraft> drafts;
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

    /**
     * @param createdByUserId 协作用户 ID；公开阅览看板传入 null，此时必须提供 creatorFallbackLabel
     */
    public ApiResponse<?> commit(long tenantId,
                                 long projectId,
                                 String purpose,
                                 Long createdByUserId,
                                 String creatorFallbackLabel,
                                 CollabAiTaskModels.AiTaskCommitRequest body) {
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
        String creatorLabel;
        if (createdByUserId != null) {
            BizUser actor = userMapper.findById(createdByUserId);
            creatorLabel = buildCreatorDisplayLabel(actor);
        } else {
            creatorLabel = creatorFallbackLabel != null && !creatorFallbackLabel.isBlank()
                    ? creatorFallbackLabel.trim() : "客户（阅览看板）";
        }
        String nowIso = LocalDateTime.now(COLLAB_STATS_ZONE).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Map<String, Long> emailToUserId = buildProjectMemberEmailMap(projectId);

        int created = 0;
        Map<Long, Boolean> projectAttachmentBoundOnce = new HashMap<>();
        for (CollabAiTaskModels.AiTaskDraft t : body.getTasks()) {
            Map<String, Object> fields = draftToRecordFields(t, columnByName, creatorLabel, nowIso, emailToUserId);
            if (!fields.isEmpty()) {
                try {
                    Long attachmentColId = null;
                    Object attachmentCol = columnByName.get("图像展示");
                    if (attachmentCol instanceof Map) {
                        Object idObj = ((Map<?, ?>) attachmentCol).get("id");
                        if (idObj != null) {
                            attachmentColId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
                            fields.remove("c" + attachmentColId);
                        }
                    }
                    var rec = recordService.createRecord(table.getId(), createdByUserId, fields);
                    if (t.getAttachmentIds() != null && !t.getAttachmentIds().isEmpty()) {
                        List<Long> finalIds = new ArrayList<>();
                        for (Long aid : t.getAttachmentIds()) {
                            if (aid == null) continue;
                            BizAttachment att = attachmentMapper.findById(aid);
                            if (att == null) continue;
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

    private Map<String, Object> draftToRecordFields(CollabAiTaskModels.AiTaskDraft t,
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
                } catch (Exception ignored) {
                }
            }
        }

        injectCreatorAndCreatedTime(fields, columnByName, creatorLabel, createdAtIso);
        return fields;
    }

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
        Map<String, Object> ccol = columnByName.get("创建人");
        if (ccol != null && creatorLabel != null && !creatorLabel.isBlank()) {
            putColumnField(fields, ccol, creatorLabel);
        }
        ccol = columnByName.get("创建时间");
        if (ccol != null && createdAtIso != null && !createdAtIso.isBlank()) {
            String ct = String.valueOf(ccol.getOrDefault("columnType", "datetime")).toLowerCase(Locale.ROOT);
            if ("date".equals(ct)) {
                String datePart = createdAtIso.length() >= 10 ? createdAtIso.substring(0, 10) : createdAtIso;
                putColumnField(fields, ccol, datePart);
            } else {
                putColumnField(fields, ccol, createdAtIso);
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
}
