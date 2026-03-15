package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.commit.GithubDiffFetcher;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统能力测试接口（Diff 拉取、AI 连通性、邮件等），供管理后台「测试页」调用。
 */
@RestController
@RequestMapping("/api/admin/test")
public class AdminTestController {

    private final GithubDiffFetcher githubDiffFetcher;
    private final AiAnalysisConfigService aiAnalysisConfigService;
    private final DeepSeekClient deepSeekClient;

    public AdminTestController(GithubDiffFetcher githubDiffFetcher,
                               AiAnalysisConfigService aiAnalysisConfigService,
                               DeepSeekClient deepSeekClient) {
        this.githubDiffFetcher = githubDiffFetcher;
        this.aiAnalysisConfigService = aiAnalysisConfigService;
        this.deepSeekClient = deepSeekClient;
    }

    /**
     * 测试当前服务器能否通过 GitHub API 拉取到 diff。
     * 使用与「查看 Diff」相同的代码路径（含 GITHUB_TOKEN、GITHUB_API_PROXY）。
     */
    @GetMapping("/diff")
    public ApiResponse<Map<String, Object>> testDiff(
            @RequestParam(value = "repoFullName", required = false) String repoFullName,
            @RequestParam(value = "commitSha", required = false) String commitSha) {
        String repo = (repoFullName != null && !repoFullName.isBlank()) ? repoFullName.trim() : "308081164/AutoAttend";
        String sha = (commitSha != null && !commitSha.isBlank()) ? commitSha.trim() : "1a0d338b10e65412fc35328ac66320c2360985ba";
        long start = System.currentTimeMillis();
        String diff = githubDiffFetcher.fetchDiff(repo, sha);
        long latencyMs = System.currentTimeMillis() - start;
        Map<String, Object> data = new HashMap<>();
        data.put("repoFullName", repo);
        data.put("commitSha", sha);
        data.put("latencyMs", latencyMs);
        data.put("hasDiff", diff != null && !diff.isBlank());
        data.put("diffLength", diff != null ? diff.length() : 0);
        if (diff != null && diff.length() > 200) {
            data.put("diffPreview", diff.substring(0, 200) + "...");
        } else {
            data.put("diffPreview", diff);
        }
        data.put("message", data.get("hasDiff").equals(Boolean.TRUE)
                ? "服务器可访问 GitHub API，已成功拉取 diff。"
                : "拉取失败或返回为空，可能原因：网络不可达 api.github.com、未配置 GITHUB_TOKEN 限流、或需配置 GITHUB_API_PROXY（大陆服务器）。");
        return ApiResponse.ok(data);
    }

    /**
     * AI 分析能力测试：读取当前 AI 配置，向 DeepSeek 发送一条简单对话，验证 API Key 与网络连通性。
     */
    @GetMapping("/ai")
    public ApiResponse<Map<String, Object>> testAi() {
        Map<String, Object> data = new HashMap<>();
        AiAnalysisConfig config = aiAnalysisConfigService.getConfig();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            data.put("available", false);
            data.put("message", "请先在「AI 配置」中填写 DeepSeek API Key。");
            data.put("latencyMs", 0);
            return ApiResponse.ok(data);
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            data.put("available", false);
            data.put("message", "请先在「AI 配置」中开启 AI 分析。");
            data.put("latencyMs", 0);
            return ApiResponse.ok(data);
        }
        long start = System.currentTimeMillis();
        String model = (config.getModel() != null && !config.getModel().isBlank()) ? config.getModel() : "deepseek-chat";
        List<DeepSeekClient.ChatMessage> messages = Collections.singletonList(
            new DeepSeekClient.ChatMessage("user", "请只回复一个词：OK")
        );
        String response = deepSeekClient.chat(config.getApiKey(), model, messages, false);
        long latencyMs = System.currentTimeMillis() - start;
        data.put("latencyMs", latencyMs);
        if (response != null && !response.isBlank()) {
            data.put("available", true);
            data.put("message", "DeepSeek API 连通正常，已收到响应。");
        } else {
            data.put("available", false);
            data.put("message", "DeepSeek API 调用失败或返回为空，请检查 API Key 与网络。");
        }
        return ApiResponse.ok(data);
    }

    /**
     * 邮件发送可用性测试（占位，后续接入邮件服务后实现）。
     */
    @GetMapping("/email")
    public ApiResponse<Map<String, Object>> testEmail() {
        Map<String, Object> data = new HashMap<>();
        data.put("available", false);
        data.put("message", "邮件功能尚未接入，请参考《工作日报邮件自动推送-功能设计文档》实现后再测试。");
        data.put("latencyMs", 0);
        return ApiResponse.ok(data);
    }
}
