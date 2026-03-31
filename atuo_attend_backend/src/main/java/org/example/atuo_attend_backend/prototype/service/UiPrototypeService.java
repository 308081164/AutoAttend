package org.example.atuo_attend_backend.prototype.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeGenerateJob;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeProject;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeSpec;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectListItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectDetail;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeSpecItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateJobStatus;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeSpecGenerateResult;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeGenerateJobMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeProjectMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeSpecMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class UiPrototypeService {

    private static final Logger log = LoggerFactory.getLogger(UiPrototypeService.class);

    private final UiPrototypeProjectMapper projectMapper;
    private final UiPrototypeSpecMapper specMapper;
    private final UiPrototypeGenerateJobMapper generateJobMapper;
    private final AiAnalysisConfigService configService;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UiPrototypeSpecValidator validator = new UiPrototypeSpecValidator();

    public UiPrototypeService(UiPrototypeProjectMapper projectMapper,
                              UiPrototypeSpecMapper specMapper,
                              UiPrototypeGenerateJobMapper generateJobMapper,
                              AiAnalysisConfigService configService,
                              DeepSeekClient deepSeekClient) {
        this.projectMapper = projectMapper;
        this.specMapper = specMapper;
        this.generateJobMapper = generateJobMapper;
        this.configService = configService;
        this.deepSeekClient = deepSeekClient;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public List<UiPrototypeProjectListItem> listProjects() {
        long tenantId = tid();
        List<UiPrototypeProject> list = projectMapper.list(tenantId);
        List<UiPrototypeProjectListItem> out = new ArrayList<>();
        for (UiPrototypeProject p : list) {
            UiPrototypeProjectListItem i = new UiPrototypeProjectListItem();
            i.setId(p.getId());
            i.setName(p.getName());
            i.setCurrentSpecVersion(p.getCurrentSpecVersion());
            i.setCreatedAt(p.getCreatedAt());
            i.setUpdatedAt(p.getUpdatedAt());
            out.add(i);
        }
        return out;
    }

    public long createProject(String name) {
        String n = name != null ? name.trim() : "";
        if (n.isEmpty()) throw new IllegalArgumentException("项目名称不能为空");
        UiPrototypeProject p = new UiPrototypeProject();
        p.setTenantId(tid());
        p.setName(n);
        p.setCurrentSpecVersion(null);
        projectMapper.insert(p);
        return p.getId();
    }

    public void renameProject(long id, String name) {
        String n = name != null ? name.trim() : "";
        if (n.isEmpty()) throw new IllegalArgumentException("项目名称不能为空");
        long tenantId = tid();
        int updated = projectMapper.updateName(tenantId, id, n);
        if (updated <= 0) throw new IllegalArgumentException("项目不存在");
    }

    public void deleteProject(long id) {
        long tenantId = tid();
        // 删除项目时同步删除该项目下的 spec 版本，避免产生孤儿数据
        specMapper.deleteByProject(tenantId, id);
        int deleted = projectMapper.deleteById(tenantId, id);
        if (deleted <= 0) throw new IllegalArgumentException("项目不存在");
    }

    public UiPrototypeProjectDetail getProjectDetail(long id) {
        long tenantId = tid();
        UiPrototypeProject p = projectMapper.findById(tenantId, id);
        if (p == null) return null;

        List<UiPrototypeSpec> specs = specMapper.listSpecs(tenantId, id);
        List<UiPrototypeSpecItem> specItems = new ArrayList<>();
        for (UiPrototypeSpec s : specs) {
            UiPrototypeSpecItem item = new UiPrototypeSpecItem();
            item.setId(s.getId());
            item.setVersion(s.getVersion());
            item.setSpecJson(s.getSpecJson());
            item.setCreatedAt(s.getCreatedAt());
            item.setUpdatedAt(s.getUpdatedAt());
            specItems.add(item);
        }

        UiPrototypeProjectDetail d = new UiPrototypeProjectDetail();
        d.setId(p.getId());
        d.setName(p.getName());
        d.setCurrentSpecVersion(p.getCurrentSpecVersion());
        d.setSpecs(specItems);
        return d;
    }

    /**
     * 入队异步生成：HTTP 立即返回 jobId，避免 nginx 同步等待 LLM 超时（504）。
     */
    public long enqueueGenerateSpec(long projectId, String prompt) {
        validateGeneratePreconditions(projectId, prompt);
        long tenantId = tid();
        UiPrototypeGenerateJob row = new UiPrototypeGenerateJob();
        row.setTenantId(tenantId);
        row.setProjectId(projectId);
        row.setStatus("pending");
        row.setPromptSnapshot(promptSnapshot(prompt));
        generateJobMapper.insert(row);
        Long jobId = row.getId();
        if (jobId == null) {
            throw new IllegalStateException("创建生成任务失败");
        }
        CompletableFuture.runAsync(() -> runGenerateJobSafe(tenantId, jobId, projectId, prompt));
        return jobId;
    }

    public UiPrototypeGenerateJobStatus getGenerateJobStatus(long projectId, long jobId) {
        long tenantId = tid();
        UiPrototypeGenerateJob j = generateJobMapper.findById(jobId, tenantId, projectId);
        if (j == null) return null;
        UiPrototypeGenerateJobStatus s = new UiPrototypeGenerateJobStatus();
        s.setJobId(j.getId());
        s.setStatus(j.getStatus());
        s.setSpecVersion(j.getSpecVersion());
        s.setErrorMessage(j.getErrorMessage());
        return s;
    }

    private void validateGeneratePreconditions(long projectId, String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) throw new IllegalArgumentException("prompt 不能为空");
        long tenantId = tid();
        UiPrototypeProject p = projectMapper.findById(tenantId, projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        AiAnalysisConfig cfg = configService.getConfig();
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled()) || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("AI 未启用或未配置 DeepSeek API Key");
        }
    }

    private static String promptSnapshot(String prompt) {
        String t = prompt != null ? prompt.trim() : "";
        if (t.length() > 2000) return t.substring(0, 2000);
        return t.isEmpty() ? null : t;
    }

    private void runGenerateJobSafe(long tenantId, long jobId, long projectId, String prompt) {
        try {
            TenantContext.runWithTenantId(tenantId, () -> runGenerateJob(jobId, projectId, prompt));
        } catch (Throwable t) {
            log.error("ui prototype generate job {} crashed", jobId, t);
            try {
                TenantContext.runWithTenantId(tenantId, () -> {
                    String msg = t.getMessage() != null ? t.getMessage() : "内部错误";
                    generateJobMapper.updateFailed(jobId, tenantId, projectId, msg);
                });
            } catch (Exception e) {
                log.warn("update job failed status error: {}", e.getMessage());
            }
        }
    }

    private void runGenerateJob(long jobId, long projectId, String prompt) {
        long tenantId = tid();
        generateJobMapper.updateStatus(jobId, tenantId, projectId, "running");
        try {
            UiPrototypeSpecGenerateResult r = generateSpec(projectId, prompt);
            generateJobMapper.updateSuccess(jobId, tenantId, projectId, r.getSpecVersion());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "生成失败";
            generateJobMapper.updateFailed(jobId, tenantId, projectId, msg);
        }
    }

    /** 同步生成并落库（供异步任务调用；请求线程勿直接调用以免 504）。 */
    public UiPrototypeSpecGenerateResult generateSpec(long projectId, String prompt) {
        validateGeneratePreconditions(projectId, prompt);

        AiAnalysisConfig cfg = configService.getConfig(); // deepseek config
        String apiKey = cfg.getApiKey();
        String model = cfg.getModel() != null ? cfg.getModel() : "deepseek-chat";

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(prompt);

        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", systemPrompt),
                new DeepSeekClient.ChatMessage("user", userPrompt)
        );

        int maxAttempts = 2;
        String lastErr = null;
        JsonNode validatedSpec = null;
        String rawContent = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            DeepSeekClient.ChatResult res = deepSeekClient.chatWithUsage(apiKey, model, messages, true);
            rawContent = res != null ? res.getContent() : null;
            if (rawContent == null || rawContent.isBlank()) {
                lastErr = "AI 返回为空";
                continue;
            }
            try {
                String jsonText = extractJson(rawContent);
                JsonNode specNode = objectMapper.readTree(jsonText);
                validator.validate(specNode);
                validatedSpec = specNode;
                break;
            } catch (Exception e) {
                lastErr = e.getMessage() != null ? e.getMessage() : e.toString();
                // retry：把失败原因回填给模型，要求只修复 JSON
                String repairUser = userPrompt + "\n\n你需要修复：\n" + lastErr + "\n请仅输出修复后的 JSON object。";
                messages = List.of(
                        new DeepSeekClient.ChatMessage("system", systemPrompt),
                        new DeepSeekClient.ChatMessage("user", repairUser)
                );
            }
        }

        if (validatedSpec == null) {
            throw new IllegalStateException("生成 spec 失败：" + (lastErr != null ? lastErr : "未知错误"));
        }

        // 存储
        long tenantId = tid();
        int nextVersion = specMapper.maxVersion(tenantId, projectId) + 1;
        String specJson = validatedSpec.toString();
        specMapper.insert(tenantId, projectId, nextVersion, specJson);
        projectMapper.updateCurrentSpecVersion(tenantId, projectId, nextVersion);

        UiPrototypeSpecGenerateResult out = new UiPrototypeSpecGenerateResult();
        out.setSpecVersion(nextVersion);
        out.setSpecJson(specJson);
        out.setRawAiContent(rawContent);
        return out;
    }

    private String buildSystemPrompt() {
        return ""
                + "你是一名资深前端设计师与可用性工程师。\n"
                + "你的任务是根据用户需求生成“UI 原型 Spec JSON”，用于平台前端的受控渲染器渲染。\n"
                + "强约束：只输出严格 JSON（JSON object），禁止输出任何解释文字、Markdown、代码块。\n"
                + "\n"
                + "Spec 结构必须包含：meta, theme, tokens, layout, nodes, interactions。\n"
                + "layout.root 必须引用 nodes 中真实存在的节点 id。\n"
                + "\n"
                + "允许的 node.type：Page, Container, Card, Text, Button, Badge, Tabs, Panel, Grid。\n"
                + "\n"
                + "节点通用字段：type、props(可选 object)、style(可选 object)、children(可选 string[]).\n"
                + "Text 节点 props.text 必填。\n"
                + "Button 节点 props.label 必填。\n"
                + "Badge 节点 props.text 必填。\n"
                + "Tabs 节点 props.tabItems 必填且为非空 array，每项必须包含 { key, label, contentId }。\n"
                + "Panel 节点 props.defaultOpen 可选 boolean。\n"
                + "Grid 节点 props.columns 可选 int（用于控制列数）。\n"
                + "\n"
                + "style tokens 仅允许这些值（不允许自由 CSS）：\n"
                + "- padding: space-4|space-8|space-12|space-16|space-24\n"
                + "- radius: r-8|r-10|r-12\n"
                + "- bg/border: primary|primary-strong|text|text-muted|bg|border-muted|success|info|warn\n"
                + "- shadow: shadow-soft|shadow-none\n"
                + "\n"
                + "interactions 是数组，允许的 interaction.type：togglePanel, setTab。\n"
                + "togglePanel：target 必须是 Panel，params.open 必须是 boolean。\n"
                + "setTab：target 必须是 Tabs，params.tabKey 必须是 Tabs.props.tabItems 中存在的 key。\n"
                + "\n"
                + "输出示例（不需要完全一致，只要结构正确）：\n"
                + "{\n"
                + "  \"meta\": {\"version\":1,\"name\":\"string\",\"viewport\":[\"desktop\",\"mobile\"]},\n"
                + "  \"theme\": {\"primaryColorToken\":\"primary\"},\n"
                + "  \"tokens\": {\"spacingScale\":\"px4\",\"radiusScale\":\"8pxSystem\"},\n"
                + "  \"layout\": {\"root\":\"page-1\"},\n"
                + "  \"nodes\": {\n"
                + "    \"page-1\": {\"type\":\"Page\",\"props\":{},\"style\":{},\"children\":[\"grid-1\"]},\n"
                + "    \"grid-1\": {\"type\":\"Grid\",\"props\":{\"columns\":2},\"style\":{},\"children\":[\"card-a\",\"card-b\"]},\n"
                + "    \"card-a\": {\"type\":\"Card\",\"props\":{},\"style\":{},\"children\":[\"text-a\"]},\n"
                + "    \"text-a\": {\"type\":\"Text\",\"props\":{\"text\":\"你好\"},\"style\":{},\"children\":[]}\n"
                + "  },\n"
                + "  \"interactions\": []\n"
                + "}\n";
    }

    private String buildUserPrompt(String prompt) {
        return ""
                + "用户需求：\n"
                + prompt.trim() + "\n\n"
                + "请把页面做成：\n"
                + "- 受控可渲染的结构（必须用你允许的节点类型）\n"
                + "- MVP 交互：只实现点击态与切换面板/Tabs\n"
                + "- 不要使用任何图片与外部资源\n"
                + "- 文案简洁\n";
    }

    private String extractJson(String content) {
        String s = content.trim();
        if (s.startsWith("```")) {
            // 去除代码块外层
            int first = s.indexOf('{');
            int last = s.lastIndexOf('}');
            if (first >= 0 && last > first) return s.substring(first, last + 1);
        }
        int first = s.indexOf('{');
        int last = s.lastIndexOf('}');
        if (first < 0 || last <= first) throw new IllegalArgumentException("无法从 AI 文本中提取 JSON object");
        return s.substring(first, last + 1);
    }
}

