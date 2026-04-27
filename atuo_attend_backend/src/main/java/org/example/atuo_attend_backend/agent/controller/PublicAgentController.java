package org.example.atuo_attend_backend.agent.controller;

import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.BackgroundTextItem;
import org.example.atuo_attend_backend.agent.service.AgentSessionService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.report.service.MailSenderService;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 公开 Agent 接口 - 客户通过 publicToken 访问，无需登录
 */
@RestController
@RequestMapping("/api/public/agent")
public class PublicAgentController {

    private static final Logger log = LoggerFactory.getLogger(PublicAgentController.class);

    private final AgentSessionService sessionService;
    private final TenantMapper tenantMapper;
    private final QuoteProjectMapper projectMapper;
    private final MailSenderService mailSenderService;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PublicAgentController(AgentSessionService sessionService,
                                TenantMapper tenantMapper,
                                QuoteProjectMapper projectMapper,
                                MailSenderService mailSenderService,
                                SystemConfigService systemConfigService) {
        this.sessionService = sessionService;
        this.tenantMapper = tenantMapper;
        this.projectMapper = projectMapper;
        this.mailSenderService = mailSenderService;
        this.systemConfigService = systemConfigService;
    }

    // ========== 团队专属全自动 Agent 链接 ==========

    /**
     * 快速创建报价项目 + Agent 会话。
     * 公开接口，通过 tenant slug 标识团队。
     *
     * 请求体：{ "projectName": "xxx", "quoteKind": "single|solution" }
     * 返回：{ "agentUrl": "/agent/{publicToken}", "projectId": 123, "publicToken": "..." }
     */
    @PostMapping("/quick-start/{slug}")
    public ApiResponse<?> quickStart(@PathVariable String slug,
                                     @RequestBody QuickStartRequest request) {
        // 1. 校验参数
        String projectName = (request.getProjectName() == null) ? "" : request.getProjectName().trim();
        if (projectName.isEmpty()) {
            return ApiResponse.error(40000, "请输入项目名称/代号");
        }
        String quoteKind = request.getQuoteKind();
        if (quoteKind == null || quoteKind.isBlank()) {
            quoteKind = "single";
        }
        if (!"single".equals(quoteKind) && !"solution".equals(quoteKind)) {
            return ApiResponse.error(40000, "quoteKind 只支持 single 或 solution");
        }

        // 2. 查找租户
        Tenant tenant = tenantMapper.findBySlug(slug.trim().toLowerCase(Locale.ROOT));
        if (tenant == null) {
            return ApiResponse.error(40400, "团队不存在");
        }
        if ("suspended".equalsIgnoreCase(tenant.getStatus())) {
            return ApiResponse.error(40300, "团队已暂停");
        }

        Long tenantId = tenant.getId();

        // 3. 创建报价项目
        QuoteProject project = new QuoteProject();
        project.setTenantId(tenantId);
        project.setName(projectName);
        project.setProjectType("other");
        project.setTechStack("vue_node");
        project.setStatus("draft");
        project.setQuoteKind(quoteKind);
        project.setQuoteSubjectMode("legal_entity");
        projectMapper.insert(project);
        Long projectId = project.getId();

        // 4. 创建 Agent 会话
        List<BackgroundTextItem> backgrounds = new ArrayList<>();
        BackgroundTextItem bt1 = new BackgroundTextItem();
        bt1.setLabel("项目名称");
        bt1.setContent(projectName);
        backgrounds.add(bt1);
        BackgroundTextItem bt2 = new BackgroundTextItem();
        bt2.setLabel("报价模式");
        bt2.setContent("single".equals(quoteKind) ? "单体应用" : "解决方案级");
        backgrounds.add(bt2);

        AgentSession session = sessionService.createSession(
                tenantId, projectId, null, backgrounds, null
        );

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("publicToken", session.getPublicToken());
        result.put("agentUrl", "/agent/" + session.getPublicToken());
        result.put("projectName", projectName);
        result.put("quoteKind", quoteKind);

        // 6. 异步发送来单通知邮件
        sendQuickQuoteNotification(tenantId, tenant.getName(), projectName, quoteKind, projectId);

        return ApiResponse.ok(result);
    }

    /**
     * 异步发送来单邮件通知（不阻塞主流程）
     */
    private void sendQuickQuoteNotification(long tenantId, String teamName,
                                           String projectName, String quoteKind, Long projectId) {
        try {
            long configTenantId = 0L; // 平台级配置 tenant_id=0
            if (!systemConfigService.isQuickQuoteNotifyEnabled(configTenantId)) return;
            String notifyEmail = systemConfigService.getQuickQuoteNotifyEmail(configTenantId);
            if (notifyEmail == null || notifyEmail.isBlank()) return;

            String baseUrl = systemConfigService.getPublicBaseUrl();
            if (baseUrl == null) baseUrl = "";

            String modeLabel = "solution".equals(quoteKind) ? "解决方案级" : "单体应用";
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String html = "<div style=\"max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;\">"
                    + "<div style=\"background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:24px;border-radius:12px 12px 0 0;\">"
                    + "<h2 style=\"color:#fff;margin:0;font-size:20px;\">📬 新客户来单通知</h2>"
                    + "</div>"
                    + "<div style=\"background:#fff;padding:24px;border:1px solid #e5e7eb;border-top:none;border-radius:0 0 12px 12px;\">"
                    + "<p style=\"color:#333;font-size:15px;margin:0 0 16px;\">您有新客户通过专属链接创建了报价项目，请及时前往系统跟进。</p>"
                    + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
                    + "<tr><td style=\"padding:8px 0;color:#888;border-bottom:1px solid #f0f0f0;width:100px;\">团队</td>"
                    + "<td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0;font-weight:500;\">" + escHtml(teamName) + "</td></tr>"
                    + "<tr><td style=\"padding:8px 0;color:#888;border-bottom:1px solid #f0f0f0;\">项目名称</td>"
                    + "<td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0;font-weight:500;\">" + escHtml(projectName) + "</td></tr>"
                    + "<tr><td style=\"padding:8px 0;color:#888;border-bottom:1px solid #f0f0f0;\">报价模式</td>"
                    + "<td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0;font-weight:500;\">" + modeLabel + "</td></tr>"
                    + "<tr><td style=\"padding:8px 0;color:#888;border-bottom:1px solid #f0f0f0;\">创建时间</td>"
                    + "<td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0;font-weight:500;\">" + now + "</td></tr>"
                    + "<tr><td style=\"padding:8px 0;color:#888;\">项目 ID</td>"
                    + "<td style=\"padding:8px 0;font-weight:500;\">" + projectId + "</td></tr>"
                    + "</table>"
                    + "<div style=\"margin-top:20px;text-align:center;\">"
                    + "<a href=\"" + escHtml(baseUrl) + "/quote/" + projectId + "\" "
                    + "style=\"display:inline-block;padding:10px 24px;background:#667eea;color:#fff;border-radius:8px;"
                    + "text-decoration:none;font-size:14px;font-weight:500;\">立即查看项目</a>"
                    + "</div>"
                    + "<p style=\"color:#999;font-size:12px;margin:20px 0 0;text-align:center;\">此邮件由系统自动发送，请勿直接回复。</p>"
                    + "</div></div>";

            // 异步发送，不阻塞响应
            final String toEmail = notifyEmail;
            new Thread(() -> {
                try {
                    mailSenderService.sendHtml(toEmail, "📬 新客户来单 - " + projectName, html);
                    log.info("来单通知邮件已发送至 {}，项目：{}", toEmail, projectName);
                } catch (Exception e) {
                    log.warn("来单通知邮件发送失败，项目：{}，错误：{}", projectName, e.getMessage());
                }
            }, "mail-notify").start();
        } catch (Exception e) {
            log.warn("来单通知配置读取失败：{}", e.getMessage());
        }
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * 验证 slug 是否有效（前端落地页预检）
     */
    @GetMapping("/quick-start/{slug}/check")
    public ApiResponse<?> checkSlug(@PathVariable String slug) {
        Tenant tenant = tenantMapper.findBySlug(slug.trim().toLowerCase(Locale.ROOT));
        if (tenant == null) {
            return ApiResponse.error(40400, "团队不存在");
        }
        if ("suspended".equalsIgnoreCase(tenant.getStatus())) {
            return ApiResponse.error(40300, "团队已暂停");
        }
        Map<String, Object> result = new HashMap<>();
    result.put("teamName", tenant.getName());
    return ApiResponse.ok(result);
}

/**
 * 获取团队能力展示配置（公开接口，供 QuickQuoteLanding 页面调用）
 * 改为读取租户级配置，支持每个租户独立配置展示内容。
 */
@GetMapping("/showcase/{slug}")
public ApiResponse<?> getShowcase(@PathVariable String slug) {
    Tenant tenant = tenantMapper.findBySlug(slug.trim().toLowerCase(Locale.ROOT));
    if (tenant == null) {
        return ApiResponse.error(40400, "团队不存在");
    }
    if ("suspended".equalsIgnoreCase(tenant.getStatus())) {
        return ApiResponse.error(40300, "团队已暂停");
    }
    long tenantId = tenant.getId();
    Map<String, Object> data = new HashMap<>();
    data.put("enabled", systemConfigService.isTenantShowcaseEnabled(tenantId));
    data.put("mode", systemConfigService.getTenantShowcaseMode(tenantId));
    data.put("templateId", systemConfigService.getTenantShowcaseTemplateId(tenantId));
    data.put("teamName", tenant.getName());

    // 解析 content JSON
    String contentJson = systemConfigService.getTenantShowcaseContentJson(tenantId);
    if (contentJson != null && !contentJson.isBlank()) {
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            data.put("content", node);
        } catch (Exception e) {
            data.put("content", null);
        }
    } else {
        data.put("content", null);
    }

    // 自定义 HTML（仅在 mode=custom_html 时返回）
    if ("custom_html".equals(systemConfigService.getTenantShowcaseMode(tenantId))) {
        data.put("customHtml", systemConfigService.getTenantShowcaseCustomHtml(tenantId));
    } else {
        data.put("customHtml", null);
    }

    return ApiResponse.ok(data);
}

    // ========== 原有 Agent 接口 ==========

    /**
     * 获取会话信息及历史消息
     */
    @GetMapping("/sessions/{publicToken}")
    public ApiResponse<?> getSession(@PathVariable String publicToken) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("session", session);

        // 获取历史消息
        List<org.example.atuo_attend_backend.agent.domain.AgentMessage> messages = sessionService.getMessages(session.getId(), 100, 0);
        result.put("messages", messages);

        return ApiResponse.ok(result);
    }

    /**
     * 发送消息
     */
    @PostMapping("/sessions/{publicToken}/messages")
    public ApiResponse<?> sendMessage(@PathVariable String publicToken,
                                      @RequestBody org.example.atuo_attend_backend.agent.dto.AgentModels.SendMessageRequest request) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            return ApiResponse.error(40000, "会话已结束，无法继续发送消息");
        }

        org.example.atuo_attend_backend.agent.domain.AgentMessage message = sessionService.sendMessage(
                session.getId(),
                request.getContent(),
                request.getAttachmentIds()
        );

        return ApiResponse.ok(message);
    }

    /**
     * 上传附件
     */
    @PostMapping("/sessions/{publicToken}/attachments")
    public ApiResponse<?> uploadAttachment(@PathVariable String publicToken,
                                           @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            return ApiResponse.error(40000, "会话已结束，无法上传附件");
        }

        if (file.isEmpty()) {
            return ApiResponse.error(40000, "上传文件不能为空");
        }

        Map<String, Object> result = sessionService.uploadAttachment(
                session.getId(), session.getProjectId(),
                file.getOriginalFilename(), file.getContentType(),
                file.getSize(), file
        );

        return ApiResponse.ok(result);
    }

    /**
     * 描述完毕：confirmed=false 时仅预览摘要，confirmed=true 时确认结束会话。
     */
    @PostMapping("/sessions/{publicToken}/finish")
    public ApiResponse<?> finishSession(@PathVariable String publicToken,
                                        @RequestBody(required = false) org.example.atuo_attend_backend.agent.dto.AgentModels.FinishRequest request) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }

        boolean confirmed = request != null && Boolean.TRUE.equals(request.getConfirmed());

        Map<String, Object> result = new HashMap<>();
        if (confirmed) {
            // 确认提交：结束会话
            AgentSession updated = sessionService.confirmAndEnd(session.getId());
            result.put("session", updated);
            result.put("summary", updated.getSummaryText());
        } else {
            // 预览模式：仅生成摘要预览，不结束会话
            try {
                String summary = sessionService.generateSummaryPreview(session.getId());
                result.put("summary", summary);
            } catch (Exception e) {
                return ApiResponse.error(50000, "生成摘要失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }

        return ApiResponse.ok(result);
    }

    /**
     * 附件预览
     */
    @GetMapping("/attachments/{id}/preview")
    public ApiResponse<?> getAttachmentPreview(@PathVariable Long id) {
        Map<String, Object> result = sessionService.getAttachmentPreview(id);
        if (result == null) {
            return ApiResponse.error(40400, "附件不存在");
        }

        return ApiResponse.ok(result);
    }

    // ========== DTO ==========

    public static class QuickStartRequest {
        private String projectName;
        private String quoteKind;

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getQuoteKind() { return quoteKind; }
        public void setQuoteKind(String quoteKind) { this.quoteKind = quoteKind; }
    }
}
