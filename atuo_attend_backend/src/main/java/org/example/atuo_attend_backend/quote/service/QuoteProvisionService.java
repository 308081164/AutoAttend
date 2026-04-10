package org.example.atuo_attend_backend.quote.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabSyncService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.quote.domain.QuoteItem;
import org.example.atuo_attend_backend.quote.domain.QuoteModule;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;
import org.example.atuo_attend_backend.quote.dto.QuoteProvisionRequest;
import org.example.atuo_attend_backend.quote.mapper.QuoteItemMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteModuleMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.tenant.quota.TenantQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class QuoteProvisionService {

    private static final Logger log = LoggerFactory.getLogger(QuoteProvisionService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private final RestTemplate githubRestTemplate;
    private final SystemConfigService systemConfigService;
    private final QuoteProjectMapper quoteProjectMapper;
    private final QuoteModuleMapper quoteModuleMapper;
    private final QuoteItemMapper quoteItemMapper;
    private final CollabSyncService collabSyncService;
    private final CollabTableService collabTableService;
    private final CollabRecordService collabRecordService;
    private final TenantQuotaService tenantQuotaService;
    private final TenantMapper tenantMapper;
    private final String fixedWebhookUrl;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuoteProvisionService(@Qualifier("githubApiRestTemplate") RestTemplate githubRestTemplate,
                                SystemConfigService systemConfigService,
                                QuoteProjectMapper quoteProjectMapper,
                                QuoteModuleMapper quoteModuleMapper,
                                QuoteItemMapper quoteItemMapper,
                                CollabSyncService collabSyncService,
                                CollabTableService collabTableService,
                                CollabRecordService collabRecordService,
                                TenantQuotaService tenantQuotaService,
                                TenantMapper tenantMapper,
                                @Value("${app.github.webhook.url:}") String fixedWebhookUrl) {
        this.githubRestTemplate = githubRestTemplate;
        this.systemConfigService = systemConfigService;
        this.quoteProjectMapper = quoteProjectMapper;
        this.quoteModuleMapper = quoteModuleMapper;
        this.quoteItemMapper = quoteItemMapper;
        this.collabSyncService = collabSyncService;
        this.collabTableService = collabTableService;
        this.collabRecordService = collabRecordService;
        this.tenantQuotaService = tenantQuotaService;
        this.tenantMapper = tenantMapper;
        this.fixedWebhookUrl = fixedWebhookUrl != null ? fixedWebhookUrl.trim() : "";
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public Map<String, Object> provision(long quoteProjectId, QuoteProvisionRequest req, HttpServletRequest httpReq) {
        QuoteProject qp = quoteProjectMapper.findById(tid(), quoteProjectId);
        if (qp == null) throw new IllegalArgumentException("报价项目不存在");
        String existingRepo = qp.getGithubRepoFullName();
        if (existingRepo == null || existingRepo.trim().isEmpty()) {
            var quota = tenantQuotaService.checkCanLinkGithubRepo(tid());
            if (!quota.allowed()) {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("status", "quota_blocked");
                out.put("blocked", true);
                out.put("message", quota.message());
                out.put("steps", List.of(stepFail("quota", quota.message())));
                return out;
            }
        }
        if (req == null) req = new QuoteProvisionRequest();
        String repoName = req.getRepoName() != null ? req.getRepoName().trim() : "";
        if (repoName.isBlank()) throw new IllegalArgumentException("repoName 不能为空");
        boolean repoPrivate = req.getRepoPrivate() != null ? req.getRepoPrivate() : true;
        boolean autoInit = req.getAutoInit() == null || req.getAutoInit();
        boolean syncMd = req.getSyncMd() == null || req.getSyncMd();
        boolean syncCollab = req.getSyncCollabTable() == null || req.getSyncCollabTable();
        boolean createWebhook = req.getCreateWebhook() == null || req.getCreateWebhook();
        boolean createAgentsMd = Boolean.TRUE.equals(req.getCreateAgentsMd());

        String token = systemConfigService.getGitHubToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("未配置 GitHub Token，请在「GitHub 集成」中填写后再试");
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        String repoFullName = null;
        String repoHtmlUrl = null;
        Long webhookId = null;
        String webhookSecret = null;
        Integer syncedToCollab = null;
        LocalDateTime syncedAt = null;
        String provisionStatus = "provisioning";
        String lastError = null;

        // 先标记 provisioning
        quoteProjectMapper.updateProvisionState(tid(), quoteProjectId, qp.getGithubRepoFullName(), qp.getGithubRepoHtmlUrl(),
                qp.getGithubWebhookId(), qp.getGithubWebhookSecret(), "provisioning", null,
                qp.getProvisionSyncedToCollab() != null && qp.getProvisionSyncedToCollab() ? 1 : 0,
                qp.getProvisionSyncedAt());

        try {
            Map<String, Object> created = createPersonalRepo(token, repoName, repoPrivate,
                    req.getDescription() != null ? req.getDescription() : buildDefaultRepoDescription(qp),
                    autoInit);
            repoFullName = String.valueOf(created.get("full_name"));
            repoHtmlUrl = String.valueOf(created.get("html_url"));
            steps.add(stepOk("createRepo", "已创建仓库", Map.of("repoFullName", repoFullName, "repoHtmlUrl", repoHtmlUrl)));

            if (syncMd) {
                String md = buildRequirementMarkdown(qp.getId(), qp.getName(), repoFullName);
                putRepoFile(token, repoFullName, "docs/需求清单.md",
                        "docs: add requirements list",
                        md);
                steps.add(stepOk("writeRequirementMd", "已写入 docs/需求清单.md", null));
            } else {
                steps.add(stepOk("writeRequirementMd", "已跳过写入需求 MD", null));
            }

            if (createAgentsMd) {
                String agents = buildAgentsMd(qp.getName(), repoFullName, quoteProjectId, syncMd);
                putRepoFile(token, repoFullName, "AGENTS.md",
                        "chore: add AGENTS.md for coding agents",
                        agents);
                steps.add(stepOk("writeAgentsMd", "已写入 AGENTS.md", null));
            } else {
                steps.add(stepOk("writeAgentsMd", "已跳过写入 AGENTS.md", null));
            }

            BizProject project = collabSyncService.ensureProjectAndTable(repoFullName);
            if (project == null) throw new IllegalStateException("创建协作项目失败");
            quoteProjectMapper.updateLinkTableId(tid(), quoteProjectId, project.getId());
            steps.add(stepOk("ensureCollabProject", "已创建/复用协作项目", Map.of("projectId", project.getId(), "repoId", project.getRepoId())));

            if (syncCollab) {
                BizProjectTable table = collabTableService.getTableByProjectId(project.getId());
                if (table == null) throw new IllegalStateException("项目未绑定表格");
                Map<String, Object> tableWithCols = collabTableService.getTableWithColumns(project.getId());
                int createdCount = syncQuoteItemsToCollabTable(table.getId(), tableWithCols, quoteProjectId);
                syncedToCollab = 1;
                syncedAt = LocalDateTime.now();
                steps.add(stepOk("syncCollabRecords", "已同步到多维表", Map.of("createdCount", createdCount)));
            } else {
                steps.add(stepOk("syncCollabRecords", "已跳过同步多维表", null));
            }

            if (createWebhook) {
                webhookSecret = randomSecret(32);
                String hookUrl = buildPublicWebhookUrl(httpReq);
                webhookId = createWebhook(token, repoFullName, hookUrl, webhookSecret);
                steps.add(stepOk("createWebhook", "已创建 Webhook", Map.of("webhookId", webhookId, "url", hookUrl)));
            } else {
                steps.add(stepOk("createWebhook", "已跳过创建 Webhook", null));
            }

            provisionStatus = "done";
        } catch (Exception e) {
            provisionStatus = "failed";
            lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            steps.add(stepFail("failed", "失败：" + lastError));
            log.warn("Quote provision failed, quoteProjectId={}: {} - {}", quoteProjectId, e.getClass().getSimpleName(), lastError);
        } finally {
            quoteProjectMapper.updateProvisionState(tid(), quoteProjectId, repoFullName, repoHtmlUrl, webhookId, webhookSecret,
                    provisionStatus, lastError, syncedToCollab, syncedAt);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("repoFullName", repoFullName);
        out.put("repoHtmlUrl", repoHtmlUrl);
        out.put("steps", steps);
        out.put("status", provisionStatus);
        return out;
    }

    private Map<String, Object> createPersonalRepo(String token, String repoName, boolean repoPrivate, String description, boolean autoInit) {
        String url = "https://api.github.com/user/repos";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", repoName);
        body.put("private", repoPrivate);
        if (description != null) body.put("description", description);
        body.put("auto_init", autoInit);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<Map> resp = githubRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("创建仓库失败");
        }
        return resp.getBody();
    }

    private void putRepoFile(String token, String repoFullName, String path, String message, String contentText) {
        String url = "https://api.github.com/repos/" + repoFullName + "/contents/" + urlEncodePath(path);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("content", Base64.getEncoder().encodeToString(contentText.getBytes(StandardCharsets.UTF_8)));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<Map> resp = githubRestTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("写入文件失败：" + path);
        }
    }

    private Long createWebhook(String token, String repoFullName, String webhookUrl, String secret) {
        String url = "https://api.github.com/repos/" + repoFullName + "/hooks";
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("url", webhookUrl);
        cfg.put("content_type", "json");
        cfg.put("secret", secret);
        cfg.put("insecure_ssl", "0");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "web");
        body.put("active", true);
        body.put("events", List.of("push"));
        body.put("config", cfg);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<Map> resp = githubRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<?, ?> data = resp.getBody();
        if (!resp.getStatusCode().is2xxSuccessful() || data == null || data.get("id") == null) {
            throw new IllegalStateException("创建 Webhook 失败");
        }
        return ((Number) data.get("id")).longValue();
    }

    private int syncQuoteItemsToCollabTable(long tableId, Map<String, Object> tableWithCols, long quoteProjectId) {
        int created = 0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = tableWithCols != null ? (List<Map<String, Object>>) tableWithCols.get("columns") : null;
        if (columns == null) throw new IllegalStateException("读取表结构失败");

        Long colProblem = findColId(columns, "问题描述");
        Long colModule = findColId(columns, "归属模块");
        Long colStatus = findColId(columns, "当前状态");
        Long colAccept = findColId(columns, "验收结果");
        Long colCreator = findColId(columns, "创建人");
        Long colCreateTime = findColId(columns, "创建时间");

        for (QuoteModule m : quoteModuleMapper.listByProjectId(tid(), quoteProjectId)) {
            List<QuoteItem> items = quoteItemMapper.listByModuleId(tid(), m.getId());
            for (QuoteItem it : items) {
                Map<String, Object> fields = new HashMap<>();
                String desc = "【" + m.getName() + "】" + it.getName()
                        + "\n复杂度：" + it.getComplexity()
                        + "\n数量：" + it.getQuantity();
                if (colProblem != null) fields.put("c" + colProblem, desc);
                if (colModule != null) fields.put("c" + colModule, m.getName());
                if (colStatus != null) fields.put("c" + colStatus, "已创建");
                if (colAccept != null) fields.put("c" + colAccept, "未验收");
                if (colCreator != null) fields.put("c" + colCreator, "报价系统");
                // 使用 LocalDateTime.toString() 的 ISO 格式，确保后端可解析写入 value_date（datetime 列）
                if (colCreateTime != null) fields.put("c" + colCreateTime, LocalDateTime.now().toString());

                collabRecordService.createRecordWithAudit(tableId, null, fields, null, "quote_provision");
                created++;
            }
        }
        return created;
    }

    private Long findColId(List<Map<String, Object>> cols, String name) {
        for (Map<String, Object> c : cols) {
            if (c == null) continue;
            Object n = c.get("name");
            if (n != null && name.equals(String.valueOf(n).trim())) {
                Object id = c.get("id");
                if (id instanceof Number) return ((Number) id).longValue();
                if (id != null) return Long.parseLong(String.valueOf(id));
            }
        }
        return null;
    }

    private static Map<String, Object> stepOk(String key, String message, Map<String, Object> data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("ok", true);
        m.put("message", message);
        if (data != null) m.put("data", data);
        return m;
    }

    private static Map<String, Object> stepFail(String key, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("ok", false);
        m.put("message", message);
        return m;
    }

    private String buildDefaultRepoDescription(QuoteProject qp) {
        String name = qp.getName() != null ? qp.getName().trim() : "";
        return name.isBlank() ? "AutoAttend 报价项目" : ("AutoAttend 报价项目：" + name);
    }

    private String buildRequirementMarkdown(long quoteProjectId, String quoteProjectName, String repoFullName) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 需求清单\n\n");
        if (quoteProjectName != null && !quoteProjectName.isBlank()) {
            sb.append("- 项目：").append(quoteProjectName.trim()).append("\n");
        }
        if (repoFullName != null && !repoFullName.isBlank()) {
            sb.append("- 仓库：").append(repoFullName.trim()).append("\n");
        }
        sb.append("- 报价项目ID：").append(quoteProjectId).append("\n\n");
        sb.append("## 功能模块与功能点\n\n");
        for (QuoteModule m : quoteModuleMapper.listByProjectId(tid(), quoteProjectId)) {
            sb.append("### ").append(m.getName()).append("\n\n");
            for (QuoteItem it : quoteItemMapper.listByModuleId(tid(), m.getId())) {
                sb.append("- ").append(it.getName())
                        .append("（复杂度：").append(it.getComplexity())
                        .append("，数量：").append(it.getQuantity())
                        .append("）\n");
            }
            sb.append("\n");
        }
        sb.append("## 备注\n\n");
        sb.append("- 本文件由 AutoAttend 报价系统自动生成。\n");
        return sb.toString();
    }

    /**
     * 根目录 AGENTS.md，结构对齐开放格式 https://agents.md/（面向编码助手的 Markdown 说明，无强制字段）。
     */
    private String buildAgentsMd(String quoteProjectName, String repoFullName, long quoteProjectId, boolean requirementMdPresent) {
        String name = quoteProjectName != null ? quoteProjectName.trim() : "";
        String repo = repoFullName != null ? repoFullName.trim() : "";
        StringBuilder sb = new StringBuilder();
        sb.append("# AGENTS.md\n\n");
        sb.append("Instructions for AI coding agents working in this repository. ");
        sb.append("This file follows the community AGENTS.md convention (see https://agents.md/).\n\n");
        sb.append("## Project overview\n\n");
        sb.append("- **Source**: Repository provisioned from **AutoAttend** (quote → GitHub workflow).\n");
        if (!name.isBlank()) {
            sb.append("- **Quote project name**: ").append(name).append("\n");
        }
        sb.append("- **Quote project ID** (in AutoAttend): ").append(quoteProjectId).append("\n");
        if (!repo.isBlank()) {
            sb.append("- **GitHub**: `").append(repo).append("`\n");
        }
        sb.append("\n");
        if (requirementMdPresent) {
            sb.append("Primary scope and feature breakdown: **`docs/需求清单.md`** (generated alongside this repo).\n\n");
        } else {
            sb.append("Requirements markdown was not written during provisioning; align scope with the product owner or add `docs/需求清单.md` manually.\n\n");
        }
        sb.append("## Setup commands\n\n");
        sb.append("- Clone: `git clone https://github.com/").append(repo.isBlank() ? "<owner>/<repo>" : repo).append(".git`\n");
        sb.append("- Install dependencies and run dev servers according to the stack you add to this repo.\n\n");
        sb.append("## Build and test\n\n");
        sb.append("- After introducing a build system, document the exact commands here (for example `pnpm build`, `mvn verify`, `pytest`).\n");
        sb.append("- Prefer running the full test/lint suite before opening a PR.\n\n");
        sb.append("## Code style\n\n");
        sb.append("- Follow the conventions of the languages and frameworks in this repository once code exists.\n");
        sb.append("- Keep changes focused; avoid unrelated refactors in the same change as feature work.\n\n");
        sb.append("## Integrations (AutoAttend)\n\n");
        sb.append("- **Collaboration**: A linked AutoAttend project / table may receive synced records from the quote; treat tabular data as downstream of this repo unless your team agrees otherwise.\n");
        sb.append("- **Webhook**: If configured, pushes may trigger AutoAttend; avoid force-pushing shared default branches without team agreement.\n\n");
        sb.append("## Security\n\n");
        sb.append("- Never commit secrets, API tokens, or real `.env` files.\n");
        sb.append("- Do not embed tenant or customer credentials in documentation or sample code.\n");
        return sb.toString();
    }

    private String buildPublicWebhookUrl(HttpServletRequest req) {
        String path = resolveWebhookPathForCurrentTenant();
        if (fixedWebhookUrl != null && !fixedWebhookUrl.isBlank()) {
            String u = fixedWebhookUrl.replaceAll("/+$", "");
            Tenant t = tenantMapper.findById(tid());
            if (t != null && t.getSlug() != null && !t.getSlug().isBlank()) {
                String slug = t.getSlug().trim();
                if (!u.endsWith("/" + slug)) {
                    return u + "/" + slug;
                }
            }
            return u;
        }
        // 优先使用 X-Forwarded-*，以适配反向代理/HTTPS
        String proto = headerFirst(req, "X-Forwarded-Proto");
        String host = headerFirst(req, "X-Forwarded-Host");
        if (proto == null || proto.isBlank()) proto = req.getScheme();
        if (host == null || host.isBlank()) host = req.getHeader("Host");
        if (host == null || host.isBlank()) host = req.getServerName() + (req.getServerPort() > 0 ? (":" + req.getServerPort()) : "");
        String base = proto + "://" + host;
        return base + path;
    }

    private String resolveWebhookPathForCurrentTenant() {
        Tenant t = tenantMapper.findById(tid());
        if (t != null && t.getSlug() != null && !t.getSlug().isBlank()) {
            return "/api/webhooks/github/" + t.getSlug().trim();
        }
        return "/api/webhooks/github";
    }

    private String headerFirst(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        if (v == null) return null;
        int idx = v.indexOf(',');
        return (idx >= 0 ? v.substring(0, idx) : v).trim();
    }

    private static String randomSecret(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String urlEncodePath(String path) {
        // 逐段编码，保留 '/'
        if (path == null) return "";
        String[] parts = path.split("/");
        List<String> encoded = new ArrayList<>();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            encoded.add(java.net.URLEncoder.encode(p, StandardCharsets.UTF_8));
        }
        return String.join("/", encoded);
    }
}

