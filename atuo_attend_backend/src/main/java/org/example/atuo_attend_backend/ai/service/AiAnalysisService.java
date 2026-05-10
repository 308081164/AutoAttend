package org.example.atuo_attend_backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.domain.ProjectAiLinkageConfig;
import org.example.atuo_attend_backend.ai.official.OfficialAiPoolService;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisJob;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisJobMapper;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisResultMapper;
import org.example.atuo_attend_backend.ai.mapper.ProjectAiLinkageConfigMapper;
import org.example.atuo_attend_backend.collab.CollabTablePurpose;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 单次提交 AI 分析：根据设计文档构建 prompt、调用 DeepSeek、解析结果落库。
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final String PROVIDER_DEEPSEEK = "deepseek";
    private static final int DEFAULT_MAX_DIFF_CHARS = 100_000;

    private final AiAnalysisConfigService configService;
    private final AiAnalysisJobMapper jobMapper;
    private final AiAnalysisResultMapper resultMapper;
    private final OfficialAiPoolService officialAiPoolService;
    private final CommitService commitService;
    private final DeepSeekClient deepSeekClient;
    private final BizProjectMapper projectMapper;
    private final CollabTableService collabTableService;
    private final CollabRecordService collabRecordService;
    private final ProjectAiLinkageConfigMapper projectAiLinkageConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalysisService(AiAnalysisConfigService configService, AiAnalysisJobMapper jobMapper,
                             AiAnalysisResultMapper resultMapper,
                             OfficialAiPoolService officialAiPoolService,
                             CommitService commitService, DeepSeekClient deepSeekClient,
                             BizProjectMapper projectMapper, CollabTableService collabTableService,
                             CollabRecordService collabRecordService,
                             ProjectAiLinkageConfigMapper projectAiLinkageConfigMapper) {
        this.configService = configService;
        this.jobMapper = jobMapper;
        this.resultMapper = resultMapper;
        this.officialAiPoolService = officialAiPoolService;
        this.commitService = commitService;
        this.deepSeekClient = deepSeekClient;
        this.projectMapper = projectMapper;
        this.collabTableService = collabTableService;
        this.collabRecordService = collabRecordService;
        this.projectAiLinkageConfigMapper = projectAiLinkageConfigMapper;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public Optional<AiAnalysisResult> getResult(String repoFullName, String commitSha) {
        AiAnalysisResult r = resultMapper.findByRepoAndSha(tid(), repoFullName, commitSha);
        return r != null ? Optional.of(r) : Optional.empty();
    }

    /**
     * 触发一次分析：若已有成功结果则直接返回；否则创建/更新任务并同步执行一次 DeepSeek 调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public Optional<AiAnalysisResult> runAnalysis(String repoFullName, String commitSha) {
        AiAnalysisResult existing = resultMapper.findByRepoAndSha(tid(), repoFullName, commitSha);
        if (existing != null) {
            return Optional.of(existing);
        }
        AiAnalysisConfig config = configService.getConfig();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            log.debug("AI analysis disabled");
            return Optional.empty();
        }
        Optional<CommitRecord> commitOpt = commitService.findCommit(repoFullName, commitSha);
        if (commitOpt.isEmpty()) {
            return Optional.empty();
        }
        CommitRecord commit = commitOpt.get();
        String diffText = commit.getDiffText();
        if (diffText == null || diffText.isBlank() || diffText.startsWith("(Diff 暂不可用")) {
            log.debug("No diff available for {}@{}", repoFullName, commitSha);
            return Optional.empty();
        }
        int maxChars = config.getMaxDiffChars() != null ? config.getMaxDiffChars() : DEFAULT_MAX_DIFF_CHARS;
        if (diffText.length() > maxChars) {
            diffText = diffText.substring(0, maxChars) + "\n\n... (diff 已截断)";
        }
        BizProject linkedProject = projectMapper.findByTenantAndRepoId(tid(), repoFullName);
        ProjectAiLinkageConfig linkageCfg = linkedProject != null
                ? projectAiLinkageConfigMapper.findByProjectId(linkedProject.getId())
                : null;
        String automationMode = normalizeProjectAutomationMode(linkageCfg);
        if (ProjectAiLinkageConfig.AUTOMATION_DISABLED.equals(automationMode)) {
            log.debug("Commit AI skipped: project automation disabled, repo={}", repoFullName);
            return Optional.empty();
        }

        AiAnalysisJob job = jobMapper.findByRepoAndSha(tid(), repoFullName, commitSha);
        if (job == null) {
            job = new AiAnalysisJob();
            job.setTenantId(tid());
            job.setRepoFullName(repoFullName);
            job.setCommitSha(commitSha);
            job.setStatus("running");
            job.setModel(config.getModel());
            job.setPromptVersion(config.getPromptVersion());
            job.setRetryCount(0);
            jobMapper.insert(job);
        } else if ("success".equals(job.getStatus())) {
            return Optional.ofNullable(resultMapper.findByRepoAndSha(tid(), repoFullName, commitSha));
        } else {
            job.setStatus("running");
            job.setLastError(null);
            jobMapper.update(job);
        }

        boolean injectCollab = linkedProject != null
                && ProjectAiLinkageConfig.AUTOMATION_AUTO_STATUS.equals(automationMode);
        List<Map<String, Object>> collabCandidates = injectCollab
                ? buildCollabCandidates(linkedProject.getId())
                : List.of();

        String systemPrompt = buildSystemPrompt(injectCollab);
        String userContent = buildUserContent(commit, diffText, collabCandidates);
        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
        messages.add(new DeepSeekClient.ChatMessage("user", userContent));
        OfficialAiPoolService.DeepSeekChatOutcome chatOut = officialAiPoolService.chatDeepSeek(
                deepSeekClient, tid(), config.getApiKey(), config.getModel(), messages, true, null);
        DeepSeekClient.ChatResult chatResult = chatOut != null ? chatOut.result() : null;
        if (chatResult == null || chatResult.getContent() == null || chatResult.getContent().isBlank()) {
            job.setStatus("failed");
            job.setLastError("DeepSeek API 返回为空或调用失败");
            jobMapper.update(job);
            return Optional.empty();
        }
        officialAiPoolService.recordDeepSeekUsage(tid(), chatResult, chatOut.officialPool(), repoFullName, commitSha);
        String response = chatResult.getContent();
        AiAnalysisResult result = parseResult(repoFullName, commitSha, config.getPromptVersion(), response);
        if (result == null) {
            job.setStatus("failed");
            job.setLastError("解析 AI 返回 JSON 失败");
            jobMapper.update(job);
            return Optional.empty();
        }
        result.setTenantId(tid());
        resultMapper.insert(result);
        if (linkedProject != null && injectCollab) {
            applyLinkageSuggestion(linkedProject.getId(), linkageCfg, response);
        }
        job.setStatus("success");
        job.setLastError(null);
        jobMapper.update(job);
        return Optional.of(result);
    }

    private String buildSystemPrompt(boolean includeCollabLinkage) {
        String base = "你是一个代码审查助手。根据用户提供的一次 Git 提交（commit）的元信息与 diff，严格按以下 JSON 结构输出一条分析结果，不要输出任何其他文字或 markdown 标记。\n" +
                "JSON 结构：\n" +
                "{\n" +
                "  \"work_summary\": \"简短中文摘要，一句或几句说明本 commit 主要在做什么\",\n" +
                "  \"work_type\": \"feature|bugfix|refactor|format_only|doc_only|config_only|other\",\n" +
                "  \"main_area\": \"主要涉及模块或文件路径摘要，可选\",\n" +
                "  \"is_effective\": \"effective|weak_effective|ineffective\",\n" +
                "  \"effective_reason\": \"有效性理由说明\",\n" +
                "  \"invalid_reason_tag\": \"仅当无效时可填：only_whitespace|only_comment|only_lockfile|no_logic_change 等，可选\",\n" +
                "  \"quality_level\": \"high|medium|low\",\n" +
                "  \"quality_comment\": \"代码质量简短评语与主要问题\",\n" +
                "  \"risk_flags\": [\"可选风险标签如 magic_number, long_method 等\"],\n" +
                "  \"suggestions\": [\"改进建议1\", \"改进建议2\"]\n" +
                "}";
        if (!includeCollabLinkage) return base;
        return base + "\n\n若输入中存在 collab_records（每条含 table_kind、summary、module、status_current 等），请判断 commit 与记录的关联并输出 related_records 数组。\n" +
                "\n" +
                "## collab_records 中 table_kind 含义\n" +
                "- issue_tracking：项目调整表；status_current 对应列「当前状态」；accept_status_context 为「验收结果」当前值（**只作上下文，禁止在输出中建议修改**）。\n" +
                "- feature_backlog：待开发功能清单；status_current 对应列「开发进度」。\n" +
                "\n" +
                "## 关联判断标准（按优先级从高到低）\n" +
                "1. **文件路径匹配**：files_changed_list 与「归属模块」或 summary（问题描述/功能描述）中提到的路径或模块一致或明显相关。\n" +
                "2. **关键词匹配**：commit message 或 diff 中的标识符出现在 summary 中。\n" +
                "3. **功能语义匹配**：diff 与 summary 所描述功能语义相关。\n" +
                "\n" +
                "## suggested_status 填写规则（系统**仅**可能自动写入该状态列；**绝不会**自动写入「验收结果」）\n" +
                "- table_kind=issue_tracking 时：suggested_status 为写入「当前状态」的值，须与该任务当前可选状态语义一致（如：已创建、开发中、修复中、待测试、测试中、已验收等，以输入为准）。\n" +
                "- table_kind=feature_backlog 时：suggested_status 为写入「开发进度」的值（如：待开发、开发中、联调中、测试中、已完成、阻塞等，以输入为准）。\n" +
                "- 不相关则不要输出该 record。\n" +
                "\n" +
                "## 输出格式（related_records 内**禁止**出现 accept_status 或任何验收相关写入字段）\n" +
                "\"related_records\": [\n" +
                "  {\"record_id\": 123, \"table_kind\": \"issue_tracking|feature_backlog\", \"confidence\": \"high|medium|low\", \"suggested_status\": \"建议写入状态列的取值\", \"match_reason\": \"简要匹配理由\"}\n" +
                "]\n" +
                "为兼容旧客户端，若缺少 suggested_status 可回退读取 resolve_status 作为同一含义字段。\n" +
                "注意：宁可漏判也不要误判。只有在你有较高把握时才输出 related_records。";
    }

    private String buildUserContent(CommitRecord commit, String diffText, List<Map<String, Object>> collabCandidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("repo_full_name: ").append(commit.getRepoFullName()).append("\n");
        sb.append("commit_sha: ").append(commit.getCommitSha()).append("\n");
        sb.append("author_name: ").append(commit.getAuthorName()).append("\n");
        sb.append("author_email: ").append(commit.getAuthorEmail()).append("\n");
        sb.append("committed_at: ").append(commit.getCommittedAt()).append("\n");
        sb.append("message: ").append(commit.getMessage()).append("\n");
        sb.append("files_changed: ").append(commit.getFilesChanged()).append("\n");
        sb.append("insertions: ").append(commit.getInsertions()).append(", deletions: ").append(commit.getDeletions()).append("\n");

        // 提取文件路径列表，帮助 AI 做路径匹配（从 diffText 中解析）
        String rawDiff = commit.getDiffText();
        if (rawDiff != null && !rawDiff.isBlank() && !rawDiff.startsWith("(Diff 暂不可用")) {
            java.util.Set<String> fileSet = new java.util.LinkedHashSet<>();
            // git diff 格式: diff --git a/path b/path
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^diff --git\\s+a/(\\S+)\\s+b/\\S+", java.util.regex.Pattern.MULTILINE);
            java.util.regex.Matcher m = p.matcher(rawDiff);
            while (m.find()) {
                fileSet.add(m.group(1));
            }
            if (!fileSet.isEmpty()) {
                sb.append("\nfiles_changed_list:\n");
                for (String f : fileSet) {
                    sb.append("  - ").append(f).append("\n");
                }
            }
        }

        if (collabCandidates != null && !collabCandidates.isEmpty()) {
            sb.append("\ncollab_records(json):\n");
            try {
                sb.append(objectMapper.writeValueAsString(collabCandidates));
            } catch (Exception e) {
                sb.append("[]");
            }
            sb.append("\n");
        }
        sb.append("\n--- diff ---\n").append(diffText);
        return sb.toString();
    }

    private static final int LINKAGE_CANDIDATE_FETCH = 80;
    private static final int LINKAGE_CANDIDATE_MAX_PER_PURPOSE = 40;

    private static String normalizeProjectAutomationMode(ProjectAiLinkageConfig cfg) {
        if (cfg == null || cfg.getAutomationMode() == null || cfg.getAutomationMode().isBlank()) {
            return ProjectAiLinkageConfig.AUTOMATION_ANALYSIS_ONLY;
        }
        String m = cfg.getAutomationMode().trim().toLowerCase(Locale.ROOT);
        if (ProjectAiLinkageConfig.AUTOMATION_DISABLED.equals(m)) {
            return ProjectAiLinkageConfig.AUTOMATION_DISABLED;
        }
        if (ProjectAiLinkageConfig.AUTOMATION_AUTO_STATUS.equals(m)) {
            return ProjectAiLinkageConfig.AUTOMATION_AUTO_STATUS;
        }
        return ProjectAiLinkageConfig.AUTOMATION_ANALYSIS_ONLY;
    }

    private List<Map<String, Object>> buildCollabCandidates(long projectId) {
        List<Map<String, Object>> out = new ArrayList<>(buildIssueTrackingCandidates(projectId));
        out.addAll(buildFeatureBacklogCandidates(projectId));
        return out;
    }

    private List<Map<String, Object>> buildIssueTrackingCandidates(long projectId) {
        BizProjectTable table = collabTableService.getTableByProjectIdAndPurpose(projectId, CollabTablePurpose.ISSUE_TRACKING);
        if (table == null) return List.of();
        Map<String, Object> twc = collabTableService.getTableWithColumns(projectId, CollabTablePurpose.ISSUE_TRACKING);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cols = twc != null ? (List<Map<String, Object>>) twc.get("columns") : List.of();
        long problemCol = findColId(cols, "问题描述");
        long moduleCol = findColId(cols, "归属模块");
        long statusCol = findColId(cols, "当前状态");
        long acceptCol = findColId(cols, "验收结果");
        long importantCol = findColId(cols, "重要程度");
        long assigneeCol = findColId(cols, "负责人");
        List<Map<String, Object>> rows = collabRecordService.listRecords(table.getId(), 1, LINKAGE_CANDIDATE_FETCH);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            if (out.size() >= LINKAGE_CANDIDATE_MAX_PER_PURPOSE) break;
            String curStatus = asString(r.get("c" + statusCol));
            String accept = asString(r.get("c" + acceptCol));
            if (isIssueRowClosedForLinkage(curStatus, accept)) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("table_kind", CollabTablePurpose.ISSUE_TRACKING);
            m.put("record_id", r.get("id"));
            m.put("summary", snippet(asString(r.get("c" + problemCol)), 300));
            m.put("module", asString(r.get("c" + moduleCol)));
            m.put("status_current", curStatus);
            m.put("important_level", asString(r.get("c" + importantCol)));
            m.put("assignee", asString(r.get("c" + assigneeCol)));
            m.put("accept_status_context", accept);
            m.put("created_at", r.get("createdAt"));
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> buildFeatureBacklogCandidates(long projectId) {
        BizProjectTable table = collabTableService.getTableByProjectIdAndPurpose(projectId, CollabTablePurpose.FEATURE_BACKLOG);
        if (table == null) return List.of();
        Map<String, Object> twc = collabTableService.getTableWithColumns(projectId, CollabTablePurpose.FEATURE_BACKLOG);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cols = twc != null ? (List<Map<String, Object>>) twc.get("columns") : List.of();
        long nameCol = findColId(cols, "功能名称");
        long descCol = findColId(cols, "功能描述");
        long moduleCol = findColId(cols, "归属模块");
        long progressCol = findColId(cols, "开发进度");
        long importantCol = findColId(cols, "重要程度");
        long assigneeCol = findColId(cols, "负责人");
        List<Map<String, Object>> rows = collabRecordService.listRecords(table.getId(), 1, LINKAGE_CANDIDATE_FETCH);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            if (out.size() >= LINKAGE_CANDIDATE_MAX_PER_PURPOSE) break;
            String progress = asString(r.get("c" + progressCol));
            if (isFeatureRowClosedForLinkage(progress)) continue;
            String title = asString(r.get("c" + nameCol));
            String desc = asString(r.get("c" + descCol));
            String combined = ((title != null ? title : "") + " " + (desc != null ? desc : "")).trim();
            Map<String, Object> m = new HashMap<>();
            m.put("table_kind", CollabTablePurpose.FEATURE_BACKLOG);
            m.put("record_id", r.get("id"));
            m.put("summary", snippet(combined.isEmpty() ? null : combined, 300));
            m.put("module", asString(r.get("c" + moduleCol)));
            m.put("status_current", progress);
            m.put("important_level", asString(r.get("c" + importantCol)));
            m.put("assignee", asString(r.get("c" + assigneeCol)));
            m.put("created_at", r.get("createdAt"));
            out.add(m);
        }
        return out;
    }

    private static boolean isIssueRowClosedForLinkage(String currentStatus, String acceptResult) {
        String s = currentStatus != null ? currentStatus.trim() : "";
        String a = acceptResult != null ? acceptResult.trim() : "";
        if ("已验收".equals(s)) return true;
        return "通过任务关闭".equals(a);
    }

    private static boolean isFeatureRowClosedForLinkage(String devProgress) {
        String p = devProgress != null ? devProgress.trim() : "";
        return "已完成".equals(p);
    }

    private void applyLinkageSuggestion(long projectId, ProjectAiLinkageConfig cfg, String rawResponse) {
        if (cfg == null || !ProjectAiLinkageConfig.AUTOMATION_AUTO_STATUS.equalsIgnoreCase(cfg.getAutomationMode())) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJson(rawResponse));
            JsonNode arr = root.get("related_records");
            if (arr == null || !arr.isArray()) return;

            Map<String, Object> issueTwc = collabTableService.getTableWithColumns(projectId, CollabTablePurpose.ISSUE_TRACKING);
            Map<String, Object> featTwc = collabTableService.getTableWithColumns(projectId, CollabTablePurpose.FEATURE_BACKLOG);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issueCols = issueTwc != null ? (List<Map<String, Object>>) issueTwc.get("columns") : List.of();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> featCols = featTwc != null ? (List<Map<String, Object>>) featTwc.get("columns") : List.of();
            long issueStatusColId = findColId(issueCols, "当前状态");
            long featureProgressColId = findColId(featCols, "开发进度");
            if (issueStatusColId <= 0 && featureProgressColId <= 0) return;

            for (JsonNode n : arr) {
                long recordId = n.path("record_id").asLong(0);
                if (recordId <= 0) continue;
                if (collabRecordService.getProjectIdByRecordId(recordId) != projectId) continue;
                if (!confidencePass(n.path("confidence").asText("medium"), cfg.getMinConfidence())) continue;

                String suggestedStatus = n.path("suggested_status").asText(null);
                if (suggestedStatus == null || suggestedStatus.isBlank()) {
                    suggestedStatus = n.path("resolve_status").asText(null);
                }
                if (suggestedStatus == null || suggestedStatus.isBlank()) continue;

                String tableKind = n.path("table_kind").asText(null);
                if (tableKind != null) {
                    tableKind = tableKind.trim();
                }
                if (tableKind == null || tableKind.isBlank()) {
                    tableKind = collabRecordService.getTablePurposeForRecord(recordId);
                }
                long targetCol = -1;
                if (CollabTablePurpose.ISSUE_TRACKING.equals(tableKind)) {
                    targetCol = issueStatusColId;
                } else if (CollabTablePurpose.FEATURE_BACKLOG.equals(tableKind)) {
                    targetCol = featureProgressColId;
                }
                if (targetCol <= 0) continue;

                Map<String, Object> fields = new HashMap<>();
                fields.put("c" + targetCol, suggestedStatus.trim());
                collabRecordService.updateRecordWithAudit(recordId, fields, null, "commit_ai_linkage");
            }
        } catch (Exception e) {
            log.warn("Apply commit linkage suggestion failed: {}", e.getMessage());
        }
    }

    private boolean confidencePass(String actual, String threshold) {
        int a = confLevel(actual);
        int t = confLevel(threshold);
        return a >= t;
    }

    private int confLevel(String s) {
        if (s == null) return 2;
        String t = s.trim().toLowerCase();
        if ("high".equals(t)) return 3;
        if ("medium".equals(t)) return 2;
        return 1;
    }

    private long findColId(List<Map<String, Object>> cols, String name) {
        if (cols == null) return -1;
        for (Map<String, Object> c : cols) {
            if (c == null) continue;
            if (name.equals(asString(c.get("name")))) {
                Object id = c.get("id");
                if (id instanceof Number n) return n.longValue();
                try { return Long.parseLong(String.valueOf(id)); } catch (Exception e) { return -1; }
            }
        }
        return -1;
    }

    private String extractJson(String raw) {
        String json = raw == null ? "" : raw.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}");
            if (start >= 0 && end > start) return json.substring(start, end + 1);
        }
        return json;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String snippet(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    private AiAnalysisResult parseResult(String repoFullName, String commitSha, String promptVersion, String rawResponse) {
        try {
            String json = extractJson(rawResponse);
            JsonNode root = objectMapper.readTree(json);
            AiAnalysisResult r = new AiAnalysisResult();
            r.setRepoFullName(repoFullName);
            r.setCommitSha(commitSha);
            r.setPromptVersion(promptVersion);
            r.setRawResponse(rawResponse);
            r.setWorkSummary(getText(root, "work_summary"));
            r.setWorkType(getText(root, "work_type"));
            r.setMainArea(truncate(getText(root, "main_area"), 255));
            r.setIsEffective(getText(root, "is_effective"));
            r.setEffectiveReason(getText(root, "effective_reason"));
            r.setInvalidReasonTag(getText(root, "invalid_reason_tag"));
            r.setQualityLevel(getText(root, "quality_level"));
            r.setQualityComment(getText(root, "quality_comment"));
            if (root.has("risk_flags") && root.get("risk_flags").isArray()) {
                r.setRiskFlags(objectMapper.writeValueAsString(root.get("risk_flags")));
            }
            if (root.has("suggestions") && root.get("suggestions").isArray()) {
                r.setSuggestions(objectMapper.writeValueAsString(root.get("suggestions")));
            }
            return r;
        } catch (Exception e) {
            log.warn("Parse AI result failed: {}", e.getMessage());
            return null;
        }
    }

    private static String getText(JsonNode node, String key) {
        if (!node.has(key)) return null;
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? null : v.asText(null);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (maxLen <= 0) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }
}
