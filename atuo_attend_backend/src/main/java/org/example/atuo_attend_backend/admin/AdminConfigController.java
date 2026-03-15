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
}
