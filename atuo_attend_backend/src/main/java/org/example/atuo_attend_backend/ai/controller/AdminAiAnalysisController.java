package org.example.atuo_attend_backend.ai.controller;

import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.dto.AiAnalysisConfigUpdate;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.ai.service.AiAnalysisService;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员：AI 分析配置与单条 commit 的 AI 分析查询/触发。
 */
@RestController
@RequestMapping("/api/admin/ai-analysis")
public class AdminAiAnalysisController {

    private final AiAnalysisConfigService configService;
    private final AiAnalysisService analysisService;
    private final CommitService commitService;
    private final AiTokenUsageMapper tokenUsageMapper;

    public AdminAiAnalysisController(AiAnalysisConfigService configService, AiAnalysisService analysisService,
                                     CommitService commitService, AiTokenUsageMapper tokenUsageMapper) {
        this.configService = configService;
        this.analysisService = analysisService;
        this.commitService = commitService;
        this.tokenUsageMapper = tokenUsageMapper;
    }

    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> getTokenUsage(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        days = Math.min(Math.max(days, 1), 365);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Map<String, Object>> items = tokenUsageMapper.listSince(since, 500);
        Map<String, Object> summary = tokenUsageMapper.sumSince(since);
        if (summary == null) summary = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("summary", summary);
        data.put("items", items);
        data.put("since", since.toString());
        return ApiResponse.ok(data);
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        AiAnalysisConfig c = configService.getConfigMasked();
        Map<String, Object> data = new HashMap<>();
        data.put("provider", c.getProvider());
        data.put("apiKeyMasked", c.getApiKey());
        data.put("hasApiKey", c.getApiKey() != null && !c.getApiKey().isEmpty());
        data.put("enabled", Boolean.TRUE.equals(c.getEnabled()));
        data.put("model", c.getModel() != null ? c.getModel() : "deepseek-chat");
        data.put("promptVersion", c.getPromptVersion());
        data.put("maxDiffChars", c.getMaxDiffChars());
        return ApiResponse.ok(data);
    }

    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestBody AiAnalysisConfigUpdate body) {
        configService.updateConfig(
            body.getApiKey(),
            body.getEnabled(),
            body.getModel(),
            body.getPromptVersion(),
            body.getMaxDiffChars()
        );
        return ApiResponse.ok(null);
    }

    /**
     * 单次提交分析看板：返回该 commit 的元数据（不含大 diff）+ 是否有 diff + 已保存的 AI 分析结果。
     */
    @GetMapping("/commits/{commitSha}/detail")
    public ApiResponse<Map<String, Object>> getCommitDetail(@PathVariable String commitSha,
                                                            @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        String repo = repoFullName;
        if (repo == null || repo.isBlank()) {
            Optional<CommitRecord> any = commitService.findAnyCommitBySha(commitSha);
            if (any.isEmpty()) return ApiResponse.error(40000, "缺少 repoFullName 或 commit 不存在");
            repo = any.get().getRepoFullName();
        }
        Optional<CommitRecord> commitOpt = commitService.findCommit(repo, commitSha);
        if (commitOpt.isEmpty()) return ApiResponse.error(40400, "commit 不存在");
        CommitRecord c = commitOpt.get();
        Map<String, Object> commitMeta = new HashMap<>();
        commitMeta.put("repoFullName", c.getRepoFullName());
        commitMeta.put("commitSha", c.getCommitSha());
        commitMeta.put("parentSha", c.getParentSha());
        commitMeta.put("authorName", c.getAuthorName());
        commitMeta.put("authorEmail", c.getAuthorEmail());
        commitMeta.put("committedAt", c.getCommittedAt());
        commitMeta.put("message", c.getMessage());
        commitMeta.put("filesChanged", c.getFilesChanged());
        commitMeta.put("insertions", c.getInsertions());
        commitMeta.put("deletions", c.getDeletions());
        commitMeta.put("validCommit", c.isValidCommit());
        commitMeta.put("validReason", c.getValidReason());
        String diffText = c.getDiffText();
        boolean hasDiff = diffText != null && !diffText.isBlank() && !diffText.startsWith("(Diff 暂不可用");
        commitMeta.put("hasDiff", hasDiff);
        Optional<AiAnalysisResult> resultOpt = analysisService.getResult(repo, commitSha);
        Map<String, Object> data = new HashMap<>();
        data.put("commit", commitMeta);
        data.put("aiResult", resultOpt.map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("workSummary", r.getWorkSummary());
            m.put("workType", r.getWorkType());
            m.put("mainArea", r.getMainArea());
            m.put("isEffective", r.getIsEffective());
            m.put("effectiveReason", r.getEffectiveReason());
            m.put("invalidReasonTag", r.getInvalidReasonTag());
            m.put("qualityLevel", r.getQualityLevel());
            m.put("qualityComment", r.getQualityComment());
            m.put("riskFlags", r.getRiskFlags());
            m.put("suggestions", r.getSuggestions());
            m.put("promptVersion", r.getPromptVersion());
            m.put("createdAt", r.getCreatedAt());
            m.put("rawResponse", r.getRawResponse());
            return m;
        }).orElse(null));
        return ApiResponse.ok(data);
    }

    @GetMapping("/commits/{commitSha}/result")
    public ApiResponse<?> getCommitAnalysis(@PathVariable String commitSha,
                                             @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        String repo = repoFullName;
        if (repo == null || repo.isBlank()) {
            Optional<CommitRecord> any = commitService.findAnyCommitBySha(commitSha);
            if (any.isEmpty()) return ApiResponse.error(40000, "缺少 repoFullName 或 commit 不存在");
            repo = any.get().getRepoFullName();
        }
        Optional<AiAnalysisResult> opt = analysisService.getResult(repo, commitSha);
        if (opt.isEmpty()) return ApiResponse.error(40400, "暂无该 commit 的 AI 分析结果");
        Map<String, Object> data = new HashMap<>();
        AiAnalysisResult r = opt.get();
        data.put("repoFullName", r.getRepoFullName());
        data.put("commitSha", r.getCommitSha());
        data.put("workSummary", r.getWorkSummary());
        data.put("workType", r.getWorkType());
        data.put("mainArea", r.getMainArea());
        data.put("isEffective", r.getIsEffective());
        data.put("effectiveReason", r.getEffectiveReason());
        data.put("invalidReasonTag", r.getInvalidReasonTag());
        data.put("qualityLevel", r.getQualityLevel());
        data.put("qualityComment", r.getQualityComment());
        data.put("riskFlags", r.getRiskFlags());
        data.put("suggestions", r.getSuggestions());
        data.put("promptVersion", r.getPromptVersion());
        data.put("createdAt", r.getCreatedAt());
        return ApiResponse.ok(data);
    }

    @PostMapping("/commits/{commitSha}/run")
    public ApiResponse<?> runCommitAnalysis(@PathVariable String commitSha,
                                            @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        String repo = repoFullName;
        if (repo == null || repo.isBlank()) {
            Optional<CommitRecord> any = commitService.findAnyCommitBySha(commitSha);
            if (any.isEmpty()) return ApiResponse.error(40000, "缺少 repoFullName 或 commit 不存在");
            repo = any.get().getRepoFullName();
        }
        Optional<AiAnalysisResult> opt = analysisService.runAnalysis(repo, commitSha);
        if (opt.isEmpty()) {
            return ApiResponse.error(40000, "无法执行分析：请确认已开启 AI 分析并填写 DeepSeek API Key，且该 commit 已有 Diff。");
        }
        Map<String, Object> data = new HashMap<>();
        AiAnalysisResult r = opt.get();
        data.put("repoFullName", r.getRepoFullName());
        data.put("commitSha", r.getCommitSha());
        data.put("workSummary", r.getWorkSummary());
        data.put("workType", r.getWorkType());
        data.put("mainArea", r.getMainArea());
        data.put("isEffective", r.getIsEffective());
        data.put("effectiveReason", r.getEffectiveReason());
        data.put("qualityLevel", r.getQualityLevel());
        data.put("qualityComment", r.getQualityComment());
        data.put("createdAt", r.getCreatedAt());
        return ApiResponse.ok(data);
    }
}
