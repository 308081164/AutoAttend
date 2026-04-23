package org.example.atuo_attend_backend.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.agent.domain.AgentMessage;
import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.BackgroundTextItem;
import org.example.atuo_attend_backend.agent.mapper.AgentMessageMapper;
import org.example.atuo_attend_backend.agent.mapper.AgentSessionMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.client.QwenClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisConfigMapper;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.collab.domain.BizAttachment;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.quote.service.QuoteCollabLinkService;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 会话服务 - 核心业务逻辑
 */
@Service
public class AgentSessionService {

    private static final Logger log = LoggerFactory.getLogger(AgentSessionService.class);
    private static final String PROVIDER_DEEPSEEK = "deepseek";
    private static final String PROVIDER_QWEN = "qwen";

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    private final BizAttachmentMapper attachmentMapper;
    private final MinioService minioService;
    private final DeepSeekClient deepSeekClient;
    private final QwenClient qwenClient;
    private final AiAnalysisConfigMapper aiAnalysisConfigMapper;
    private final AiTokenUsageMapper tokenUsageMapper;
    private final TenantResourceQuotaService tenantResourceQuotaService;
    private final QuoteCollabLinkService quoteCollabLinkService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 新建会话时可选背景附件数量上限 */
    private static final int MAX_BACKGROUND_ATTACHMENTS = 12;

    public AgentSessionService(AgentSessionMapper sessionMapper,
                               AgentMessageMapper messageMapper,
                               BizAttachmentMapper attachmentMapper,
                               MinioService minioService,
                               DeepSeekClient deepSeekClient,
                               QwenClient qwenClient,
                               AiAnalysisConfigMapper aiAnalysisConfigMapper,
                               AiTokenUsageMapper tokenUsageMapper,
                               TenantResourceQuotaService tenantResourceQuotaService,
                               QuoteCollabLinkService quoteCollabLinkService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.attachmentMapper = attachmentMapper;
        this.minioService = minioService;
        this.deepSeekClient = deepSeekClient;
        this.qwenClient = qwenClient;
        this.aiAnalysisConfigMapper = aiAnalysisConfigMapper;
        this.tokenUsageMapper = tokenUsageMapper;
        this.tenantResourceQuotaService = tenantResourceQuotaService;
        this.quoteCollabLinkService = quoteCollabLinkService;
    }

    private AiAnalysisConfig requireDeepSeekConfig(long tenantId) {
        AiAnalysisConfig cfg = aiAnalysisConfigMapper.findByProvider(tenantId, PROVIDER_DEEPSEEK);
        if (cfg == null || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            return null;
        }
        return cfg;
    }

    private AiAnalysisConfig requireQwenConfig(long tenantId) {
        AiAnalysisConfig cfg = aiAnalysisConfigMapper.findByProvider(tenantId, PROVIDER_QWEN);
        if (cfg == null || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            return null;
        }
        return cfg;
    }

    private static Map<String, Object> deepSeekResultToMap(DeepSeekClient.ChatResult r) {
        Map<String, Object> m = new HashMap<>();
        if (r == null) {
            m.put("content", null);
            m.put("inputTokens", 0);
            m.put("outputTokens", 0);
            return m;
        }
        m.put("content", r.getContent());
        m.put("inputTokens", r.getInputTokens());
        m.put("outputTokens", r.getOutputTokens());
        return m;
    }

    private List<DeepSeekClient.ChatMessage> buildDeepSeekMessages(String systemPrompt,
                                                                  List<Map<String, String>> history,
                                                                  String userContent) {
        List<DeepSeekClient.ChatMessage> msgs = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            msgs.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
        }
        if (history != null) {
            for (Map<String, String> h : history) {
                if (h == null) continue;
                String role = h.get("role");
                String content = h.get("content");
                if (role == null || content == null) continue;
                if (!"user".equals(role) && !"assistant".equals(role)) continue;
                msgs.add(new DeepSeekClient.ChatMessage(role, content));
            }
        }
        msgs.add(new DeepSeekClient.ChatMessage("user", userContent != null ? userContent : ""));
        return msgs;
    }

    /**
     * 调用 DeepSeek，按租户读取 API Key / model。
     */
    private Map<String, Object> chatDeepSeek(long tenantId,
                                            String systemPrompt,
                                            List<Map<String, String>> history,
                                            String userContent,
                                            boolean requestJson,
                                            Integer maxCompletionTokens) {
        AiAnalysisConfig cfg = requireDeepSeekConfig(tenantId);
        if (cfg == null) {
            log.warn("DeepSeek 未配置或缺少 API Key，tenantId={}", tenantId);
            return deepSeekResultToMap(null);
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";
        List<DeepSeekClient.ChatMessage> messages = buildDeepSeekMessages(systemPrompt, history, userContent);
        DeepSeekClient.ChatResult r = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, requestJson, maxCompletionTokens);
        return deepSeekResultToMap(r);
    }

    // ==================== 会话生命周期 ====================

    /**
     * 创建 Agent 会话
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentSession createSession(Long tenantId, Long projectId, String projectContext,
                                      List<BackgroundTextItem> backgroundTexts,
                                      List<Long> backgroundAttachmentIds) {
        log.info("Creating agent session: tenantId={}, projectId={}", tenantId, projectId);

        tenantResourceQuotaService.assertCanCreateAgentSession(tenantId);

        backgroundAttachmentIds = validateAndNormalizeBackgroundAttachmentIds(tenantId, projectId, backgroundAttachmentIds);

        // 1. 生成 48 字符随机 publicToken
        String publicToken = generatePublicToken();

        // 2. 构建会话对象
        AgentSession session = new AgentSession();
        session.setTenantId(tenantId);
        session.setProjectId(projectId);
        session.setPublicToken(publicToken);
        session.setStatus("active");
        session.setProjectContext(projectContext);
        session.setTotalMessages(0);
        session.setTotalInputTokens(0L);
        session.setTotalOutputTokens(0L);

        // 3. 处理背景材料
        boolean hasBackground = (backgroundTexts != null && !backgroundTexts.isEmpty())
                || (backgroundAttachmentIds != null && !backgroundAttachmentIds.isEmpty());

        if (hasBackground) {
            try {
                String backgroundSummary = processBackgroundMaterials(tenantId, backgroundTexts, backgroundAttachmentIds);
                session.setBackgroundContext(backgroundSummary);

                // 构建背景来源 JSON
                List<Map<String, Object>> sources = new ArrayList<>();
                if (backgroundTexts != null) {
                    for (BackgroundTextItem item : backgroundTexts) {
                        Map<String, Object> source = new HashMap<>();
                        source.put("type", "text");
                        source.put("label", item.getLabel());
                        sources.add(source);
                    }
                }
                if (backgroundAttachmentIds != null) {
                    for (Long attachmentId : backgroundAttachmentIds) {
                        Map<String, Object> source = new HashMap<>();
                        source.put("type", "attachment");
                        source.put("id", attachmentId);
                        sources.add(source);
                    }
                }
                session.setBackgroundSources(objectMapper.writeValueAsString(sources));
            } catch (Exception e) {
                log.error("Failed to process background materials", e);
            }
        }

        // 4. 插入数据库
        sessionMapper.insert(session);
        log.info("Agent session created: id={}, publicToken={}", session.getId(), publicToken);

        // 5. 生成第一条 assistant 消息
        try {
            String openingMessage = generateOpeningMessage(session);
            AgentMessage msg = new AgentMessage();
            msg.setSessionId(session.getId());
            msg.setRole("assistant");
            msg.setContent(openingMessage);
            msg.setContentType("text");
            messageMapper.insert(msg);

            session.setTotalMessages(1);
            sessionMapper.update(session);
        } catch (Exception e) {
            log.error("Failed to generate opening message", e);
        }

        return session;
    }

    /**
     * 报价页「新建 Agent 会话」：列出当前报价项目已上传的 Agent 背景附件。
     * 附件独立于协作项目，仅通过 quoteProjectId 关联。
     */
    public Map<String, Object> listBackgroundAttachmentsForQuote(long tenantId, long quoteProjectId) {
        // 使用 quoteProjectId 作为 MinIO 存储桶前缀，查询 project_id = quoteProjectId 的附件
        List<BizAttachment> list = attachmentMapper.listByProjectId(quoteProjectId);
        List<Map<String, Object>> items = list.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("fileName", a.getFileName());
            m.put("fileSize", a.getFileSize());
            m.put("createdAt", a.getCreatedAt());
            m.put("isImage", isImageFile(a.getFileName()));
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("collabProjectId", null);
        return data;
    }

    /**
     * 报价页上传背景附件：独立于协作项目，直接存储到 MinIO 并记录到 biz_attachment。
     * project_id 使用 quoteProjectId 用于 MinIO 存储桶隔离，agent_session_id 在创建会话时回填。
     */
    public Map<String, Object> uploadBackgroundAttachmentForQuote(long tenantId, long quoteProjectId,
                                                                   Long uploadedBy, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择文件");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        // 使用 quoteProjectId 作为 MinIO 存储桶前缀（仅用于存储隔离，不依赖 biz_project 表）
        String key = minioService.upload(quoteProjectId, 0L, originalName, file.getInputStream(), file.getSize());
        BizAttachment att = new BizAttachment();
        att.setProjectId(quoteProjectId);
        att.setRecordId(null);
        att.setAgentSessionId(null);
        att.setFileName(originalName);
        att.setFileSize(file.getSize());
        att.setStorageKey(key);
        att.setUploadedBy(uploadedBy);
        attachmentMapper.insertAgentAttachment(att);
        Map<String, Object> data = new HashMap<>();
        data.put("id", att.getId());
        data.put("fileName", att.getFileName());
        data.put("fileSize", att.getFileSize());
        data.put("createdAt", att.getCreatedAt());
        data.put("isImage", isImageFile(att.getFileName()));
        data.put("collabProjectId", null);
        return data;
    }

    /**
     * 校验「背景附件」ID 均有效，并去重、限制数量。
     * 附件独立于协作项目，直接校验 ID 存在性即可。
     */
    private List<Long> validateAndNormalizeBackgroundAttachmentIds(long tenantId, long quoteProjectId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<Long> dedup = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (dedup.size() > MAX_BACKGROUND_ATTACHMENTS) {
            throw new IllegalArgumentException("背景附件最多选择 " + MAX_BACKGROUND_ATTACHMENTS + " 个");
        }
        for (Long id : dedup) {
            BizAttachment att = attachmentMapper.findById(id);
            if (att == null) {
                throw new IllegalArgumentException("附件不存在，ID=" + id);
            }
        }
        return dedup;
    }

    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentMessage sendMessage(Long sessionId, String content, List<Long> attachmentIds) {
        log.info("Sending message: sessionId={}", sessionId);

        // 1. 查找并验证会话
        AgentSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            throw new RuntimeException("会话已结束，无法继续发送消息");
        }

        // 2. 保存用户消息
        AgentMessage userMsg = new AgentMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setContentType("text");
        messageMapper.insert(userMsg);

        // 3. 处理附件（图片用 Qwen VL 理解）
        StringBuilder attachmentDesc = new StringBuilder();
        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            for (Long attachmentId : attachmentIds) {
                try {
                    String desc = processAttachment(session.getTenantId(), attachmentId);
                    if (desc != null && !desc.isEmpty()) {
                        attachmentDesc.append("\n\n[附件描述]: ").append(desc);
                    }
                } catch (Exception e) {
                    log.warn("Failed to process attachment: id={}", attachmentId, e);
                }
            }
        }

        // 4. 构建上下文
        String systemPrompt = buildSystemPrompt(session);
        List<Map<String, String>> historyMessages = getRecentMessages(sessionId, 20);

        // 将用户消息加入历史
        String userContent = content;
        if (attachmentDesc.length() > 0) {
            userContent += attachmentDesc.toString();
        }

        // 5. 调用 DeepSeek
        String assistantReply;
        int inputTokens = 0;
        int outputTokens = 0;
        try {
            Map<String, Object> usageResult = chatDeepSeek(session.getTenantId(), systemPrompt, historyMessages, userContent, false, null);
            assistantReply = (String) usageResult.get("content");
            inputTokens = usageResult.get("inputTokens") != null ? ((Number) usageResult.get("inputTokens")).intValue() : 0;
            outputTokens = usageResult.get("outputTokens") != null ? ((Number) usageResult.get("outputTokens")).intValue() : 0;
        } catch (Exception e) {
            log.error("DeepSeek chat failed", e);
            assistantReply = "抱歉，我暂时无法回复，请稍后再试。";
        }

        // 6. 保存 assistant 回复
        AgentMessage assistantMsg = new AgentMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantReply);
        assistantMsg.setContentType("text");
        assistantMsg.setInputTokens(inputTokens);
        assistantMsg.setOutputTokens(outputTokens);
        messageMapper.insert(assistantMsg);

        // 7. 更新会话统计
        session.setTotalMessages(session.getTotalMessages() + 2);
        session.setTotalInputTokens(session.getTotalInputTokens() + inputTokens);
        session.setTotalOutputTokens(session.getTotalOutputTokens() + outputTokens);
        sessionMapper.update(session);

        // 8. 记录 token 用量
        try {
            AiAnalysisConfig dsCfg = requireDeepSeekConfig(session.getTenantId());
            String modelLabel = dsCfg != null && dsCfg.getModel() != null && !dsCfg.getModel().isBlank()
                    ? dsCfg.getModel() : "deepseek-chat";
            recordTokenUsage(session.getTenantId(), modelLabel, PROVIDER_DEEPSEEK, inputTokens, outputTokens);
        } catch (Exception e) {
            log.warn("Failed to record token usage", e);
        }

        return assistantMsg;
    }

    /**
     * 生成摘要预览
     */
    public String generateSummaryPreview(Long sessionId) {
        log.info("Generating summary preview: sessionId={}", sessionId);

        AgentSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        long tenantId = session.getTenantId() != null ? session.getTenantId() : 1L;

        List<AgentMessage> messages = messageMapper.listBySessionId(sessionId, 100, 0);
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder conversation = new StringBuilder();
        for (AgentMessage msg : messages) {
            conversation.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        String prompt = "请根据以下对话内容，生成一份结构化的需求摘要。摘要应包含：\n" +
                "1. 项目背景概述\n" +
                "2. 核心需求列表（按优先级排列）\n" +
                "3. 功能描述要点\n" +
                "4. 特殊要求或约束\n\n" +
                "对话内容：\n" + conversation.toString() + "\n\n" +
                "请用中文输出，格式清晰，约500字。";

        try {
            Map<String, Object> result = chatDeepSeek(
                    tenantId,
                    "你是一个专业的需求分析师，擅长从对话中提炼结构化需求文档。",
                    null,
                    prompt,
                    false,
                    8192);
            return (String) result.get("content");
        } catch (Exception e) {
            log.error("Failed to generate summary preview", e);
            throw new RuntimeException("生成摘要失败");
        }
    }

    /**
     * 确认并结束会话
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentSession confirmAndEnd(Long sessionId) {
        log.info("Confirming and ending session: sessionId={}", sessionId);

        AgentSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            throw new RuntimeException("会话已结束");
        }

        // 生成最终摘要
        try {
            String summary = generateSummaryPreview(sessionId);
            session.setSummaryText(summary);
        } catch (Exception e) {
            log.warn("Failed to generate final summary", e);
        }

        session.setStatus("ended");
        session.setEndedAt(LocalDateTime.now());
        session.setEndedBy("customer");
        sessionMapper.update(session);

        return session;
    }

    /**
     * 终止会话（运营方操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentSession terminateSession(Long sessionId) {
        log.info("Terminating session: sessionId={}", sessionId);

        AgentSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            throw new RuntimeException("会话已结束");
        }

        // 生成已有内容的摘要
        try {
            String summary = generateSummaryPreview(sessionId);
            session.setSummaryText(summary);
        } catch (Exception e) {
            log.warn("Failed to generate termination summary", e);
        }

        session.setStatus("terminated");
        session.setEndedAt(LocalDateTime.now());
        session.setEndedBy("operator");
        sessionMapper.update(session);

        return session;
    }

    /**
     * 根据 publicToken 获取会话
     */
    public AgentSession getByPublicToken(String publicToken) {
        return sessionMapper.findByPublicToken(publicToken);
    }

    /**
     * 获取会话详情
     */
    public AgentSession getSession(Long sessionId) {
        return sessionMapper.findById(sessionId);
    }

    /**
     * 获取会话消息列表
     */
    public List<AgentMessage> getMessages(Long sessionId, int limit, int offset) {
        return messageMapper.listBySessionId(sessionId, limit, offset);
    }

    /**
     * 获取项目下的会话列表
     */
    public List<AgentSession> listByProject(Long projectId) {
        return sessionMapper.listByProjectId(projectId);
    }

    // ==================== 附件操作 ====================

    /**
     * 上传附件到会话
     */
    public Map<String, Object> uploadAttachment(Long sessionId, Long projectId,
                                                String originalFilename, String contentType,
                                                long size, MultipartFile file) {
        try {
            // 上传到 MinIO，返回实际 storageKey
            String storageKey = minioService.upload(
                    projectId != null ? projectId : 0L, sessionId,
                    originalFilename, file.getInputStream(), file.getSize());

            // 保存附件记录
            BizAttachment attachment = new BizAttachment();
            attachment.setProjectId(projectId != null ? projectId : 0L);
            attachment.setRecordId(sessionId);
            attachment.setFileName(originalFilename);
            attachment.setFileSize(size);
            attachment.setStorageKey(storageKey);
            attachmentMapper.insert(attachment);

            Map<String, Object> result = new HashMap<>();
            result.put("id", attachment.getId());
            result.put("fileName", originalFilename);
            result.put("fileSize", size);
            result.put("fileType", contentType);
            result.put("storageKey", storageKey);

            return result;
        } catch (Exception e) {
            log.error("Failed to upload attachment: sessionId={}", sessionId, e);
            throw new RuntimeException("上传附件失败: " + e.getMessage());
        }
    }

    /**
     * 获取附件预览信息
     */
    public Map<String, Object> getAttachmentPreview(Long id) {
        BizAttachment attachment = attachmentMapper.findById(id);
        if (attachment == null) {
            return null;
        }

        try {
            String previewUrl = minioService.generatePresignedUrl(attachment.getStorageKey(), 3600);

            Map<String, Object> result = new HashMap<>();
            result.put("id", attachment.getId());
            result.put("fileName", attachment.getFileName());
            result.put("fileSize", attachment.getFileSize());
            result.put("previewUrl", previewUrl);

            return result;
        } catch (Exception e) {
            log.error("Failed to get attachment preview: id={}", id, e);
            throw new RuntimeException("获取附件预览失败: " + e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 处理背景材料，生成背景摘要
     */
    private String processBackgroundMaterials(long tenantId,
                                              List<BackgroundTextItem> backgroundTexts,
                                              List<Long> attachmentIds) {
        StringBuilder allMaterials = new StringBuilder();
        int totalChars = 0;
        int maxChars = 8000;

        // 收集文本内容
        if (backgroundTexts != null) {
            for (BackgroundTextItem item : backgroundTexts) {
                String text = "[" + item.getLabel() + "]\n" + item.getContent();
                if (totalChars + text.length() > maxChars) {
                    text = text.substring(0, Math.max(0, maxChars - totalChars)) + "...(已截断)";
                }
                allMaterials.append("\n\n").append(text);
                totalChars += text.length();
                if (totalChars >= maxChars) {
                    break;
                }
            }
        }

        // 处理附件
        if (attachmentIds != null && totalChars < maxChars) {
            for (Long attachmentId : attachmentIds) {
                try {
                    String desc = processAttachment(tenantId, attachmentId);
                    if (desc != null && !desc.isEmpty()) {
                        String text = "[附件 " + attachmentId + " 描述]\n" + desc;
                        if (totalChars + text.length() > maxChars) {
                            text = text.substring(0, Math.max(0, maxChars - totalChars)) + "...(已截断)";
                        }
                        allMaterials.append("\n\n").append(text);
                        totalChars += text.length();
                    }
                } catch (Exception e) {
                    log.warn("Failed to process background attachment: id={}", attachmentId, e);
                }
                if (totalChars >= maxChars) {
                    break;
                }
            }
        }

        if (allMaterials.length() == 0) {
            return "";
        }

        // 调用 DeepSeek 生成背景摘要
        String prompt = "请根据以下背景材料，生成一份约500字的项目背景摘要。" +
                "摘要应提炼关键信息，包括：项目类型、业务领域、核心目标、主要功能需求等。\n\n" +
                "背景材料：\n" + allMaterials.toString();

        try {
            Map<String, Object> result = chatDeepSeek(
                    tenantId,
                    "你是一个专业的项目分析师，擅长从杂乱的背景材料中提炼关键信息。",
                    null,
                    prompt,
                    false,
                    8192);
            return (String) result.get("content");
        } catch (Exception e) {
            log.error("Failed to generate background summary", e);
            return allMaterials.toString();
        }
    }

    /**
     * 处理单个附件
     */
    private String processAttachment(long tenantId, Long attachmentId) {
        // 查询附件信息
        BizAttachment attachment = attachmentMapper.findById(attachmentId);
        if (attachment == null) {
            log.warn("Attachment not found: id={}", attachmentId);
            return null;
        }

        String fileName = attachment.getFileName();
        String storageKey = attachment.getStorageKey();

        // 如果是图片，调用 Qwen VL 生成描述
        if (isImageFile(fileName)) {
            try {
                AiAnalysisConfig qCfg = requireQwenConfig(tenantId);
                if (qCfg == null) {
                    log.warn("Qwen 未配置，无法描述图片: tenantId={}", tenantId);
                    return "[图片附件，未配置通义 API Key]";
                }
                String imageUrl = minioService.generatePresignedUrl(storageKey, 3600);
                String model = qCfg.getModel() != null && !qCfg.getModel().isBlank() ? qCfg.getModel() : "qwen-vl-plus";
                List<QwenClient.ChatMessage> msgs = List.of(new QwenClient.ChatMessage(
                        "user",
                        "请用中文简要描述这张图片的内容与关键信息，用于项目需求沟通上下文（约 200 字以内）。",
                        List.of(imageUrl)));
                QwenClient.ChatResult qr = qwenClient.chat(qCfg.getApiKey(), model, msgs, false);
                if (qr == null || qr.isError()) {
                    return "[图片附件，AI 描述失败: " + (qr != null ? qr.getErrorMessage() : "null") + "]";
                }
                return qr.getContent();
            } catch (Exception e) {
                log.error("Failed to describe image: attachmentId={}", attachmentId, e);
                return "[图片附件，AI 描述生成失败]";
            }
        }

        // 如果是文档，提取文本（模拟）
        if (isDocumentFile(fileName)) {
            try {
                return extractTextFromDocument(storageKey);
            } catch (Exception e) {
                log.error("Failed to extract text from document: attachmentId={}", attachmentId, e);
                return "[文档附件，文本提取失败]";
            }
        }

        return "[不支持的附件类型]";
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp")
                || lower.endsWith(".bmp");
    }

    /**
     * 判断是否为文档文件
     */
    private boolean isDocumentFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf")
                || lower.endsWith(".doc") || lower.endsWith(".docx")
                || lower.endsWith(".xls") || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt") || lower.endsWith(".pptx")
                || lower.endsWith(".txt");
    }

    /**
     * 从文档中提取文本（模拟实现，实际需要 PDFBox/POI）
     */
    private String extractTextFromDocument(String storageKey) {
        // TODO: 实际项目中使用 PDFBox 或 Apache POI 提取文本
        // 当前为模拟实现
        log.info("Extracting text from document: {} (simulated)", storageKey);
        return "[文档内容已上传，具体文本提取需集成 PDFBox/POI 库]";
    }

    /**
     * 构建 System Prompt
     */
    private String buildSystemPrompt(AgentSession session) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的项目需求顾问 AI 助手。你的职责是：\n");
        prompt.append("1. 通过对话引导客户清晰地描述他们的项目需求\n");
        prompt.append("2. 提出有针对性的问题，帮助客户梳理需求细节\n");
        prompt.append("3. 对客户的需求进行专业分析和建议\n");
        prompt.append("4. 最终生成结构化的需求摘要文档\n\n");
        prompt.append("注意事项：\n");
        prompt.append("- 使用友好、专业的语气\n");
        prompt.append("- 每次回复控制在合理长度，避免过长\n");
        prompt.append("- 主动引导对话方向，不要被动等待\n");
        prompt.append("- 对技术细节给出专业建议\n");

        // 添加项目上下文
        if (session.getProjectContext() != null && !session.getProjectContext().isEmpty()) {
            prompt.append("\n\n【项目上下文】\n");
            prompt.append(session.getProjectContext());
        }

        // 添加背景资料
        if (session.getBackgroundContext() != null && !session.getBackgroundContext().isEmpty()) {
            prompt.append("\n\n【背景资料摘要】\n");
            prompt.append("以下是从客户提供的背景材料中提取的摘要信息，请在对话中参考：\n");
            prompt.append(session.getBackgroundContext());
        }

        return prompt.toString();
    }

    /**
     * 生成开场白
     */
    private String generateOpeningMessage(AgentSession session) {
        if (session.getBackgroundContext() != null && !session.getBackgroundContext().isEmpty()) {
            // 基于背景生成个性化开场白
            String prompt = "根据以下项目背景摘要，生成一段友好的开场白（约100字）。" +
                    "开场白应：\n" +
                    "1. 表明你已经了解了客户的项目背景\n" +
                    "2. 简要提及你理解到的关键信息\n" +
                    "3. 引导客户开始描述具体需求\n\n" +
                    "背景摘要：\n" + session.getBackgroundContext();

            try {
                Map<String, Object> result = chatDeepSeek(
                        session.getTenantId() != null ? session.getTenantId() : 1L,
                        "你是一个专业的项目需求顾问 AI 助手。",
                        null,
                        prompt,
                        false,
                        null);
                return (String) result.get("content");
            } catch (Exception e) {
                log.error("Failed to generate personalized opening", e);
            }
        }

        // 通用开场白
        return "您好！我是您的项目需求顾问助手。我会通过对话帮助您梳理和描述项目需求。\n\n" +
                "请告诉我，您希望做一个什么样的项目？可以从以下几个方面开始：\n" +
                "- 这个项目要解决什么问题？\n" +
                "- 主要面向哪些用户？\n" +
                "- 您期望实现的核心功能是什么？\n\n" +
                "不用一次说全，我们可以一步步来聊。";
    }

    /**
     * 获取最近的历史消息
     */
    private List<Map<String, String>> getRecentMessages(Long sessionId, int limit) {
        List<AgentMessage> messages = messageMapper.listBySessionId(sessionId, limit, 0);
        List<Map<String, String>> history = new ArrayList<>();
        for (AgentMessage msg : messages) {
            Map<String, String> item = new HashMap<>();
            item.put("role", msg.getRole());
            item.put("content", msg.getContent());
            history.add(item);
        }
        return history;
    }

    /**
     * 生成 48 字符随机 publicToken
     */
    private String generatePublicToken() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String random = UUID.randomUUID().toString().replace("-", "");
        return uuid + random.substring(0, 48 - uuid.length());
    }

    /**
     * 记录 token 用量
     */
    private void recordTokenUsage(Long tenantId, String model, String provider, int inputTokens, int outputTokens) {
        try {
            int inTok = Math.max(0, inputTokens);
            int outTok = Math.max(0, outputTokens);
            int total = inTok + outTok;
            tokenUsageMapper.insert(
                    tenantId != null ? tenantId : 1L,
                    LocalDateTime.now(),
                    provider != null ? provider : PROVIDER_DEEPSEEK,
                    model != null ? model : "unknown",
                    inTok,
                    outTok,
                    total,
                    null,
                    null);
        } catch (Exception e) {
            log.warn("Failed to record token usage", e);
        }
    }
}
