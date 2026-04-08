package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统/集成配置（GitHub Token 等），与 DeepSeek 一样在管理后台表单填写、维护。
 */
@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final SystemConfigService systemConfigService;

    public AdminConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/github")
    public ApiResponse<Map<String, Object>> getGitHubConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("githubTokenMasked", systemConfigService.getGitHubTokenMasked());
        data.put("hasGitHubToken", systemConfigService.getGitHubToken() != null);
        data.put("githubApiProxy", systemConfigService.getGitHubApiProxy());
        return ApiResponse.ok(data);
    }

    @PutMapping("/github")
    public ApiResponse<Void> updateGitHubConfig(@RequestBody Map<String, String> body) {
        String token = body != null ? body.get("githubToken") : null;
        String proxy = body != null ? body.get("githubApiProxy") : null;
        if (token != null && !token.isBlank() && !token.contains("****")) {
            systemConfigService.setGitHubToken(token);
        }
        if (proxy != null) {
            systemConfigService.setGitHubApiProxy(proxy);
        }
        return ApiResponse.ok(null);
    }

    @GetMapping("/mail")
    public ApiResponse<Map<String, Object>> getMailConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("publicBaseUrl", systemConfigService.getPublicBaseUrl());
        data.put("smtpHost", systemConfigService.getMailSmtpHost());
        data.put("smtpPort", systemConfigService.getMailSmtpPort());
        data.put("smtpUsername", systemConfigService.getMailSmtpUsername());
        data.put("smtpPasswordMasked", systemConfigService.getMailSmtpPasswordMasked());
        data.put("fromAddress", systemConfigService.getMailFromAddress());
        data.put("fromName", systemConfigService.getMailFromName());
        data.put("configured", systemConfigService.getMailSmtpHost() != null
                && systemConfigService.getMailSmtpPort() != null
                && systemConfigService.getMailFromAddress() != null);
        return ApiResponse.ok(data);
    }

    @PutMapping("/mail")
    public ApiResponse<Void> updateMailConfig(@RequestBody Map<String, Object> body) {
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
        return ApiResponse.ok(null);
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
