package org.example.atuo_attend_backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.domain.ProjectAiLinkageConfig;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisJob;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisJobMapper;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisResultMapper;
import org.example.atuo_attend_backend.ai.mapper.ProjectAiLinkageConfigMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final AiTokenUsageMapper tokenUsageMapper;
    private final CommitService commitService;
    private final DeepSeekClient deepSeekClient;
    private final BizProjectMapper projectMapper;
    private final CollabTableService collabTableService;
    private final CollabRecordService collabRecordService;
    private final ProjectAiLinkageConfigMapper projectAiLinkageConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalysisService(AiAnalysisConfigService configService, AiAnalysisJobMapper jobMapper,
                             AiAnalysisResultMapper resultMapper, AiTokenUsageMapper tokenUsageMapper,
                             CommitService commitService, DeepSeekClient deepSeekClient,
                             BizProjectMapper projectMapper, CollabTableService collabTableService,
                             CollabRecordService collabRecordService,
                             ProjectAiLinkageConfigMapper projectAiLinkageConfigMapper) {
        this.configService = configService;
        this.jobMapper = jobMapper;
        this.resultMapper = resultMapper;
        this.tokenUsageMapper = tokenUsageMapper;
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

    private void recordTokenUsage(DeepSeekClient.ChatResult chatResult, String repoFullName, String commitSha) {
        if (chatResult == null || tokenUsageMapper == null) return;
        try {
            int total = chatResult.getInputTokens() + chatResult.getOutputTokens();
            tokenUsageMapper.insert(tid(), LocalDateTime.now(), PROVIDER_DEEPSEEK, chatResult.getModel(),
                chatResult.getInputTokens(), chatResult.getOutputTokens(), total, repoFullName, commitSha);
        } catch (Exception e) {
            log.warn("Record token usage failed: {}", e.getMessage());
        }
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
        if (!Boolean.TRUE.equals(config.getEnabled()) || config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.debug("AI analysis disabled or no API key");
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
        BizProject linkedProject = projectMapper.findByTenantAndRepoId(tid(), repoFullName);
        ProjectAiLinkageConfig linkageCfg = linkedProject != null
                ? projectAiLinkageConfigMapper.findByProjectId(linkedProject.getId())
                : null;
        boolean linkageEnabled = linkageCfg != null && Boolean.TRUE.equals(linkageCfg.getEnabled());
        List<Map<String, Object>> collabCandidates = linkageEnabled && linkedProject != null
                ? buildCollabCandidates(linkedProject.getId())
                : List.of();

        String systemPrompt = buildSystemPrompt(linkageEnabled);
        String userContent = buildUserContent(commit, diffText, collabCandidates);
        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
        messages.add(new DeepSeekClient.ChatMessage("user", userContent));
        DeepSeekClient.ChatResult chatResult = deepSeekClient.chatWithUsage(config.getApiKey(), config.getModel(), messages, true);
        if (chatResult == null || chatResult.getContent() == null || chatResult.getContent().isBlank()) {
            job.setStatus("failed");
            job.setLastError("DeepSeek API 返回为空或调用失败");
            jobMapper.update(job);
            return Optional.empty();
        }
        recordTokenUsage(chatResult, repoFullName, commitSha);
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
        if (linkedProject != null && linkageEnabled) {
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
        return base + "\n\n若输入中存在 collab_records，请额外输出 related_records 数组，格式：\n" +
                "\"related_records\": [\n" +
                "  {\"record_id\": 123, \"confidence\": \"high|medium|low\", \"resolve_status\": \"建议解决情况\", \"accept_status\": \"建议验收结果\", \"comment\": \"理由\"}\n" +
                "]\n" +
                "只有在你明确判断与该记录有关时才输出该项。";
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
        sb.append("insertions: ").append(commit.getInsertions()).append(", deletions: ").append(commit.getDeletions()).append("\n\n");
        if (collabCandidates != null && !collabCandidates.isEmpty()) {
            sb.append("collab_records(json): ");
            try {
                sb.append(objectMapper.writeValueAsString(collabCandidates));
            } catch (Exception e) {
                sb.append("[]");
            }
            sb.append("\n\n");
        }
        sb.append("--- diff ---\n").append(diffText);
        return sb.toString();
    }

    private List<Map<String, Object>> buildCollabCandidates(long projectId) {
        List<Map<String, Object>> out = new ArrayList<>();
        BizProjectTable table = collabTableService.getTableByProjectIdAndPurpose(projectId, "issue_tracking");
        if (table == null) return out;
        Map<String, Object> twc = collabTableService.getTableWithColumns(projectId, "issue_tracking");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cols = twc != null ? (List<Map<String, Object>>) twc.get("columns") : List.of();
        long problemCol = findColId(cols, "问题描述");
        long moduleCol = findColId(cols, "归属模块");
        long resolveCol = findColId(cols, "解决情况");
        long acceptCol = findColId(cols, "验收结果");
        List<Map<String, Object>> rows = collabRecordService.listRecords(table.getId(), 1, 30);
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("record_id", r.get("id"));
            m.put("problem_summary", snippet(asString(r.get("c" + problemCol)), 200));
            m.put("module", asString(r.get("c" + moduleCol)));
            m.put("resolve_status", asString(r.get("c" + resolveCol)));
            m.put("accept_status", asString(r.get("c" + acceptCol)));
            out.add(m);
        }
        return out;
    }

    private void applyLinkageSuggestion(long projectId, ProjectAiLinkageConfig cfg, String rawResponse) {
        if (cfg == null || !"auto".equalsIgnoreCase(cfg.getMode())) return;
        try {
            JsonNode root = objectMapper.readTree(extractJson(rawResponse));
            JsonNode arr = root.get("related_records");
            if (arr == null || !arr.isArray()) return;
            Map<String, Object> table = collabTableService.getTableWithColumns(projectId, "issue_tracking");
            if (table == null) return;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cols = (List<Map<String, Object>>) table.get("columns");
            long resolveCol = findColId(cols, "解决情况");
            long acceptCol = findColId(cols, "验收结果");
            if (resolveCol <= 0 && acceptCol <= 0) return;
            for (JsonNode n : arr) {
                long recordId = n.path("record_id").asLong(0);
                if (recordId <= 0) continue;
                if (collabRecordService.getProjectIdByRecordId(recordId) != projectId) continue;
                if (!confidencePass(n.path("confidence").asText("medium"), cfg.getMinConfidence())) continue;
                Map<String, Object> fields = new HashMap<>();
                String resolveStatus = n.path("resolve_status").asText(null);
                String acceptStatus = n.path("accept_status").asText(null);
                if (resolveCol > 0 && resolveStatus != null && !resolveStatus.isBlank()) fields.put("c" + resolveCol, resolveStatus.trim());
                if (acceptCol > 0 && acceptStatus != null && !acceptStatus.isBlank()) fields.put("c" + acceptCol, acceptStatus.trim());
                if (!fields.isEmpty()) {
                    collabRecordService.updateRecordWithAudit(recordId, fields, null, "commit_ai_linkage");
                }
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
