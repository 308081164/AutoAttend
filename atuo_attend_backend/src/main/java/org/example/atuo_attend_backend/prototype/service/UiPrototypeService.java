package org.example.atuo_attend_backend.prototype.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeGenerateJob;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockup;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockupGenerateJob;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeProject;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeSpec;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectListItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectDetail;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeSpecItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateJobStatus;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeSpecGenerateResult;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeGenerateJobMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeMockupGenerateJobMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeMockupMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeProjectMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeSpecMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UiPrototypeService {

    private static final Logger log = LoggerFactory.getLogger(UiPrototypeService.class);

    private final UiPrototypeProjectMapper projectMapper;
    private final UiPrototypeSpecMapper specMapper;
    private final UiPrototypeGenerateJobMapper generateJobMapper;
    private final UiPrototypeMockupMapper mockupMapper;
    private final UiPrototypeMockupGenerateJobMapper mockupGenerateJobMapper;
    private final AiAnalysisConfigService configService;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UiPrototypeSpecValidator validator = new UiPrototypeSpecValidator();
    private final UiPrototypeSemanticValidator semanticValidator = new UiPrototypeSemanticValidator();

    public UiPrototypeService(UiPrototypeProjectMapper projectMapper,
                              UiPrototypeSpecMapper specMapper,
                              UiPrototypeGenerateJobMapper generateJobMapper,
                              UiPrototypeMockupMapper mockupMapper,
                              UiPrototypeMockupGenerateJobMapper mockupGenerateJobMapper,
                              AiAnalysisConfigService configService,
                              DeepSeekClient deepSeekClient) {
        this.projectMapper = projectMapper;
        this.specMapper = specMapper;
        this.generateJobMapper = generateJobMapper;
        this.mockupMapper = mockupMapper;
        this.mockupGenerateJobMapper = mockupGenerateJobMapper;
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
        // HTML+CSS mockup：删除当前产物与任务
        try {
            mockupMapper.deleteByProjectId(tenantId, id);
        } catch (Exception ignore) { /* non-fatal */ }
        try {
            mockupGenerateJobMapper.deleteByProjectId(tenantId, id);
        } catch (Exception ignore) { /* non-fatal */ }
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

    // =========================
    // HTML+CSS Mockup 模式（按 mvp-vue 的解析/修复方式实现；不做版本控制）
    // =========================

    public UiPrototypeMockup getMockup(long projectId) {
        long tenantId = tid();
        return mockupMapper.findByProjectId(tenantId, projectId);
    }

    public void saveMockupMessages(long projectId, String messagesJson) {
        long tenantId = tid();
        UiPrototypeMockup existing = mockupMapper.findByProjectId(tenantId, projectId);
        if (existing == null) {
            UiPrototypeMockup row = new UiPrototypeMockup();
            row.setTenantId(tenantId);
            row.setProjectId(projectId);
            row.setHtml("");
            row.setCss("");
            row.setMessagesJson(messagesJson);
            row.setModelUsed(null);
            row.setRawAiContent(null);
            row.setRepaired(false);
            mockupMapper.insert(row);
        } else {
            UiPrototypeMockup row = new UiPrototypeMockup();
            row.setTenantId(tenantId);
            row.setProjectId(projectId);
            row.setHtml(existing.getHtml() != null ? existing.getHtml() : "");
            row.setCss(existing.getCss() != null ? existing.getCss() : "");
            row.setRawAiContent(existing.getRawAiContent());
            row.setModelUsed(existing.getModelUsed());
            row.setRepaired(existing.isRepaired());
            row.setMessagesJson(messagesJson);
            mockupMapper.updateByProjectId(row);
        }
    }

    public long enqueueGenerateMockup(long projectId, String prompt, String model, String messagesJson) {
        validateGeneratePreconditions(projectId, prompt);
        long tenantId = tid();
        UiPrototypeMockupGenerateJob row = new UiPrototypeMockupGenerateJob();
        row.setTenantId(tenantId);
        row.setProjectId(projectId);
        row.setStatus("pending");
        row.setPromptSnapshot(promptSnapshot(prompt));
        row.setModel(model != null && !model.isBlank() ? model.trim() : null);
        mockupGenerateJobMapper.insert(row);
        Long jobId = row.getId();
        if (jobId == null) {
            throw new IllegalStateException("创建生成任务失败");
        }
        CompletableFuture.runAsync(() -> runGenerateMockupJobSafe(tenantId, jobId, projectId, prompt, row.getModel(), messagesJson));
        return jobId;
    }

    public UiPrototypeMockupGenerateJob getMockupGenerateJobStatus(long projectId, long jobId) {
        long tenantId = tid();
        return mockupGenerateJobMapper.findById(jobId, tenantId, projectId);
    }

    private void runGenerateMockupJobSafe(long tenantId, long jobId, long projectId, String prompt, String model, String messagesJson) {
        try {
            TenantContext.runWithTenantId(tenantId, () -> runGenerateMockupJob(jobId, projectId, prompt, model, messagesJson));
        } catch (Throwable t) {
            log.error("ui prototype mockup generate job {} crashed", jobId, t);
            try {
                TenantContext.runWithTenantId(tenantId, () -> {
                    String msg = t.getMessage() != null ? t.getMessage() : "内部错误";
                    mockupGenerateJobMapper.updateFailed(jobId, tenantId, projectId, msg);
                });
            } catch (Exception e) {
                log.warn("update mockup job failed status error: {}", e.getMessage());
            }
        }
    }

    private void runGenerateMockupJob(long jobId, long projectId, String prompt, String model, String messagesJson) {
        long tenantId = tid();
        mockupGenerateJobMapper.updateStatus(jobId, tenantId, projectId, "running");
        try {
            MockupGenerateResult r = generateMockupWithRepair(prompt, model);
            UiPrototypeMockup existing = mockupMapper.findByProjectId(tenantId, projectId);
            UiPrototypeMockup row = existing != null ? existing : new UiPrototypeMockup();
            row.setTenantId(tenantId);
            row.setProjectId(projectId);
            row.setHtml(r.html != null ? r.html : "");
            row.setCss(r.css != null ? r.css : "");
            row.setRawAiContent(r.rawContent);
            row.setMessagesJson(messagesJson);
            row.setModelUsed(r.modelUsed);
            row.setRepaired(r.repaired);
            if (existing == null) mockupMapper.insert(row);
            else mockupMapper.updateByProjectId(row);

            mockupGenerateJobMapper.updateSuccess(jobId, tenantId, projectId);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "生成失败";
            mockupGenerateJobMapper.updateFailed(jobId, tenantId, projectId, msg);
        }
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
        // 用于入库展示/回溯；生成仍使用完整 prompt。这里适当放宽，避免模块清单在记录中被裁掉。
        if (t.length() > 8000) return t.substring(0, 8000);
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
        int expectedModuleCount = countExpectedModulesFromRequirementText(prompt);

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
                JsonNode specNode = parseJsonObjectLenient(rawContent);
                validator.validate(specNode);
                semanticValidator.validate(specNode, expectedModuleCount);
                validatedSpec = specNode;
                break;
            } catch (Exception e) {
                lastErr = e.getMessage() != null ? e.getMessage() : e.toString();
                // retry：按更鲁棒的“修复式”提示词，让模型只输出一个可解析的 JSON object
                String repairUser = buildRepairPrompt(userPrompt, rawContent, lastErr);
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
                + "interaction 字段必须使用标准键名，不要使用同义词：\n"
                + "- 必须使用 sourceId，不要使用 source/sourceNodeId/fromId/from/triggerId/trigger\n"
                + "- 必须使用 targetId，不要使用 target/targetNodeId/toId/to/panelId/tabsId\n"
                + "- 参数必须放在 params 内，不要把 open/tabKey 放在 interaction 顶层\n"
                + "\n"
                + "关于需求形态：\n"
                + "- 若输入为《页面设计文档》结构（含「需求来源」「信息架构与导航」「页面蓝图」等章节）：必须以**用户原始叙述**为主线推导页面与导航；文末「附录」表格仅作能力覆盖参考，**禁止**按表格行数机械生成同等数量的 Tab。\n"
                + "- 若输入含旧式标记【模块1】模块名与“功能点：”清单：可用 Tabs 分区，每 Tab 对应一模块，模块下用 Badge 列功能点。\n"
                + "- 其它自由文本：自行组织信息架构；须保证可预览、层次清晰、组件数量合理，且至少有一处可用的 Tabs（用于 MVP 切换）。\n"
                + "\n"
                + "输出前请自检（不输出自检过程）：\n"
                + "1) interactions 中每一项都有 type/sourceId/targetId/params\n"
                + "2) sourceId、targetId 都是 nodes 中存在的节点 id\n"
                + "3) togglePanel.params.open 为 boolean，setTab.params.tabKey 为 string\n"
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
                + "- 文案简洁\n"
                + "\n输出规则（务必遵守）：\n"
                + "1) 只输出一个 JSON object，不要任何解释文字/Markdown/代码块。\n"
                + "2) 若为《页面设计文档》导入稿：按文档中的信息架构与页面蓝图组织 Tabs/面板；Tab 标签用**页面或业务域名称**，不要用「附录表格行」当 Tab 标题。\n"
                + "3) 若出现旧式【模块N】+ 功能点清单：用 Tabs 分区并用 Badge 列出功能点。\n"
                + "4) 其它情况：自行组织合理结构，仍须包含可用的 Tabs。\n";
    }

    private static int countExpectedModulesFromRequirementText(String prompt) {
        if (prompt == null || prompt.isBlank()) return 0;
        // 需求模板中的模块标记：形如 【模块1】模块名
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("【模块\\d+】").matcher(prompt);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    private static final Pattern FENCED_JSON = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);

    private JsonNode parseJsonObjectLenient(String text) throws Exception {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("AI 输出为空，无法解析 JSON");
        }

        // 1) 直接 parse（正常情况下 response_format=json_object 会走这里）
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node != null && node.isObject()) return node;
        } catch (Exception ignore) {
            // continue
        }

        // 2) fenced ```json ... ```
        Matcher fenced = FENCED_JSON.matcher(trimmed);
        if (fenced.find()) {
            String inside = fenced.group(1);
            if (inside != null && !inside.trim().isEmpty()) {
                JsonNode node = objectMapper.readTree(inside.trim());
                if (node != null && node.isObject()) return node;
            }
        }

        // 3) first '{' ... last '}' slice（容忍前后多余内容）
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String sliced = trimmed.substring(firstBrace, lastBrace + 1);
            JsonNode node = objectMapper.readTree(sliced);
            if (node != null && node.isObject()) return node;
        }

        throw new IllegalArgumentException("无法从 AI 文本中解析 JSON object");
    }

    private String buildRepairPrompt(String originalUserPrompt, String previousOutput, String error) {
        String raw = previousOutput != null ? previousOutput : "";
        if (raw.length() > 12000) raw = raw.substring(0, 12000);
        String err = (error != null && !error.isBlank()) ? error : "未知错误";

        return ""
                + "你上一次的输出无法被解析/校验（常见原因：JSON 不完整、字段缺失、字符串未闭合、包含多余文字）。\n"
                + "请你【只输出一个 JSON 对象】并确保 100% 可被 JSON.parse 解析。\n"
                + "不要输出 ``` 代码块标记，不要输出任何解释文字。\n"
                + "\n"
                + "必须满足：\n"
                + "1) 输出是单个 JSON object（以 { 开头，以 } 结束）\n"
                + "2) layout.root 指向 nodes 中存在的节点 id\n"
                + "3) interactions 每项都有 type/sourceId/targetId/params\n"
                + "4) 禁止出现未定义的 interaction 键名别名（必须是 sourceId/targetId/params）\n"
                + "\n"
                + "这是用户原始需求（不要原样重复输出，只用于你生成 JSON）：\n"
                + originalUserPrompt + "\n"
                + "\n"
                + "这是你上一次的原始输出（供你修复参考）：\n"
                + raw + "\n"
                + "\n"
                + "上一次失败原因：\n"
                + err + "\n";
    }

    // ======== mvp-vue 的 HTML+CSS 解析/修复逻辑（Java 版复刻）========

    private record MockupGenerateResult(String rawContent, String html, String css, boolean repaired, String modelUsed) {}
    private record Design(String html, String css) {}

    private MockupGenerateResult generateMockupWithRepair(String prompt, String model) {
        // NOTE: 调用方已验证 projectId 与 prompt；这里仅专注于“调用模型 + 解析/修复”。
        AiAnalysisConfig cfg = configService.getConfig();
        String apiKey = cfg.getApiKey();
        String realModel = (model != null && !model.isBlank())
                ? model.trim()
                : (cfg.getModel() != null ? cfg.getModel() : "deepseek-chat");

        DeepSeekClient.ChatResult first = deepSeekClient.chatWithUsage(
                apiKey,
                realModel,
                List.of(new DeepSeekClient.ChatMessage("user", prompt.trim())),
                false,
                4096
        );
        String firstText = first != null ? Objects.toString(first.getContent(), "") : "";
        Design d1 = parseDesignFromText(firstText);
        if (d1 != null && (!d1.html.isBlank() || !d1.css.isBlank())) {
            return new MockupGenerateResult(firstText, d1.html, d1.css, false, first != null ? first.getModel() : realModel);
        }

        String repairPrompt = buildMockupRepairPrompt(firstText);
        DeepSeekClient.ChatResult second = deepSeekClient.chatWithUsage(
                apiKey,
                realModel,
                List.of(new DeepSeekClient.ChatMessage("user", repairPrompt)),
                false,
                4096
        );
        String secondText = second != null ? Objects.toString(second.getContent(), "") : "";
        Design d2 = parseDesignFromText(secondText);
        if (d2 == null) d2 = new Design("", "");
        return new MockupGenerateResult(secondText, d2.html, d2.css, true, second != null ? second.getModel() : realModel);
    }

    private static final Pattern FENCED_JSON_GENERIC = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
    private static final Pattern FENCED_HTML = Pattern.compile("```html\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
    private static final Pattern FENCED_CSS = Pattern.compile("```css\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_TAG = Pattern.compile("<style[\\s\\S]*?>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_TAG = Pattern.compile("<script[\\s\\S]*?>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);

    private Design parseDesignFromText(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return null;

        ObjectNode json = safeExtractJsonObject(trimmed);
        if (json != null) {
            String html = json.has("html") && json.get("html").isTextual() ? json.get("html").asText("") : "";
            String css = json.has("css") && json.get("css").isTextual() ? json.get("css").asText("") : "";
            html = sanitizeHtml(html);
            if (!html.isBlank() || !css.isBlank()) return new Design(html, css);
        }

        String htmlFence = extractFence(trimmed, FENCED_HTML);
        String cssFence = extractFence(trimmed, FENCED_CSS);
        if (htmlFence != null && cssFence != null) {
            return new Design(sanitizeHtml(htmlFence), cssFence);
        }

        if (trimmed.contains("<style")) {
            Matcher sm = STYLE_TAG.matcher(trimmed);
            String css = sm.find() ? Objects.toString(sm.group(1), "").trim() : "";
            String html = trimmed.replaceAll("(?i)<style[\\s\\S]*?>[\\s\\S]*?</style>", "");
            html = sanitizeHtml(html);
            if (!html.isBlank() || !css.isBlank()) return new Design(html, css);
        }

        return null;
    }

    private ObjectNode safeExtractJsonObject(String text) {
        String t = text != null ? text.trim() : "";
        if (t.isEmpty()) return null;

        try {
            JsonNode node = objectMapper.readTree(t);
            if (node != null && node.isObject()) return (ObjectNode) node;
        } catch (Exception ignore) { }

        Matcher fenced = FENCED_JSON_GENERIC.matcher(t);
        if (fenced.find() && fenced.group(1) != null) {
            try {
                JsonNode node = objectMapper.readTree(fenced.group(1).trim());
                if (node != null && node.isObject()) return (ObjectNode) node;
            } catch (Exception ignore) { }
        }

        int firstBrace = t.indexOf('{');
        int lastBrace = t.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            try {
                JsonNode node = objectMapper.readTree(t.substring(firstBrace, lastBrace + 1));
                if (node != null && node.isObject()) return (ObjectNode) node;
            } catch (Exception ignore) { }
        }
        return null;
    }

    private static String extractFence(String text, Pattern p) {
        Matcher m = p.matcher(text != null ? text : "");
        if (!m.find()) return null;
        String inside = m.group(1);
        return inside != null ? inside.trim() : null;
    }

    private static String sanitizeHtml(String html) {
        if (html == null) return "";
        String out = html;
        out = SCRIPT_TAG.matcher(out).replaceAll("");
        out = out.replaceAll("(?i)\\son[a-z]+\\s*=\\s*(['\"]).*?\\1", "");
        return out.trim();
    }

    private static String buildMockupRepairPrompt(String firstText) {
        String raw = firstText != null ? firstText : "";
        if (raw.length() > 12000) raw = raw.substring(0, 12000);
        return ""
                + "你刚才的输出无法被 JSON.parse 解析（通常是因为输出被截断或字符串未闭合）。\n"
                + "请你【只输出一个 JSON 对象】并确保 100% 可解析，结构固定为：\n"
                + "{ \"html\": \"...\", \"css\": \"...\" }\n"
                + "\n"
                + "约束：\n"
                + "1) 不要输出 ``` 代码块标记，不要输出任何解释文字\n"
                + "2) html/css 字符串必须闭合，不能被截断\n"
                + "3) CSS 要简洁（控制在 120 行以内），避免冗长\n"
                + "4) html 只包含 body 内元素，不要包含 <html>/<head>/<body>/<style>/<script>\n"
                + "5) 不要包含 <script> 或 on* 事件属性\n"
                + "\n"
                + "这是你上一次的原始输出（请基于它修复并压缩）：\n"
                + raw;
    }
}

