package org.example.atuo_attend_backend.ai.controller;

import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.dto.AiAnalysisConfigUpdate;
import org.example.atuo_attend_backend.ai.dto.AiQwenConfigUpdate;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.ai.service.AiAnalysisService;
import org.example.atuo_attend_backend.ai.service.ProjectDailySummaryService;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final ProjectDailySummaryService projectDailySummaryService;

    public AdminAiAnalysisController(AiAnalysisConfigService configService, AiAnalysisService analysisService,
                                     CommitService commitService, AiTokenUsageMapper tokenUsageMapper,
                                     ProjectDailySummaryService projectDailySummaryService) {
        this.configService = configService;
        this.analysisService = analysisService;
        this.commitService = commitService;
        this.tokenUsageMapper = tokenUsageMapper;
        this.projectDailySummaryService = projectDailySummaryService;
    }

    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> getTokenUsage(
            @RequestParam(value = "days", defaultValue = "30") int days,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        days = Math.min(Math.max(days, 1), 365);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<String, Object> summary;
        List<Map<String, Object>> items;
        boolean usePaging = page != null && pageSize != null && page > 0 && pageSize > 0 && pageSize <= 500;
        if (usePaging && provider != null && !provider.isBlank()) {
            String p = provider.trim();
            summary = tokenUsageMapper.sumSinceByProvider(since, p);
            long total = tokenUsageMapper.countSinceByProvider(since, p);
            int offset = (page - 1) * pageSize;
            items = tokenUsageMapper.listSinceByProviderPaged(since, p, offset, pageSize);
            Map<String, Object> data = new HashMap<>();
            if (summary == null) summary = new HashMap<>();
            data.put("summary", summary);
            data.put("items", items);
            data.put("since", since.toString());
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            return ApiResponse.ok(data);
        }
        if (provider != null && !provider.isBlank()) {
            items = tokenUsageMapper.listSinceByProvider(since, provider.trim(), 500);
            summary = tokenUsageMapper.sumSinceByProvider(since, provider.trim());
        } else {
            items = tokenUsageMapper.listSince(since, 500);
            summary = tokenUsageMapper.sumSince(since);
        }
        if (summary == null) summary = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("summary", summary);
        data.put("items", items);
        data.put("since", since.toString());
        return ApiResponse.ok(data);
    }

    @GetMapping("/usage/daily")
    public ApiResponse<Map<String, Object>> getTokenUsageDaily(
            @RequestParam(value = "days", defaultValue = "30") int days,
            @RequestParam(value = "provider") String provider) {
        if (provider == null || provider.isBlank()) {
            return ApiResponse.error(40000, "provider 必填");
        }
        days = Math.min(Math.max(days, 1), 365);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Map<String, Object>> daily = tokenUsageMapper.listDailyByProvider(since, provider.trim());
        Map<String, Object> data = new HashMap<>();
        data.put("daily", daily != null ? daily : List.of());
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
        data.put("dailySummaryEnabled", Boolean.TRUE.equals(c.getDailySummaryEnabled()));
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
            body.getDailySummaryEnabled(),
            body.getModel(),
            body.getPromptVersion(),
            body.getMaxDiffChars()
        );
        return ApiResponse.ok(null);
    }

    /** 分页查询某仓库的历史每日总结 */
    @GetMapping("/daily-summaries")
    public ApiResponse<Map<String, Object>> listDailySummaries(
            @RequestParam("repoFullName") String repoFullName,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        if (repoFullName == null || repoFullName.isBlank()) {
            return ApiResponse.error(40000, "repoFullName 必填");
        }
        return ApiResponse.ok(projectDailySummaryService.listByRepo(repoFullName.trim(), page, pageSize));
    }

    @GetMapping("/daily-summaries/{id}")
    public ApiResponse<Map<String, Object>> getDailySummary(@PathVariable long id) {
        return projectDailySummaryService.findById(id)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("repoFullName", s.getRepoFullName());
                    m.put("summaryDate", s.getSummaryDate() != null ? s.getSummaryDate().toString() : null);
                    m.put("title", s.getTitle());
                    m.put("content", s.getContent());
                    m.put("commitCount", s.getCommitCount());
                    m.put("model", s.getModel());
                    m.put("status", s.getStatus());
                    m.put("errorMessage", s.getErrorMessage());
                    m.put("createdAt", s.getCreatedAt());
                    m.put("updatedAt", s.getUpdatedAt());
                    return ApiResponse.ok(m);
                })
                .orElseGet(() -> ApiResponse.error(40400, "总结不存在"));
    }

    /**
     * 手动触发生成指定业务日的总结（默认昨天）；可限定单个仓库。需已开启 dailySummaryEnabled 且配置 API Key。
     */
    @PostMapping("/daily-summary/run")
    public ApiResponse<Map<String, Object>> runDailySummary(
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        try {
            LocalDate d;
            if (date != null && !date.isBlank()) {
                d = LocalDate.parse(date.trim());
            } else {
                d = LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(1);
            }
            int n = projectDailySummaryService.runSummariesForDate(d, repoFullName, false);
            Map<String, Object> data = new HashMap<>();
            data.put("summaryDate", d.toString());
            data.put("reposProcessed", n);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "执行失败");
        }
    }

    @GetMapping("/qwen-config")
    public ApiResponse<Map<String, Object>> getQwenConfig() {
        AiAnalysisConfig c = configService.getQwenConfigMasked();
        Map<String, Object> data = new HashMap<>();
        data.put("provider", c.getProvider());
        data.put("apiKeyMasked", c.getApiKey());
        data.put("hasApiKey", c.getApiKey() != null && !c.getApiKey().isEmpty());
        data.put("enabled", Boolean.TRUE.equals(c.getEnabled()));
        data.put("model", c.getModel() != null ? c.getModel() : "qwen-vl-plus");
        return ApiResponse.ok(data);
    }

    @PutMapping("/qwen-config")
    public ApiResponse<Void> updateQwenConfig(@RequestBody AiQwenConfigUpdate body) {
        configService.updateQwenConfig(
                body.getApiKey(),
                body.getEnabled(),
                body.getModel()
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
