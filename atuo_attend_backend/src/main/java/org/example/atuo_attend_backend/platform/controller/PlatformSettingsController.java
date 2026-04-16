package org.example.atuo_attend_backend.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.platform.auth.PlatformAuthFilter;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsAuditMapper;
import org.example.atuo_attend_backend.platform.task.PlatformReportMailScheduler;
import org.example.atuo_attend_backend.report.service.MailSenderService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台级系统配置（SMTP、日报邮件调度），与租户无关。
 */
@RestController
@RequestMapping("/api/platform/settings")
public class PlatformSettingsController {

    private final SystemConfigService systemConfigService;
    private final MailSenderService mailSenderService;
    private final PlatformReportMailScheduler platformReportMailScheduler;
    private final PlatformOpsAuditMapper platformOpsAuditMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlatformSettingsController(SystemConfigService systemConfigService,
                                      MailSenderService mailSenderService,
                                      PlatformReportMailScheduler platformReportMailScheduler,
                                      PlatformOpsAuditMapper platformOpsAuditMapper) {
        this.systemConfigService = systemConfigService;
        this.mailSenderService = mailSenderService;
        this.platformReportMailScheduler = platformReportMailScheduler;
        this.platformOpsAuditMapper = platformOpsAuditMapper;
    }

    @GetMapping("/mail")
    public ApiResponse<Map<String, Object>> getMail() {
        Map<String, Object> data = new HashMap<>();
        data.put("publicBaseUrl", systemConfigService.getPublicBaseUrl());
        data.put("smtpHost", systemConfigService.getMailSmtpHost());
        data.put("smtpPort", systemConfigService.getMailSmtpPort());
        data.put("smtpUsername", systemConfigService.getMailSmtpUsername());
        data.put("smtpPasswordMasked", systemConfigService.getMailSmtpPasswordMasked());
        data.put("fromAddress", systemConfigService.getMailFromAddress());
        data.put("fromName", systemConfigService.getMailFromName());
        data.put("configured", mailSenderService.isConfigured());
        return ApiResponse.ok(data);
    }

    @PutMapping("/mail")
    public ApiResponse<Void> putMail(@RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        String publicBaseUrl = body != null ? asString(body.get("publicBaseUrl")) : null;
        String host = body != null ? asString(body.get("smtpHost")) : null;
        Integer port = body != null ? asInt(body.get("smtpPort")) : null;
        String username = body != null ? asString(body.get("smtpUsername")) : null;
        String password = body != null ? asString(body.get("smtpPassword")) : null;
        String fromAddress = body != null ? asString(body.get("fromAddress")) : null;
        String fromName = body != null ? asString(body.get("fromName")) : null;
        if (publicBaseUrl != null) {
            systemConfigService.setPublicBaseUrl(publicBaseUrl);
        }
        systemConfigService.saveMailSmtpConfig(host, port, username, password, fromAddress, fromName);
        audit(request, "platform.settings.mail", Map.of("action", "save"));
        return ApiResponse.ok(null);
    }

    @GetMapping("/report-mail")
    public ApiResponse<Map<String, Object>> getReportMail() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", systemConfigService.getPlatformReportMailEnabled());
        data.put("cron", systemConfigService.getPlatformReportMailCron());
        data.put("timezone", systemConfigService.getPlatformReportMailTimezone());
        return ApiResponse.ok(data);
    }

    @PutMapping("/report-mail")
    public ApiResponse<Void> putReportMail(@RequestBody(required = false) Map<String, Object> body,
                                           HttpServletRequest request) {
        Boolean enabled = body != null && body.get("enabled") instanceof Boolean b ? b : null;
        String cron = body != null ? asString(body.get("cron")) : null;
        String timezone = body != null ? asString(body.get("timezone")) : null;
        systemConfigService.savePlatformReportMailSettings(enabled, cron, timezone);
        platformReportMailScheduler.reschedule();
        audit(request, "platform.settings.report_mail",
                Map.of("enabled", String.valueOf(enabled != null ? enabled : systemConfigService.getPlatformReportMailEnabled()),
                        "cron", cron != null ? cron : systemConfigService.getPlatformReportMailCron()));
        return ApiResponse.ok(null);
    }

    /**
     * 发送一封测试 HTML 邮件（需已配置 SMTP）。
     */
    @PostMapping("/mail/test")
    public ApiResponse<Map<String, Object>> testMail(@RequestBody(required = false) Map<String, Object> body) {
        String to = body != null ? asString(body.get("toEmail")) : null;
        if (to == null || to.isBlank()) {
            return ApiResponse.error(40000, "toEmail 必填");
        }
        if (!mailSenderService.isConfigured()) {
            Map<String, Object> data = new HashMap<>();
            data.put("ok", false);
            data.put("message", "SMTP 未配置完整（host/port/发件人）");
            return ApiResponse.ok(data);
        }
        long start = System.currentTimeMillis();
        try {
            mailSenderService.sendHtml(to.trim(), "流帮Project 邮件连通测试",
                    "<p>这是一封来自监测台「系统配置」的 SMTP 连通性测试邮件。</p>");
            Map<String, Object> data = new HashMap<>();
            data.put("ok", true);
            data.put("message", "已发送，请查收收件箱（或垃圾箱）");
            data.put("latencyMs", System.currentTimeMillis() - start);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            Map<String, Object> data = new HashMap<>();
            data.put("ok", false);
            data.put("message", e.getMessage() != null ? e.getMessage() : "发送失败");
            data.put("latencyMs", System.currentTimeMillis() - start);
            return ApiResponse.ok(data);
        }
    }

    /** 项目信息发布：平台总开关与白名单（tenant_id=0 JSON） */
    @GetMapping("/project-marketplace")
    public ApiResponse<Map<String, Object>> getProjectMarketplace() {
        Map<String, Object> raw = systemConfigService.getMarketplaceProjectInfoConfig();
        Map<String, Object> data = new HashMap<>(raw);
        Object tids = raw.get("tenantIds");
        Object uids = raw.get("userIds");
        data.put("tenantWhitelistCount", tids instanceof List<?> l ? l.size() : 0);
        data.put("userWhitelistCount", uids instanceof List<?> l2 ? l2.size() : 0);
        return ApiResponse.ok(data);
    }

    @PutMapping("/project-marketplace")
    public ApiResponse<Void> putProjectMarketplace(@RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        if (body == null) {
            return ApiResponse.error(40000, "请求体不能为空");
        }
        try {
            systemConfigService.saveMarketplaceProjectInfoConfig(body);
            audit(request, "platform.settings.project_marketplace", Map.copyOf(body));
            return ApiResponse.ok(null);
        } catch (JsonProcessingException e) {
            return ApiResponse.error(40000, "配置 JSON 无效");
        }
    }

    private void audit(HttpServletRequest request, String action, Map<String, Object> payload) {
        Object sid = request.getAttribute(PlatformAuthFilter.ATTR_PLATFORM_SESSION_ID);
        Long sessionId = sid instanceof Long ? (Long) sid : null;
        String json = null;
        if (payload != null) {
            try {
                json = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                json = "{}";
            }
        }
        platformOpsAuditMapper.insert(sessionId, "platform", action, null, json);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }
}
