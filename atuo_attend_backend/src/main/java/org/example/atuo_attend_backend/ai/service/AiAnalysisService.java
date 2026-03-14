package org.example.atuo_attend_backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisJob;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisResult;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisJobMapper;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisResultMapper;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
    private final CommitService commitService;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalysisService(AiAnalysisConfigService configService, AiAnalysisJobMapper jobMapper,
                             AiAnalysisResultMapper resultMapper, CommitService commitService, DeepSeekClient deepSeekClient) {
        this.configService = configService;
        this.jobMapper = jobMapper;
        this.resultMapper = resultMapper;
        this.commitService = commitService;
        this.deepSeekClient = deepSeekClient;
    }

    public Optional<AiAnalysisResult> getResult(String repoFullName, String commitSha) {
        AiAnalysisResult r = resultMapper.findByRepoAndSha(repoFullName, commitSha);
        return r != null ? Optional.of(r) : Optional.empty();
    }

    /**
     * 触发一次分析：若已有成功结果则直接返回；否则创建/更新任务并同步执行一次 DeepSeek 调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public Optional<AiAnalysisResult> runAnalysis(String repoFullName, String commitSha) {
        AiAnalysisResult existing = resultMapper.findByRepoAndSha(repoFullName, commitSha);
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
        AiAnalysisJob job = jobMapper.findByRepoAndSha(repoFullName, commitSha);
        if (job == null) {
            job = new AiAnalysisJob();
            job.setRepoFullName(repoFullName);
            job.setCommitSha(commitSha);
            job.setStatus("running");
            job.setModel(config.getModel());
            job.setPromptVersion(config.getPromptVersion());
            job.setRetryCount(0);
            jobMapper.insert(job);
        } else if ("success".equals(job.getStatus())) {
            return Optional.ofNullable(resultMapper.findByRepoAndSha(repoFullName, commitSha));
        } else {
            job.setStatus("running");
            job.setLastError(null);
            jobMapper.update(job);
        }
        String systemPrompt = buildSystemPrompt();
        String userContent = buildUserContent(commit, diffText);
        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
        messages.add(new DeepSeekClient.ChatMessage("user", userContent));
        String response = deepSeekClient.chat(config.getApiKey(), config.getModel(), messages, true);
        if (response == null || response.isBlank()) {
            job.setStatus("failed");
            job.setLastError("DeepSeek API 返回为空或调用失败");
            jobMapper.update(job);
            return Optional.empty();
        }
        AiAnalysisResult result = parseResult(repoFullName, commitSha, config.getPromptVersion(), response);
        if (result == null) {
            job.setStatus("failed");
            job.setLastError("解析 AI 返回 JSON 失败");
            jobMapper.update(job);
            return Optional.empty();
        }
        resultMapper.insert(result);
        job.setStatus("success");
        job.setLastError(null);
        jobMapper.update(job);
        return Optional.of(result);
    }

    private String buildSystemPrompt() {
        return "你是一个代码审查助手。根据用户提供的一次 Git 提交（commit）的元信息与 diff，严格按以下 JSON 结构输出一条分析结果，不要输出任何其他文字或 markdown 标记。\n" +
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
    }

    private String buildUserContent(CommitRecord commit, String diffText) {
        StringBuilder sb = new StringBuilder();
        sb.append("repo_full_name: ").append(commit.getRepoFullName()).append("\n");
        sb.append("commit_sha: ").append(commit.getCommitSha()).append("\n");
        sb.append("author_name: ").append(commit.getAuthorName()).append("\n");
        sb.append("author_email: ").append(commit.getAuthorEmail()).append("\n");
        sb.append("committed_at: ").append(commit.getCommittedAt()).append("\n");
        sb.append("message: ").append(commit.getMessage()).append("\n");
        sb.append("files_changed: ").append(commit.getFilesChanged()).append("\n");
        sb.append("insertions: ").append(commit.getInsertions()).append(", deletions: ").append(commit.getDeletions()).append("\n\n");
        sb.append("--- diff ---\n").append(diffText);
        return sb.toString();
    }

    private AiAnalysisResult parseResult(String repoFullName, String commitSha, String promptVersion, String rawResponse) {
        try {
            String json = rawResponse.trim();
            if (json.startsWith("```")) {
                int start = json.indexOf("{");
                int end = json.lastIndexOf("}");
                if (start >= 0 && end > start) json = json.substring(start, end + 1);
            }
            JsonNode root = objectMapper.readTree(json);
            AiAnalysisResult r = new AiAnalysisResult();
            r.setRepoFullName(repoFullName);
            r.setCommitSha(commitSha);
            r.setPromptVersion(promptVersion);
            r.setRawResponse(rawResponse);
            r.setWorkSummary(getText(root, "work_summary"));
            r.setWorkType(getText(root, "work_type"));
            r.setMainArea(getText(root, "main_area"));
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
}
