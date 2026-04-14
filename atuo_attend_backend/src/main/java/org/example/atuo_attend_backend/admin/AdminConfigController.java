package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
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
    private final AiAnalysisConfigService aiAnalysisConfigService;

    public AdminConfigController(SystemConfigService systemConfigService,
                                 AiAnalysisConfigService aiAnalysisConfigService) {
        this.systemConfigService = systemConfigService;
        this.aiAnalysisConfigService = aiAnalysisConfigService;
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

    @GetMapping("/membership-plans")
    public ApiResponse<Map<String, Object>> getMembershipPlans() {
        Map<String, Object> data = new HashMap<>();
        data.put("freeMaxMembers", nz(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_FREE_MAX_MEMBERS), 20));
        data.put("freeMaxGithubRepos", nz(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_FREE_MAX_GITHUB_REPOS), 3));
        data.put("teamMaxMembers", nz(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_TEAM_MAX_MEMBERS), 100));
        data.put("teamMaxGithubRepos", nz(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_TEAM_MAX_GITHUB_REPOS), 20));
        data.put("proMaxMembers", nz(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_PRO_MAX_MEMBERS), 10_000));
        data.put("proMaxGithubRepos", nz(systemConfigService.getPlanQuota(SystemConfigService.KEY_PLAN_PRO_MAX_GITHUB_REPOS), 500));
        return ApiResponse.ok(data);
    }

    @PutMapping("/membership-plans")
    public ApiResponse<Void> updateMembershipPlans(@RequestBody Map<String, Object> body) {
        systemConfigService.saveMembershipPlanConfig(body);
        return ApiResponse.ok(null);
    }

    /**
     * 是否已设置「二级密钥」（仅布尔，不返回哈希）。
     */
    @GetMapping("/export-guard")
    public ApiResponse<Map<String, Object>> getExportGuard() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", systemConfigService.isExportSecondaryPasswordConfigured());
        return ApiResponse.ok(data);
    }

    /**
     * 设置二级密钥，或在校验当前密钥后清除。
     * body: { password, passwordConfirm } 设置；{ clear: true, currentPassword } 清除。
     */
    @PutMapping("/export-guard")
    public ApiResponse<Void> updateExportGuard(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return ApiResponse.error(40000, "请求体不能为空");
        }
        if (Boolean.TRUE.equals(body.get("clear"))) {
            String current = asString(body.get("currentPassword"));
            try {
                systemConfigService.clearExportSecondaryPassword(current);
            } catch (IllegalArgumentException e) {
                return ApiResponse.error(40000, e.getMessage());
            }
            return ApiResponse.ok(null);
        }
        String p1 = asString(body.get("password"));
        String p2 = asString(body.get("passwordConfirm"));
        if (p1 == null || p2 == null || !p1.equals(p2)) {
            return ApiResponse.error(40000, "两次输入的二级密钥不一致");
        }
        try {
            systemConfigService.setExportSecondaryPassword(p1);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
        return ApiResponse.ok(null);
    }

    /**
     * 验证二级密钥后返回已配置的敏感字段明文（用于迁移/备份）。未设置二级密钥时不可用。
     */
    @PostMapping("/sensitive-export")
    public ApiResponse<Map<String, Object>> sensitiveExport(@RequestBody Map<String, String> body) {
        if (!systemConfigService.isExportSecondaryPasswordConfigured()) {
            return ApiResponse.error(40000, "请先在 API 配置页设置二级密钥");
        }
        String pwd = body != null ? body.get("secondaryPassword") : null;
        if (!systemConfigService.verifyExportSecondaryPassword(pwd)) {
            return ApiResponse.error(40300, "二级密钥错误");
        }
        AiAnalysisConfig ds = aiAnalysisConfigService.getConfig();
        AiAnalysisConfig qw = aiAnalysisConfigService.getQwenConfig();
        Map<String, Object> data = new HashMap<>();
        data.put("deepseekApiKey", ds != null ? ds.getApiKey() : null);
        data.put("qwenApiKey", qw != null ? qw.getApiKey() : null);
        data.put("githubToken", systemConfigService.getGitHubToken());
        data.put("smtpPassword", systemConfigService.getMailSmtpPassword());
        return ApiResponse.ok(data);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static int nz(Integer v, int def) {
        return v != null && v > 0 ? v : def;
    }
}
