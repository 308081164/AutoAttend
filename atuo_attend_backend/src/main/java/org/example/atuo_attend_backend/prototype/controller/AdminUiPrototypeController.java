package org.example.atuo_attend_backend.prototype.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.platform.service.PlatformComponentEventService;
import org.example.atuo_attend_backend.prototype.config.PenpotProperties;
import org.example.atuo_attend_backend.prototype.penpot.PenpotRpcClient;
import org.example.atuo_attend_backend.prototype.penpot.TenantPenpotOnboardingService;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.domain.TenantPenpotCredential;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantPenpotCredentialMapper;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectDetail;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectListItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectCreateRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectRenameRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateJobStatus;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateSpecRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateMockupRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeImportQuoteRequirementRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeSaveMockupMessagesRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypePenpotJobStatus;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypePenpotGenerateRequest;
import org.example.atuo_attend_backend.prototype.service.UiPrototypePenpotService;
import org.example.atuo_attend_backend.quote.service.QuoteService;
import org.example.atuo_attend_backend.prototype.service.UiPrototypeService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/ui-prototype")
public class AdminUiPrototypeController {

    private final UiPrototypeService uiPrototypeService;
    private final UiPrototypePenpotService uiPrototypePenpotService;
    private final QuoteService quoteService;
    private final PlatformComponentEventService componentEventService;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final PenpotProperties penpotProperties;
    private final PenpotRpcClient penpotRpcClient;
    private final TenantPenpotOnboardingService tenantPenpotOnboardingService;
    private final TenantPenpotCredentialMapper tenantPenpotCredentialMapper;
    private final RestTemplate restTemplate;

    public AdminUiPrototypeController(UiPrototypeService uiPrototypeService,
                                      UiPrototypePenpotService uiPrototypePenpotService,
                                      QuoteService quoteService,
                                      PlatformComponentEventService componentEventService,
                                      TenantAdminUserMapper tenantAdminUserMapper,
                                      PenpotProperties penpotProperties,
                                      PenpotRpcClient penpotRpcClient,
                                      TenantPenpotOnboardingService tenantPenpotOnboardingService,
                                      TenantPenpotCredentialMapper tenantPenpotCredentialMapper,
                                      RestTemplate restTemplate) {
        this.uiPrototypeService = uiPrototypeService;
        this.uiPrototypePenpotService = uiPrototypePenpotService;
        this.quoteService = quoteService;
        this.componentEventService = componentEventService;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.penpotProperties = penpotProperties;
        this.penpotRpcClient = penpotRpcClient;
        this.tenantPenpotOnboardingService = tenantPenpotOnboardingService;
        this.tenantPenpotCredentialMapper = tenantPenpotCredentialMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Penpot Beta 是否可用。
     * @param probe 传 1 或 true 时额外请求 Penpot get-teams 做连通性检查（略慢，勿高频轮询）
     */
    @GetMapping("/penpot/status")
    public ApiResponse<Map<String, Object>> penpotStatus(@RequestParam(required = false) String probe,
                                                         HttpServletRequest req) {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", uiPrototypePenpotService.isFeatureEnabled());
        data.put("configured", uiPrototypePenpotService.isConfiguredOnly());
        Long uid = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        Long tenantId = (Long) req.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        data.put("mustBootstrap", false);
        if (tenantId != null && uiPrototypePenpotService.isFeatureEnabled()) {
            boolean admin = isTenantAdminForCurrentTenant(uid, tenantId);
            data.put("penpotReady", admin && uiPrototypePenpotService.isTenantPenpotReady(tenantId));
            if (admin) {
                data.put("mustBootstrap", !uiPrototypePenpotService.isTenantPenpotReady(tenantId));
            }
        } else {
            data.put("penpotReady", false);
        }
        boolean doProbe = "1".equals(probe) || "true".equalsIgnoreCase(probe);
        if (doProbe) {
            if (tenantId != null && isTenantAdminForCurrentTenant(uid, tenantId)
                    && uiPrototypePenpotService.isTenantPenpotReady(tenantId)) {
                data.put("reachable", uiPrototypePenpotService.probeConnection());
            } else {
                data.put("reachable", false);
            }
        }
        return ApiResponse.ok(data);
    }

    /**
     * 租户管理员首次进入快原型 Penpot Beta 时调用：自动注册 Penpot 账号并落库租户 Token（一租户一账号）。
     */
    @PostMapping("/penpot/bootstrap")
    public ApiResponse<Map<String, Object>> penpotBootstrap(HttpServletRequest req) {
        Long uid = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        Long tenantId = (Long) req.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        if (!isTenantAdminForCurrentTenant(uid, tenantId)) {
            return ApiResponse.error(40300, "仅租户管理员可开通 Penpot 工作区");
        }
        try {
            uiPrototypePenpotService.ensureTenantPenpotOnboarded();
            Map<String, Object> data = new HashMap<>();
            data.put("ok", true);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    @GetMapping("/projects")
    public ApiResponse<Map<String, Object>> listProjects() {
        List<UiPrototypeProjectListItem> items = uiPrototypeService.listProjects();
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }

    @PostMapping("/projects")
    public ApiResponse<Map<String, Object>> createProject(@RequestBody(required = false) UiPrototypeProjectCreateRequest body) {
        String name = body != null ? body.getName() : null;
        long id = uiPrototypeService.createProject(name);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return ApiResponse.ok(data);
    }

    @PutMapping("/projects/{id}")
    public ApiResponse<Void> renameProject(@PathVariable long id,
                                            @RequestBody(required = false) UiPrototypeProjectRenameRequest body) {
        String name = body != null ? body.getName() : null;
        uiPrototypeService.renameProject(id, name);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable long id) {
        uiPrototypeService.deleteProject(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/projects/{id}")
    public ApiResponse<UiPrototypeProjectDetail> getProject(@PathVariable long id) {
        UiPrototypeProjectDetail d = uiPrototypeService.getProjectDetail(id);
        if (d == null) return ApiResponse.error(40400, "项目不存在");
        return ApiResponse.ok(d);
    }

    /**
     * 异步生成：立即返回 jobId，客户端轮询 {@link #getGenerateJob(long, long)}。
     * 避免 LLM 耗时超过 nginx 反向代理超时导致 504。
     */
    @PostMapping("/projects/{id}/specs/generate")
    public ApiResponse<Map<String, Object>> enqueueGenerateSpec(@PathVariable long id,
                                                                 @RequestBody(required = false) UiPrototypeGenerateSpecRequest body,
                                                                 HttpServletRequest req) {
        String prompt = body != null ? body.getPrompt() : null;
        try {
            long jobId = uiPrototypeService.enqueueGenerateSpec(id, prompt);

            // usage：实际调用核心接口（即“生成结构化 spec”）
            Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
            String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
            componentEventService.recordUsage(adminUserId, adminPhone, "hub_prototype", "ui_prototype_generate_spec");

            Map<String, Object> data = new HashMap<>();
            data.put("jobId", jobId);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    @GetMapping("/projects/{id}/specs/jobs/{jobId}")
    public ApiResponse<UiPrototypeGenerateJobStatus> getGenerateJob(@PathVariable long id, @PathVariable long jobId) {
        UiPrototypeGenerateJobStatus s = uiPrototypeService.getGenerateJobStatus(id, jobId);
        if (s == null) return ApiResponse.error(40400, "任务不存在");
        return ApiResponse.ok(s);
    }

    @GetMapping("/projects/{id}/mockup")
    public ApiResponse<Map<String, Object>> getMockup(@PathVariable long id) {
        UiPrototypeProjectDetail d = uiPrototypeService.getProjectDetail(id);
        if (d == null) return ApiResponse.error(40400, "项目不存在");
        org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockup m = uiPrototypeService.getMockup(id);
        Map<String, Object> data = new HashMap<>();
        if (m != null) {
            data.put("html", m.getHtml());
            data.put("css", m.getCss());
            data.put("rawAiContent", m.getRawAiContent());
            data.put("messagesJson", m.getMessagesJson());
            data.put("modelUsed", m.getModelUsed());
            data.put("repaired", m.isRepaired());
            data.put("updatedAt", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
        } else {
            data.put("html", "");
            data.put("css", "");
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/projects/{id}/mockup/messages")
    public ApiResponse<Void> saveMockupMessages(@PathVariable long id,
                                                @RequestBody(required = false) UiPrototypeSaveMockupMessagesRequest body) {
        if (uiPrototypeService.getProjectDetail(id) == null) {
            return ApiResponse.error(40400, "项目不存在");
        }
        String messagesJson = body != null ? body.getMessagesJson() : null;
        uiPrototypeService.saveMockupMessages(id, messagesJson);
        return ApiResponse.ok(null);
    }

    /**
     * HTML+CSS mockup：异步生成任务（与 mvp-vue 体验一致，但用 jobId 轮询避免 504）。
     */
    @PostMapping("/projects/{id}/mockups/generate")
    public ApiResponse<Map<String, Object>> enqueueGenerateMockup(@PathVariable long id,
                                                                  @RequestBody(required = false) UiPrototypeGenerateMockupRequest body,
                                                                  HttpServletRequest req) {
        String prompt = body != null ? body.getPrompt() : null;
        String model = body != null ? body.getModel() : null;
        String messagesJson = body != null ? body.getMessagesJson() : null;
        try {
            long jobId = uiPrototypeService.enqueueGenerateMockup(id, prompt, model, messagesJson);

            // usage：实际调用核心接口（即“生成 HTML/CSS mockup”）
            Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
            String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
            componentEventService.recordUsage(adminUserId, adminPhone, "hub_prototype", "ui_prototype_generate_mockup");

            Map<String, Object> data = new HashMap<>();
            data.put("jobId", jobId);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    @GetMapping("/projects/{id}/mockups/jobs/{jobId}")
    public ApiResponse<Map<String, Object>> getMockupGenerateJob(@PathVariable long id, @PathVariable long jobId) {
        org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockupGenerateJob j =
                uiPrototypeService.getMockupGenerateJobStatus(id, jobId);
        if (j == null) return ApiResponse.error(40400, "任务不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", j.getId());
        data.put("status", j.getStatus());
        data.put("errorMessage", j.getErrorMessage());
        data.put("createdAt", j.getCreatedAt() != null ? j.getCreatedAt().toString() : null);
        data.put("updatedAt", j.getUpdatedAt() != null ? j.getUpdatedAt().toString() : null);
        return ApiResponse.ok(data);
    }

    /**
     * 从报价项目生成快原型需求正文（后端判别）：有 PRD/AI 原文则仅返回该叙述；无则仅返回功能清单表。供快原型需求框使用。
     */
    /**
     * Penpot Beta：LLM 规划 → 创建文件 → 写入画布 → 导出 .penpot（异步任务）；方案 A 技术账号。
     */
    @PostMapping("/projects/{id}/penpot/jobs")
    public ApiResponse<Map<String, Object>> enqueuePenpotJob(@PathVariable long id,
                                                             @RequestBody(required = false) UiPrototypePenpotGenerateRequest body,
                                                             HttpServletRequest req) {
        Long uid = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        Long tenantId = (Long) req.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        if (!isTenantAdminForCurrentTenant(uid, tenantId)) {
            return ApiResponse.error(40300, "仅租户管理员可使用 Penpot Beta 生成");
        }
        try {
            String prompt = body != null ? body.getPrompt() : null;
            String note = body != null ? body.getNote() : null;
            long jobId = uiPrototypePenpotService.enqueueGenerate(id, prompt, note);
            Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
            String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
            componentEventService.recordUsage(adminUserId, adminPhone, "hub_prototype", "ui_prototype_penpot_generate");
            Map<String, Object> data = new HashMap<>();
            data.put("jobId", jobId);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    /** 重新请求 .penpot 导出链接（临时 URL，与任务成功时一致） */
    @GetMapping("/projects/{id}/penpot/export-binfile")
    public ApiResponse<Map<String, Object>> penpotExportBinfile(@PathVariable long id, HttpServletRequest req) {
        Long uid = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        Long tenantId = (Long) req.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        if (!isTenantAdminForCurrentTenant(uid, tenantId)) {
            return ApiResponse.error(40300, "仅租户管理员可导出 Penpot 文件");
        }
        try {
            String url = uiPrototypePenpotService.exportBinfileForProject(id);
            Map<String, Object> data = new HashMap<>();
            data.put("exportBinfileUrl", url);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    @GetMapping("/projects/{id}/penpot/jobs/{jobId}")
    public ApiResponse<UiPrototypePenpotJobStatus> getPenpotJob(@PathVariable long id, @PathVariable long jobId,
                                                                HttpServletRequest req) {
        Long uid = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        Long tenantId = (Long) req.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        if (!isTenantAdminForCurrentTenant(uid, tenantId)) {
            return ApiResponse.error(40300, "仅租户管理员可查看 Penpot 任务");
        }
        UiPrototypePenpotJobStatus s = uiPrototypePenpotService.getJobStatus(id, jobId);
        if (s == null) return ApiResponse.error(40400, "任务不存在");
        return ApiResponse.ok(s);
    }

    @PostMapping("/projects/{id}/import-quote-requirement")
    public ApiResponse<Map<String, Object>> importQuoteRequirement(@PathVariable long id,
                                                                   @RequestBody(required = false) UiPrototypeImportQuoteRequirementRequest body) {
        try {
            if (uiPrototypeService.getProjectDetail(id) == null) {
                return ApiResponse.error(40400, "快原型项目不存在");
            }
            Long quoteProjectId = body != null ? body.getQuoteProjectId() : null;
            if (quoteProjectId == null || quoteProjectId <= 0) {
                return ApiResponse.error(40000, "quoteProjectId 不能为空");
            }
            return ApiResponse.ok(quoteService.buildPrototypeRequirementFromQuote(quoteProjectId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    // ==================== Penpot 诊断 ====================

    /**
     * Penpot 连通性与配置诊断端点。
     * 逐步检查配置、网络连通性、RPC 路径、租户凭证等，返回每一步的状态与耗时。
     */
    @GetMapping("/penpot/diagnose")
    public ApiResponse<Map<String, Object>> penpotDiagnose(HttpServletRequest req) {
        Long uid = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        Long tenantId = (Long) req.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        if (!isTenantAdminForCurrentTenant(uid, tenantId)) {
            return ApiResponse.error(40300, "仅租户管理员可执行 Penpot 诊断");
        }

        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 功能是否启用
        checks.add(checkStep("功能启用状态", () -> {
            if (penpotProperties.isEnabled()) {
                return stepOk("Penpot 功能已启用");
            }
            return stepFail("Penpot 功能未启用：请在 .env 中设置 PENPOT_ENABLED=true");
        }));

        if (!penpotProperties.isEnabled()) {
            result.put("checks", checks);
            result.put("summary", "Penpot 功能未启用，后续检查已跳过");
            return ApiResponse.ok(result);
        }

        // 2. 配置检查（脱敏）
        checks.add(checkStep("配置检查", () -> {
            Map<String, String> config = new LinkedHashMap<>();
            config.put("baseUrl", maskUri(penpotProperties.getBaseUrl()));
            config.put("apiBaseUrl", maskUri(penpotProperties.getApiBaseUrl()));
            config.put("effectiveRpcBaseUrl", maskUri(penpotProperties.getEffectiveRpcBaseUrl()));
            config.put("backendDirectUri", maskUri(penpotProperties.getBackendDirectUri()));
            config.put("publicUri", maskUri(penpotProperties.getPublicUri()));
            config.put("rpcPathStyle", penpotProperties.getRpcPathStyle() != null ? penpotProperties.getRpcPathStyle() : "(auto)");
            config.put("clientHeader", penpotProperties.getClientHeader() != null ? penpotProperties.getClientHeader() : "(默认)");
            config.put("tenantEmailDomain", penpotProperties.getTenantEmailDomain());
            config.put("hasAccessToken", hasText(penpotProperties.getAccessToken()));
            config.put("hasEmailPassword", hasText(penpotProperties.getEmail()) && hasText(penpotProperties.getPassword()));

            // 检查常见配置错误
            List<String> warnings = new ArrayList<>();
            String effective = penpotProperties.getEffectiveRpcBaseUrl();
            if (effective != null && (effective.contains("localhost") && !effective.contains("penpot"))) {
                warnings.add("effectiveRpcBaseUrl 包含 localhost，在 Docker 环境中可能不可达");
            }
            if (effective != null && effective.contains("/api/")) {
                warnings.add("effectiveRpcBaseUrl 不应包含路径部分（/api），仅需 scheme://host:port");
            }
            if (effective != null && (effective.contains(":80") || effective.contains(":443") || effective.contains(":8080") || effective.contains(":6060"))) {
                // ok
            } else if (effective != null && !effective.contains(":")) {
                warnings.add("effectiveRpcBaseUrl 未指定端口（默认 80），请确认 Penpot 监听端口");
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", warnings.isEmpty() ? "ok" : "warn");
            out.put("message", warnings.isEmpty() ? "配置正常" : "配置存在潜在问题");
            out.put("config", config);
            if (!warnings.isEmpty()) {
                out.put("warnings", warnings);
            }
            return out;
        }));

        // 3. PRIMARY 基址连通性
        String primaryBase = trimSlash(penpotProperties.getEffectiveRpcBaseUrl());
        if (hasText(primaryBase)) {
            checks.add(checkStep("PRIMARY 基址连通 (" + maskUri(primaryBase) + ")", () -> {
                return testBaseConnectivity(primaryBase, "PRIMARY");
            }));
        } else {
            checks.add(checkStep("PRIMARY 基址连通", () ->
                    stepFail("PRIMARY 基址为空（baseUrl 和 apiBaseUrl 均未配置）")));
        }

        // 4. DIRECT 基址连通性
        String directBase = trimSlash(penpotProperties.getBackendDirectUri());
        if (hasText(directBase)) {
            checks.add(checkStep("DIRECT 基址连通 (" + maskUri(directBase) + ")", () -> {
                return testBaseConnectivity(directBase, "DIRECT");
            }));
        } else {
            checks.add(checkStep("DIRECT 基址连通", () ->
                    stepWarn("DIRECT 基址未配置（backendDirectUri 为空），将仅使用 PRIMARY 基址")));
        }

        // 5. RPC new 路径测试
        String rpcTestBase = hasText(primaryBase) ? primaryBase : directBase;
        if (hasText(rpcTestBase)) {
            checks.add(checkStep("RPC new 路径测试 (/api/main/methods/)", () -> {
                return testRpcPath(rpcTestBase, "/api/main/methods/get-teams", "new");
            }));

            // 6. RPC legacy 路径测试
            checks.add(checkStep("RPC legacy 路径测试 (/api/rpc/command/)", () -> {
                return testRpcPath(rpcTestBase, "/api/rpc/command/get-teams", "legacy");
            }));
        }

        // 7. 租户凭证检查
        long effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        checks.add(checkStep("租户凭证检查", () -> {
            TenantPenpotCredential cred = tenantPenpotCredentialMapper.findByTenantId(effectiveTenantId);
            if (cred == null) {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("status", "warn");
                out.put("message", "当前租户尚未创建 Penpot 凭证（首次使用时将自动注册）");
                out.put("hasCredential", false);
                return out;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", "ok");
            out.put("message", "租户凭证已存在");
            out.put("hasCredential", true);
            out.put("penpotEmail", cred.getPenpotEmail());
            out.put("createdAt", cred.getCreatedAt() != null ? cred.getCreatedAt().toString() : null);
            out.put("updatedAt", cred.getUpdatedAt() != null ? cred.getUpdatedAt().toString() : null);
            return out;
        }));

        // 8. 如果凭证存在，测试认证 RPC
        TenantPenpotCredential cred = tenantPenpotCredentialMapper.findByTenantId(effectiveTenantId);
        if (cred != null) {
            checks.add(checkStep("认证 RPC 测试 (get-teams)", () -> {
                try {
                    String token = tenantPenpotOnboardingService.getOrCreateTenantAccessToken(effectiveTenantId);
                    penpotRpcClient.command("get-teams", new HashMap<>(), token);
                    return stepOk("认证 RPC 调用成功（使用租户 Token）");
                } catch (Exception e) {
                    return stepFail("认证 RPC 失败：" + truncateMsg(e.getMessage(), 500));
                }
            }));
        } else {
            // 9. 如果凭证不存在，测试 prepare-register-profile 是否可达
            checks.add(checkStep("prepare-register-profile 可达性", () -> {
                return testRpcPath(rpcTestBase, "/api/main/methods/prepare-register-profile", "new (prepare-register-profile)");
            }));
        }

        // 汇总
        long failCount = checks.stream().filter(c -> "fail".equals(c.get("status"))).count();
        long warnCount = checks.stream().filter(c -> "warn".equals(c.get("status"))).count();
        String summary;
        if (failCount > 0) {
            summary = "诊断完成：" + failCount + " 项失败，" + warnCount + " 项警告，" + (checks.size() - failCount - warnCount) + " 项通过";
        } else if (warnCount > 0) {
            summary = "诊断完成：全部通过，" + warnCount + " 项警告";
        } else {
            summary = "诊断完成：全部通过，Penpot 连接正常";
        }

        result.put("checks", checks);
        result.put("summary", summary);
        return ApiResponse.ok(result);
    }

    // ==================== 诊断辅助方法 ====================

    private interface CheckRunnable {
        Map<String, Object> run();
    }

    private Map<String, Object> checkStep(String name, CheckRunnable r) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", name);
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> result = r.run();
            step.put("status", result.getOrDefault("status", "unknown"));
            step.put("message", result.getOrDefault("message", ""));
            // 合并额外字段
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                if (!"status".equals(entry.getKey()) && !"message".equals(entry.getKey())) {
                    step.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            step.put("status", "fail");
            step.put("message", "诊断异常：" + truncateMsg(e.getMessage(), 500));
        }
        step.put("durationMs", System.currentTimeMillis() - start);
        return step;
    }

    private Map<String, Object> stepOk(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        m.put("message", msg);
        return m;
    }

    private Map<String, Object> stepWarn(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "warn");
        m.put("message", msg);
        return m;
    }

    private Map<String, Object> stepFail(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "fail");
        m.put("message", msg);
        return m;
    }

    private Map<String, Object> testBaseConnectivity(String baseUri, String label) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(h);
            ResponseEntity<String> resp = restTemplate.exchange(baseUri + "/", HttpMethod.GET, entity, String.class);
            int code = resp.getStatusCodeValue();
            if (code >= 200 && code < 500) {
                return stepOk("HTTP " + code + " — " + label + " 基址可达（服务已响应）");
            }
            return stepFail("HTTP " + code + " — " + label + " 基址返回异常状态码");
        } catch (ResourceAccessException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Failed to resolve") || msg.contains("Name or service not known")) {
                return stepFail("DNS 解析失败：无法解析 " + extractHost(baseUri)
                        + "。请确认容器网络配置正确，或检查 PENPOT_INTERNAL_URI 是否指向正确的容器名");
            }
            if (msg.contains("Connection refused")) {
                return stepFail("连接被拒绝：" + extractHost(baseUri) + " 端口未监听。请确认 Penpot 容器已启动且端口映射正确");
            }
            if (msg.contains("timed out") || msg.contains("Timeout")) {
                return stepFail("连接超时：" + extractHost(baseUri) + " 不可达。请检查网络和防火墙配置");
            }
            return stepFail("网络错误：" + truncateMsg(msg, 400));
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCodeValue();
            if (code >= 200 && code < 500) {
                return stepOk("HTTP " + code + " — " + label + " 基址可达");
            }
            return stepFail("HTTP " + code + " — " + label + " 基址返回异常状态码");
        } catch (Exception e) {
            return stepFail("请求异常：" + truncateMsg(e.getMessage(), 400));
        }
    }

    private Map<String, Object> testRpcPath(String baseUri, String path, String label) {
        String url = baseUri + path;
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            String clientHeader = penpotProperties.getClientHeader();
            if (hasText(clientHeader)) {
                h.set("x-client", clientHeader.trim());
            }
            HttpEntity<String> entity = new HttpEntity<>("{}", h);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            int code = resp.getStatusCodeValue();
            if (code == 401 || code == 403) {
                return stepOk("HTTP " + code + " — RPC 路径存在（未授权，说明路由可达）");
            }
            if (code >= 200 && code < 500) {
                return stepOk("HTTP " + code + " — RPC 路径可达");
            }
            return stepWarn("HTTP " + code + " — RPC 路径返回异常状态码");
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCodeValue();
            if (code == 401 || code == 403) {
                return stepOk("HTTP " + code + " — RPC 路径存在（未授权，说明路由可达）");
            }
            if (code == 404 || code == 405) {
                return stepFail("HTTP " + code + " — RPC 路径不存在（" + label + "）。Penpot 版本可能不支持此路径，或基址指向了错误的服务");
            }
            if (code == 502 || code == 503 || code == 504) {
                return stepWarn("HTTP " + code + " — RPC 路径网关错误（Penpot 后端可能未就绪或反代配置有误）");
            }
            return stepFail("HTTP " + code + " — RPC 路径异常");
        } catch (ResourceAccessException e) {
            return stepFail("网络错误：" + truncateMsg(e.getMessage(), 400));
        } catch (Exception e) {
            return stepFail("请求异常：" + truncateMsg(e.getMessage(), 400));
        }
    }

    private static String maskUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "(未配置)";
        }
        return uri.trim();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String trimSlash(String u) {
        if (u == null) return "";
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static String extractHost(String uri) {
        try {
            if (uri == null) return "(空)";
            String trimmed = uri.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                int idx = trimmed.indexOf("://") + 3;
                int slash = trimmed.indexOf('/', idx);
                if (slash < 0) slash = trimmed.length();
                return trimmed.substring(idx, slash);
            }
            return trimmed;
        } catch (Exception e) {
            return uri;
        }
    }

    private static String truncateMsg(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** 当前会话用户属于该租户的 aa_tenant_admin_user（管理员控制台）。协作「成员」无此会话，无法调用。 */
    private boolean isTenantAdminForCurrentTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        TenantAdminUser u = tenantAdminUserMapper.findById(userId);
        return u != null && u.getTenantId() != null && u.getTenantId().longValue() == tenantId.longValue();
    }
}

