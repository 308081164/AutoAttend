package org.example.atuo_attend_backend.ai.service;

import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.dto.ProjectDailySummaryListItem;
import org.example.atuo_attend_backend.ai.domain.ProjectDailySummary;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisResultMapper;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.mapper.ProjectDailySummaryMapper;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 按仓库、按业务日汇总提交与单次 AI 分析结果，调用 DeepSeek 生成「项目每日进展总结」并落库。
 */
@Service
public class ProjectDailySummaryService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDailySummaryService.class);
    private static final String PROVIDER_DEEPSEEK = "deepseek";
    private static final int MAX_COMMITS_IN_PROMPT = 100;
    private static final int MAX_USER_PROMPT_CHARS = 100_000;
    private static final int MAX_COMPLETION_TOKENS = 8192;

    private final AiAnalysisConfigService configService;
    private final CommitService commitService;
    private final AiAnalysisResultMapper resultMapper;
    private final DeepSeekClient deepSeekClient;
    private final ProjectDailySummaryMapper summaryMapper;
    private final AiTokenUsageMapper tokenUsageMapper;

    @Value("${app.daily-summary.timezone:Asia/Shanghai}")
    private String timezoneId;

    public ProjectDailySummaryService(AiAnalysisConfigService configService, CommitService commitService,
                                      AiAnalysisResultMapper resultMapper, DeepSeekClient deepSeekClient,
                                      ProjectDailySummaryMapper summaryMapper, AiTokenUsageMapper tokenUsageMapper) {
        this.configService = configService;
        this.commitService = commitService;
        this.resultMapper = resultMapper;
        this.deepSeekClient = deepSeekClient;
        this.summaryMapper = summaryMapper;
        this.tokenUsageMapper = tokenUsageMapper;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(timezoneId != null ? timezoneId : "Asia/Shanghai");
        } catch (Exception e) {
            return ZoneId.of("Asia/Shanghai");
        }
    }

    /**
     * 对指定业务日、可选限定仓库，生成/覆盖每日总结。返回成功生成的仓库数量。
     *
     * @param requireFeatureToggle true 时须开启「每日进展总结」开关（用于定时任务）；false 时仅校验 API Key（用于管理端手动触发）
     */
    public int runSummariesForDate(LocalDate summaryDate, String onlyRepoFullName, boolean requireFeatureToggle) {
        AiAnalysisConfig cfg = configService.getConfig();
        if (requireFeatureToggle && !Boolean.TRUE.equals(cfg.getDailySummaryEnabled())) {
            log.info("Daily summary skipped: daily_summary_enabled is false");
            return 0;
        }
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            log.warn("Daily summary skipped: no DeepSeek API key");
            return 0;
        }
        OffsetDateTime start = summaryDate.atStartOfDay(zone()).toOffsetDateTime();
        OffsetDateTime end = summaryDate.plusDays(1).atStartOfDay(zone()).toOffsetDateTime();

        List<String> repos;
        if (onlyRepoFullName != null && !onlyRepoFullName.isBlank()) {
            String r = onlyRepoFullName.trim();
            repos = commitService.listCommitsByRepoBetween(r, start, end).isEmpty() ? List.of() : List.of(r);
        } else {
            repos = commitService.listReposWithCommitsBetween(start, end);
        }
        int ok = 0;
        for (String repo : repos) {
            try {
                generateAndPersist(repo, summaryDate, start, end, cfg);
                ok++;
            } catch (Exception e) {
                log.warn("Daily summary failed for {} @ {}: {}", repo, summaryDate, e.getMessage());
                try {
                    summaryMapper.upsert(tid(), repo, summaryDate, null,
                            "（本日总结生成失败，请检查日志与 DeepSeek 配额。）\n\n`" + truncate(e.getMessage(), 500) + "`",
                            0, cfg.getModel(), "failed", truncate(e.getMessage(), 2000));
                } catch (Exception ignore) {
                    log.debug("upsert failed row also failed", ignore);
                }
            }
        }
        return ok;
    }

    /** 定时任务：总结「昨天」（按配置的时区自然日）。 */
    public int runSummariesForYesterday() {
        LocalDate y = LocalDate.now(zone()).minusDays(1);
        return runSummariesForDate(y, null, true);
    }

    private void generateAndPersist(String repo, LocalDate summaryDate,
                                    OffsetDateTime start, OffsetDateTime end, AiAnalysisConfig cfg) {
        List<CommitRecord> commits = commitService.listCommitsByRepoBetween(repo, start, end);
        if (commits.isEmpty()) {
            return;
        }

        String userPayload = buildUserPayload(repo, summaryDate, commits);
        String system = """
                你是资深研发项目经理与技术负责人，正在为管理员撰写一份「项目每日进展总结」。
                请严格根据用户提供的当日提交记录与（如有）单次提交 AI 分析摘要进行归纳，不要编造未出现的仓库或提交。
                输出使用 **Markdown**，结构清晰，建议包含以下板块（可根据数据多少取舍，但信息要尽量全面）：
                1. **概要**：一句话概括本日该仓库的整体进展与节奏。
                2. **功能与需求**：正在开发或已交付的功能点、模块（可从提交说明与 AI 摘要推断）。
                3. **实现与变更**：重要实现、重构、Bug 修复、配置/文档等（结合 work_type 与提交说明）。
                4. **工作量小计**：提交次数、涉及人数、整体增删行规模（可列表）。
                5. **成员贡献**：按作者的提交分布、主要工作方向（客观描述，避免褒贬过度）。
                6. **质量与风险**（若有 AI 质量/风险字段）：简要归纳。
                7. **待关注项**：信息不足处、需管理员跟进的问题（若无则写「无」）。
                8. **小结**：3～6 条要点，便于管理层快速阅读。

                语气专业、简洁，面向管理员，使其仅阅读本文即可掌握昨日该项目研发动态。
                """;

        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", system),
                new DeepSeekClient.ChatMessage("user", userPayload)
        );
        DeepSeekClient.ChatResult chatResult = deepSeekClient.chatWithUsage(
                cfg.getApiKey(), cfg.getModel(), messages, false, MAX_COMPLETION_TOKENS);
        if (chatResult == null || chatResult.getContent() == null || chatResult.getContent().isBlank()) {
            throw new IllegalStateException("DeepSeek 未返回有效内容");
        }
        String content = chatResult.getContent().trim();
        String title = extractTitle(content, repo, summaryDate);
        summaryMapper.upsert(tid(), repo, summaryDate, title, content, commits.size(), cfg.getModel(), "success", null);
        recordTokenUsage(chatResult, repo, summaryDate);
        log.info("Daily summary saved: {} @ {} ({} commits)", repo, summaryDate, commits.size());
    }

    private void recordTokenUsage(DeepSeekClient.ChatResult chatResult, String repo, LocalDate summaryDate) {
        if (chatResult == null || tokenUsageMapper == null) return;
        try {
            int total = chatResult.getInputTokens() + chatResult.getOutputTokens();
            String pseudoSha = "daily_summary:" + summaryDate;
            tokenUsageMapper.insert(tid(), LocalDateTime.now(), PROVIDER_DEEPSEEK, chatResult.getModel(),
                    chatResult.getInputTokens(), chatResult.getOutputTokens(), total, repo, pseudoSha);
        } catch (Exception e) {
            log.warn("Record daily summary token usage failed: {}", e.getMessage());
        }
    }

    private String buildUserPayload(String repo, LocalDate summaryDate, List<CommitRecord> commits) {
        StringBuilder sb = new StringBuilder();
        sb.append("业务日：").append(summaryDate).append("（时区：").append(zone().getId()).append("）\n");
        sb.append("GitHub 仓库：").append(repo).append("\n");
        sb.append("当日提交次数：").append(commits.size()).append("\n\n");

        Map<String, AuthorAgg> byKey = new LinkedHashMap<>();
        for (CommitRecord c : commits) {
            String key = (c.getAuthorEmail() != null && !c.getAuthorEmail().isBlank())
                    ? c.getAuthorEmail().trim()
                    : "unknown:" + Objects.toString(c.getAuthorName(), "");
            byKey.computeIfAbsent(key, k -> new AuthorAgg(c.getAuthorName(), c.getAuthorEmail())).add(c);
        }
        sb.append("【按作者汇总】\n");
        for (AuthorAgg a : byKey.values()) {
            sb.append("- ").append(Objects.toString(a.authorName, "-"))
                    .append(" <").append(Objects.toString(a.authorEmail, "")).append(">：提交 ")
                    .append(a.commitCount).append(" 次；合计 +").append(a.insertions).append(" / -").append(a.deletions)
                    .append(" 行；涉及文件变更累计 ").append(a.filesChanged).append(" 个文件（按提交字段累加）\n");
        }
        sb.append("\n【逐条提交（含单次 AI 分析摘要，若有）】\n");

        int n = 0;
        for (CommitRecord c : commits) {
            n++;
            if (n > MAX_COMMITS_IN_PROMPT) {
                sb.append("\n... 另有 ").append(commits.size() - MAX_COMMITS_IN_PROMPT)
                        .append(" 条提交未逐条列出，请在总结中结合总数与作者汇总表整体描述。\n");
                break;
            }
            String sha = c.getCommitSha() != null ? c.getCommitSha() : "";
            String shortSha = sha.length() >= 7 ? sha.substring(0, 7) : sha;
            sb.append("\n").append(n).append(". ").append(shortSha)
                    .append(" | ").append(Objects.toString(c.getAuthorName(), "-"))
                    .append(" | ").append(c.getCommittedAt()).append("\n");
            sb.append("   说明：").append(truncate(Objects.toString(c.getMessage(), ""), 500)).append("\n");
            sb.append("   规模：文件 ").append(c.getFilesChanged()).append("，+").append(c.getInsertions())
                    .append(" -").append(c.getDeletions()).append("\n");

            AiAnalysisResult ar = resultMapper.findByRepoAndSha(tid(), repo, sha);
            if (ar != null) {
                sb.append("   [AI分析] ");
                sb.append("摘要:").append(truncate(Objects.toString(ar.getWorkSummary(), ""), 200));
                sb.append(" | 类型:").append(Objects.toString(ar.getWorkType(), "-"));
                sb.append(" | 模块:").append(truncate(Objects.toString(ar.getMainArea(), ""), 120));
                sb.append(" | 有效性:").append(Objects.toString(ar.getIsEffective(), "-"));
                sb.append(" | 质量:").append(Objects.toString(ar.getQualityLevel(), "-"));
                if (ar.getQualityComment() != null && !ar.getQualityComment().isBlank()) {
                    sb.append(" | 质量说明:").append(truncate(ar.getQualityComment(), 200));
                }
                sb.append("\n");
            } else {
                sb.append("   [AI分析] 暂无记录\n");
            }
        }

        String out = sb.toString();
        if (out.length() > MAX_USER_PROMPT_CHARS) {
            return out.substring(0, MAX_USER_PROMPT_CHARS) + "\n\n...(输入已截断，请在总结中说明数据截断情况)\n";
        }
        return out;
    }

    private static String extractTitle(String markdown, String repo, LocalDate date) {
        if (markdown == null) return repo + " " + date;
        for (String line : markdown.split("\n")) {
            String t = line.trim();
            if (t.startsWith("# ")) return truncate(t.substring(2).trim(), 200);
            if (t.startsWith("## ")) return truncate(t.substring(3).trim(), 200);
        }
        return truncate(repo + " · " + date + " 进展总结", 200);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    public Optional<ProjectDailySummary> findById(long id) {
        ProjectDailySummary s = summaryMapper.findById(tid(), id);
        return Optional.ofNullable(s);
    }

    public Map<String, Object> listByRepo(String repoFullName, int page, int pageSize) {
        if (repoFullName == null || repoFullName.isBlank()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("items", List.of());
            empty.put("total", 0L);
            empty.put("page", page);
            empty.put("pageSize", pageSize);
            return empty;
        }
        page = Math.max(1, page);
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (page - 1) * pageSize;
        long total = summaryMapper.countByRepo(tid(), repoFullName.trim());
        List<ProjectDailySummaryListItem> items = summaryMapper.listByRepoPaged(tid(), repoFullName.trim(), offset, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    private static final class AuthorAgg {
        final String authorName;
        final String authorEmail;
        int commitCount;
        int insertions;
        int deletions;
        int filesChanged;

        AuthorAgg(String authorName, String authorEmail) {
            this.authorName = authorName;
            this.authorEmail = authorEmail;
        }

        void add(CommitRecord c) {
            commitCount++;
            insertions += Math.max(0, c.getInsertions());
            deletions += Math.max(0, c.getDeletions());
            filesChanged += Math.max(0, c.getFilesChanged());
        }
    }
}
