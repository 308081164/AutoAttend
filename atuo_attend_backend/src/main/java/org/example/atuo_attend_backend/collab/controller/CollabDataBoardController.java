package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.admin.GithubRepoInfoFetcher;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.service.AiAnalysisService;
import org.example.atuo_attend_backend.ai.service.ProjectDailySummaryService;
import org.example.atuo_attend_backend.collab.auth.CollabAuthFilter;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.commit.mapper.CommitMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.platform.service.PlatformComponentEventService;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 成员（研究员）在项目协作页嵌入「开发与数据看板」时使用的只读数据接口。
 * 在目标项目的租户上下文中复用与 /api/admin 相同的 Commit / 统计 / AI 查询逻辑，避免成员端误调管理员接口产生 401。
 */
@RestController
@RequestMapping("/api/collab/data-board/projects/{projectId}")
public class CollabDataBoardController {

    private final CollabProjectService projectService;
    private final CommitService commitService;
    private final GithubRepoInfoFetcher githubRepoInfoFetcher;
    private final ProjectDailySummaryService projectDailySummaryService;
    private final AiAnalysisService analysisService;
    private final PlatformComponentEventService componentEventService;

    public CollabDataBoardController(CollabProjectService projectService,
                                    CommitService commitService,
                                    GithubRepoInfoFetcher githubRepoInfoFetcher,
                                    ProjectDailySummaryService projectDailySummaryService,
                                    AiAnalysisService analysisService,
                                    PlatformComponentEventService componentEventService) {
        this.projectService = projectService;
        this.commitService = commitService;
        this.githubRepoInfoFetcher = githubRepoInfoFetcher;
        this.projectDailySummaryService = projectDailySummaryService;
        this.analysisService = analysisService;
        this.componentEventService = componentEventService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute(CollabAuthFilter.ATTR_COLLAB_USER_ID);
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    private BizProject requireAccessibleProject(long userId, long projectId, HttpServletRequest req) {
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return null;
        }
        return projectService.getById(projectId);
    }

    private <T> ApiResponse<T> withProjectTenant(long projectId, HttpServletRequest req, java.util.function.Supplier<ApiResponse<T>> action) {
        long userId = requireUserId(req);
        BizProject p = requireAccessibleProject(userId, projectId, req);
        if (p == null) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        if (p.getTenantId() == null) {
            return ApiResponse.error(50000, "项目租户信息缺失");
        }
        return TenantContext.runWithTenantId(p.getTenantId(), action::get);
    }

    @GetMapping("/repos")
    public ApiResponse<Map<String, Object>> listRepos(@PathVariable long projectId, HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            BizProject p = projectService.getById(projectId);
            String repo = p != null && p.getRepoId() != null ? p.getRepoId().trim() : "";
            Map<String, Object> data = new HashMap<>();
            data.put("items", repo.isEmpty() ? List.of() : List.of(repo));
            return ApiResponse.ok(data);
        });
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@PathVariable long projectId,
                                                      @RequestParam(value = "range", required = false) String range,
                                                      @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                                      HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            Map<String, Object> data = new HashMap<>();
            Map<String, Integer> summary = new HashMap<>();
            long totalCommits = (repoFullName != null && !repoFullName.isBlank())
                    ? commitService.countByRepo(repoFullName)
                    : commitService.countAll();
            int activeCoding = totalCommits > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalCommits;
            summary.put("activeCoding", activeCoding);
            summary.put("inReview", 0);
            summary.put("reviewingOthers", 0);
            summary.put("ciFixing", 0);
            summary.put("blocked", 0);
            summary.put("idle", 0);
            data.put("summary", summary);
            data.put("commits", List.of());
            data.put("repoFullName", repoFullName);
            if (repoFullName != null && !repoFullName.isBlank()) {
                List<CommitMapper.AuthorAggregate> authors = commitService.aggregateByAuthor(repoFullName);
                data.put("authors", authors);
            } else {
                data.put("authors", List.of());
            }
            data.put("alerts", List.of());
            data.put("range", range != null ? range : "24h");
            return ApiResponse.ok(data);
        });
    }

    @GetMapping("/commits")
    public ApiResponse<Map<String, Object>> listCommits(@PathVariable long projectId,
                                                         @RequestParam(value = "userId", required = false) Long userId,
                                                         @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                                         @RequestParam(value = "range", required = false) String range,
                                                         @RequestParam(value = "page", defaultValue = "1") int page,
                                                         @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                                                         HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            List<CommitRecord> pageItems;
            long total;
            if (repoFullName != null && !repoFullName.isBlank()) {
                pageItems = commitService.listPagedByRepo(repoFullName, page, pageSize);
                total = commitService.countByRepo(repoFullName);
            } else {
                pageItems = commitService.listPaged(page, pageSize);
                total = commitService.countAll();
            }
            Map<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("total", total);
            data.put("repoFullName", repoFullName);
            data.put("items", pageItems);
            return ApiResponse.ok(data);
        });
    }

    @GetMapping("/commits/{commitSha}/diff")
    public ApiResponse<?> getDiff(@PathVariable long projectId,
                                  @PathVariable String commitSha,
                                  @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                  @RequestParam(value = "mode", required = false, defaultValue = "raw") String mode,
                                  @RequestParam(value = "chunk", required = false, defaultValue = "1") int chunk,
                                  HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            String repo = (repoFullName != null && !repoFullName.isBlank()) ? repoFullName.trim() : null;
            if (repo == null) {
                Optional<CommitRecord> any = commitService.findAnyCommitBySha(commitSha);
                if (any.isEmpty()) return ApiResponse.error(40000, "repoFullName is required or commit not found");
                repo = any.get().getRepoFullName();
            }
            Optional<CommitRecord> recordOpt = commitService.findCommit(repo, commitSha);
            if (recordOpt.isEmpty()) return ApiResponse.error(40400, "commit not found");
            CommitRecord record = recordOpt.get();
            Map<String, Object> data = new HashMap<>();
            data.put("repoFullName", record.getRepoFullName());
            data.put("commitSha", record.getCommitSha());
            data.put("diffText", record.getDiffText());
            data.put("chunk", 1);
            data.put("chunkCount", 1);
            data.put("message", record.getMessage());
            data.put("authorName", record.getAuthorName());
            data.put("authorEmail", record.getAuthorEmail());
            data.put("committedAt", record.getCommittedAt());
            return ApiResponse.ok(data);
        });
    }

    @GetMapping("/stats/overview")
    public ApiResponse<Map<String, Object>> statsOverview(@PathVariable long projectId,
                                                         @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                                         HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            List<String> repos = commitService.listRepos();
            long repoCount = repos.size();
            long totalCommits = repoFullName != null && !repoFullName.isBlank()
                    ? commitService.countByRepo(repoFullName)
                    : commitService.countAll();
            long authorCount = commitService.countDistinctAuthors(repoFullName);
            Map<String, Object> data = new HashMap<>();
            data.put("repoCount", repoCount);
            data.put("totalCommits", totalCommits);
            data.put("authorCount", authorCount);
            return ApiResponse.ok(data);
        });
    }

    @GetMapping("/stats/repo-info")
    public ApiResponse<Map<String, Object>> repoInfo(@PathVariable long projectId,
                                                     @RequestParam("repoFullName") String repoFullName,
                                                     HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            if (repoFullName == null || repoFullName.isBlank()) {
                return ApiResponse.error(40000, "repoFullName required");
            }
            Map<String, Object> info = githubRepoInfoFetcher.fetchRepoInfo(repoFullName.trim());
            return ApiResponse.ok(info != null ? info : new HashMap<>());
        });
    }

    @GetMapping("/stats/commits-by-day")
    public ApiResponse<java.util.List<Map<String, Object>>> commitsByDay(@PathVariable long projectId,
                                                                          @RequestParam(value = "range", defaultValue = "7d") String range,
                                                                          @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                                                          HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            int days = "30d".equalsIgnoreCase(range) ? 30 : 7;
            List<CommitMapper.CommitByDay> list = commitService.listCommitsByDay(days, repoFullName);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            List<Map<String, Object>> data = list.stream().map(d -> {
                Map<String, Object> m = new HashMap<>();
                m.put("date", d.getDay() != null ? fmt.format(d.getDay()) : null);
                m.put("count", d.getCount());
                m.put("insertions", d.getInsertions());
                m.put("deletions", d.getDeletions());
                return m;
            }).collect(Collectors.toList());
            return ApiResponse.ok(data);
        });
    }

    @GetMapping("/stats/authors")
    public ApiResponse<Map<String, Object>> authors(@PathVariable long projectId,
                                                    @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                                    @RequestParam(value = "period", required = false, defaultValue = "total") String period,
                                                    @RequestParam(value = "offset", defaultValue = "0") int offset,
                                                    HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            String p = period != null ? period.trim().toLowerCase() : "total";
            if (!p.equals("week") && !p.equals("month") && !p.equals("year") && !p.equals("total")) {
                return ApiResponse.error(40000, "period 须为 week、month、year 或 total");
            }
            Map<String, Object> data = commitService.aggregateAuthorsByPeriod(repoFullName, p, offset);
            return ApiResponse.ok(data);
        });
    }

    @GetMapping("/ai-analysis/daily-summaries")
    public ApiResponse<Map<String, Object>> listDailySummaries(@PathVariable long projectId,
                                                               @RequestParam("repoFullName") String repoFullName,
                                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                                               @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                               HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            if (repoFullName == null || repoFullName.isBlank()) {
                return ApiResponse.error(40000, "repoFullName 必填");
            }
            return ApiResponse.ok(projectDailySummaryService.listByRepo(repoFullName.trim(), page, pageSize));
        });
    }

    @PostMapping("/ai-analysis/daily-summary/run")
    public ApiResponse<Map<String, Object>> runDailySummary(@PathVariable long projectId,
                                                            @RequestParam(value = "date", required = false) String date,
                                                            @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                                            HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            try {
                LocalDate d;
                if (date != null && !date.isBlank()) {
                    d = LocalDate.parse(date.trim());
                } else {
                    d = LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1);
                }
                int n = projectDailySummaryService.runSummariesForDate(d, repoFullName, false);
                Map<String, Object> data = new HashMap<>();
                data.put("summaryDate", d.toString());
                data.put("reposProcessed", n);
                return ApiResponse.ok(data);
            } catch (Exception e) {
                return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "执行失败");
            }
        });
    }

    @GetMapping("/ai-analysis/daily-summaries/{id}")
    public ApiResponse<Map<String, Object>> getDailySummary(@PathVariable long projectId,
                                                            @PathVariable long id,
                                                            HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> projectDailySummaryService.findById(id)
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
                .orElseGet(() -> ApiResponse.error(40400, "总结不存在")));
    }

    @GetMapping("/ai-analysis/commits/{commitSha}/result")
    public ApiResponse<?> getCommitAnalysis(@PathVariable long projectId,
                                            @PathVariable String commitSha,
                                            @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                            HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            String repo = repoFullName;
            if (repo == null || repo.isBlank()) {
                Optional<CommitRecord> any = commitService.findAnyCommitBySha(commitSha);
                if (any.isEmpty()) return ApiResponse.error(40000, "缺少 repoFullName 或 commit 不存在");
                repo = any.get().getRepoFullName();
            }
            Optional<AiAnalysisResult> opt = analysisService.getResult(repo, commitSha);
            if (opt.isEmpty()) return ApiResponse.error(40400, "暂无该 commit 的 AI 分析结果");
            AiAnalysisResult r = opt.get();
            Map<String, Object> data = new HashMap<>();
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
        });
    }

    @PostMapping("/ai-analysis/commits/{commitSha}/run")
    public ApiResponse<?> runCommitAnalysis(@PathVariable long projectId,
                                          @PathVariable String commitSha,
                                          @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                          HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
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
            AiAnalysisResult r = opt.get();
            Long collabUid = (Long) req.getAttribute(CollabAuthFilter.ATTR_COLLAB_USER_ID);
            String phone = null;
            componentEventService.recordUsage(collabUid, phone, "ai_commit_analysis", "ai_analysis_run");
            Map<String, Object> data = new HashMap<>();
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
        });
    }

    @PostMapping("/ops/events/component-click")
    public ApiResponse<Void> componentClick(@PathVariable long projectId,
                                            @RequestBody Map<String, String> body,
                                            HttpServletRequest req) {
        return withProjectTenant(projectId, req, () -> {
            if (body == null) return ApiResponse.ok(null);
            String key = body.get("componentKey");
            String core = body.get("coreApiKey");
            if (key != null && !key.isBlank()) {
                Long collabUid = (Long) req.getAttribute(CollabAuthFilter.ATTR_COLLAB_USER_ID);
                componentEventService.recordClick(collabUid, null, key.trim(), core != null ? core.trim() : null);
            }
            return ApiResponse.ok(null);
        });
    }
}
