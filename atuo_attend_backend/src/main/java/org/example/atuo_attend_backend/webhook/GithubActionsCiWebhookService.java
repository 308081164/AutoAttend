package org.example.atuo_attend_backend.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.collab.CollabTablePurpose;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.domain.BizTableColumn;
import org.example.atuo_attend_backend.collab.mapper.BizProjectTableMapper;
import org.example.atuo_attend_backend.collab.mapper.BizTableColumnMapper;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabSyncService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.webhook.mapper.WebhookDeliveryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * GitHub Actions {@code workflow_run} 失败时，写入协作「项目调整」表（{@link CollabTablePurpose#ISSUE_TRACKING}）。
 */
@Service
public class GithubActionsCiWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GithubActionsCiWebhookService.class);

    private static final Set<String> SKIP_CONCLUSIONS = Set.of("success", "skipped", "neutral");

    private final CollabSyncService collabSyncService;
    private final CollabRecordService collabRecordService;
    private final BizProjectTableMapper tableMapper;
    private final BizTableColumnMapper columnMapper;
    private final WebhookDeliveryMapper webhookDeliveryMapper;
    private final DeepSeekClient deepSeekClient;
    private final AiAnalysisConfigService aiAnalysisConfigService;

    private final boolean aiSummaryEnabled;

    public GithubActionsCiWebhookService(CollabSyncService collabSyncService,
                                         CollabRecordService collabRecordService,
                                         BizProjectTableMapper tableMapper,
                                         BizTableColumnMapper columnMapper,
                                         WebhookDeliveryMapper webhookDeliveryMapper,
                                         DeepSeekClient deepSeekClient,
                                         AiAnalysisConfigService aiAnalysisConfigService,
                                         @Value("${app.github.actions-ci.ai-summary-enabled:false}") boolean aiSummaryEnabled) {
        this.collabSyncService = collabSyncService;
        this.collabRecordService = collabRecordService;
        this.tableMapper = tableMapper;
        this.columnMapper = columnMapper;
        this.webhookDeliveryMapper = webhookDeliveryMapper;
        this.deepSeekClient = deepSeekClient;
        this.aiAnalysisConfigService = aiAnalysisConfigService;
        this.aiSummaryEnabled = aiSummaryEnabled;
    }

    /**
     * 处理 {@code workflow_run} + {@code action=completed}；仅在结论为非 success 等时落库。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleWorkflowRunCompleted(long tenantId, String deliveryId, JsonNode root) {
        if (root == null || root.isNull()) {
            return;
        }
        String action = text(root, "action");
        if (!"completed".equals(action)) {
            return;
        }
        JsonNode wr = root.get("workflow_run");
        if (wr == null || wr.isNull()) {
            return;
        }
        String conclusion = text(wr, "conclusion").toLowerCase(Locale.ROOT);
        if (conclusion.isEmpty() || SKIP_CONCLUSIONS.contains(conclusion)) {
            return;
        }

        JsonNode repoNode = root.get("repository");
        String repoFullName = text(repoNode, "full_name");
        if (repoFullName.isBlank()) {
            log.warn("[github-actions-ci] missing repository.full_name");
            return;
        }

        if (deliveryId != null && !deliveryId.isBlank()) {
            int n = webhookDeliveryMapper.insertIgnore(deliveryId, "workflow_run");
            if (n == 0) {
                log.info("[github-actions-ci] duplicate delivery_id={}, skip", deliveryId);
                return;
            }
        }

        TenantContext.setTenantId(tenantId);
        try {
            BizProject project = collabSyncService.ensureProjectAndTable(repoFullName);
            if (project == null) {
                log.warn("[github-actions-ci] could not ensure project for repo={}", repoFullName);
                return;
            }
            BizProjectTable table = tableMapper.findByProjectIdAndPurpose(project.getId(), CollabTablePurpose.ISSUE_TRACKING);
            if (table == null) {
                log.warn("[github-actions-ci] issue_tracking table missing projectId={}", project.getId());
                return;
            }
            List<BizTableColumn> columns = columnMapper.listByTableId(table.getId());
            Map<String, Long> colIdByName = new HashMap<>();
            for (BizTableColumn c : columns) {
                if (c.getName() != null) {
                    colIdByName.put(c.getName().trim(), c.getId());
                }
            }

            String baseDesc = buildIssueDescription(repoFullName, root.get("workflow"), wr, conclusion);
            String fullDesc = baseDesc;
            if (aiSummaryEnabled) {
                String aiLine = tryAiOneLineSummary(baseDesc);
                if (aiLine != null && !aiLine.isBlank()) {
                    fullDesc = baseDesc + "\n\n【AI 摘要】" + aiLine.trim();
                }
            }

            Map<String, Object> fields = new LinkedHashMap<>();
            putField(fields, colIdByName, "问题描述", fullDesc);
            putField(fields, colIdByName, "当前状态", "修复中");
            putField(fields, colIdByName, "重要程度", "严重紧急");
            putField(fields, colIdByName, "创建人", "GitHub Actions");
            putField(fields, colIdByName, "验收结果", "未验收");
            putField(fields, colIdByName, "创建时间", LocalDateTime.now().toString());

            collabRecordService.createRecord(table.getId(), null, fields);
            log.info("[github-actions-ci] recorded workflow failure repo={} conclusion={} tableId={}",
                    repoFullName, conclusion, table.getId());
        } finally {
            TenantContext.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
        }
    }

    private static void putField(Map<String, Object> fields, Map<String, Long> colIdByName, String colName, Object value) {
        Long id = colIdByName.get(colName);
        if (id == null || value == null) {
            return;
        }
        fields.put("c" + id, value);
    }

    private String buildIssueDescription(String repoFullName, JsonNode workflow, JsonNode wr, String conclusion) {
        String wfName = workflow != null && !workflow.isNull() ? text(workflow, "name") : "";
        String runName = text(wr, "name");
        String displayTitle = text(wr, "display_title");
        String headBranch = text(wr, "head_branch");
        String headSha = text(wr, "head_sha");
        String htmlUrl = text(wr, "html_url");
        String runNumber = text(wr, "run_number");
        StringBuilder sb = new StringBuilder();
        sb.append("【CI/CD 非成功】仓库 ").append(repoFullName).append("\n");
        if (!wfName.isBlank()) {
            sb.append("工作流：").append(wfName).append("\n");
        }
        if (!runName.isBlank()) {
            sb.append("运行名称：").append(runName).append("\n");
        }
        if (!displayTitle.isBlank() && !displayTitle.equals(runName)) {
            sb.append("标题：").append(displayTitle).append("\n");
        }
        sb.append("结论：").append(conclusion).append("\n");
        if (!headBranch.isBlank()) {
            sb.append("分支：").append(headBranch).append("\n");
        }
        if (!headSha.isBlank()) {
            sb.append("SHA：").append(headSha).append("\n");
        }
        if (!runNumber.isBlank()) {
            sb.append("运行号：#").append(runNumber).append("\n");
        }
        if (!htmlUrl.isBlank()) {
            sb.append("运行页：").append(htmlUrl).append("\n");
        }
        sb.append("\n说明：请打开运行页查看失败 Job 与日志。常见原因包括单测/构建失败、依赖安装超时、密钥或权限配置错误等。");
        return sb.toString();
    }

    private String tryAiOneLineSummary(String userContent) {
        try {
            AiAnalysisConfig cfg = aiAnalysisConfigService.getConfig();
            if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
                return null;
            }
            List<DeepSeekClient.ChatMessage> messages = List.of(
                    new DeepSeekClient.ChatMessage("system",
                            "你是 DevOps 助手。根据用户提供的 GitHub Actions 失败元信息，用 1～3 句中文概括最可能的原因与下一步排查方向。不要编造日志中未出现的事实。"),
                    new DeepSeekClient.ChatMessage("user", userContent)
            );
            String model = cfg.getModel() != null ? cfg.getModel() : "deepseek-chat";
            DeepSeekClient.ChatResult r = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, false, 512);
            return r != null ? r.getContent() : null;
        } catch (Exception e) {
            log.warn("[github-actions-ci] AI summary failed: {}", e.getMessage());
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return "";
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return "";
        }
        if (v.isTextual()) {
            return v.asText("");
        }
        if (v.isNumber()) {
            return v.asText();
        }
        return v.toString();
    }
}
