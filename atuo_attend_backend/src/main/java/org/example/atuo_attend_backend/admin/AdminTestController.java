package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.commit.GithubDiffFetcher;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统能力测试接口（Diff 拉取、后续可扩展 AI/邮件等），供管理后台「测试页」调用。
 */
@RestController
@RequestMapping("/api/admin/test")
public class AdminTestController {

    private final GithubDiffFetcher githubDiffFetcher;

    public AdminTestController(GithubDiffFetcher githubDiffFetcher) {
        this.githubDiffFetcher = githubDiffFetcher;
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
     * AI API 可用性测试（占位，后续接入大模型后实现）。
     */
    @GetMapping("/ai")
    public ApiResponse<Map<String, Object>> testAi() {
        Map<String, Object> data = new HashMap<>();
        data.put("available", false);
        data.put("message", "AI 分析功能尚未接入，请参考《单次提交AI分析-功能设计文档》实现后再测试。");
        data.put("latencyMs", 0);
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
