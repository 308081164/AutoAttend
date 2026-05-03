package org.example.atuo_attend_backend.prototype.service;

import org.example.atuo_attend_backend.prototype.config.PenpotProperties;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypePenpotJob;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeProject;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypePenpotJobStatus;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypePenpotJobMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeProjectMapper;
import org.example.atuo_attend_backend.prototype.penpot.PenpotFileWriterService;
import org.example.atuo_attend_backend.prototype.penpot.PenpotPlannerService;
import org.example.atuo_attend_backend.prototype.penpot.PenpotRpcClient;
import org.example.atuo_attend_backend.prototype.penpot.PenpotWorkspaceService;
import org.example.atuo_attend_backend.prototype.penpot.TenantPenpotOnboardingService;
import org.example.atuo_attend_backend.prototype.penpot.dto.PenpotLayoutPlan;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class UiPrototypePenpotService {

    private static final Logger log = LoggerFactory.getLogger(UiPrototypePenpotService.class);
    private static final int MAX_WRITE_ATTEMPTS = 3;

    private final PenpotProperties penpotProperties;
    private final PenpotRpcClient penpotRpcClient;
    private final PenpotWorkspaceService workspaceService;
    private final PenpotPlannerService plannerService;
    private final PenpotFileWriterService fileWriterService;
    private final UiPrototypeProjectMapper projectMapper;
    private final UiPrototypePenpotJobMapper penpotJobMapper;
    private final TenantPenpotOnboardingService tenantPenpotOnboardingService;
    private final Executor penpotTaskExecutor;

    public UiPrototypePenpotService(PenpotProperties penpotProperties,
                                    PenpotRpcClient penpotRpcClient,
                                    PenpotWorkspaceService workspaceService,
                                    PenpotPlannerService plannerService,
                                    PenpotFileWriterService fileWriterService,
                                    UiPrototypeProjectMapper projectMapper,
                                    UiPrototypePenpotJobMapper penpotJobMapper,
                                    TenantPenpotOnboardingService tenantPenpotOnboardingService,
                                    @Qualifier("penpotTaskExecutor") Executor penpotTaskExecutor) {
        this.penpotTaskExecutor = penpotTaskExecutor;
        this.penpotProperties = penpotProperties;
        this.penpotRpcClient = penpotRpcClient;
        this.workspaceService = workspaceService;
        this.plannerService = plannerService;
        this.fileWriterService = fileWriterService;
        this.projectMapper = projectMapper;
        this.penpotJobMapper = penpotJobMapper;
        this.tenantPenpotOnboardingService = tenantPenpotOnboardingService;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public boolean isFeatureEnabled() {
        return penpotProperties.isEnabled();
    }

    /**
     * 使用当前租户已落库的 Access Token 探测 Penpot（不会在探测时自动开户）。
     */
    public boolean probeConnection() {
        if (!penpotProperties.isEnabled()) {
            return false;
        }
        long tenantId = tid();
        if (!tenantPenpotOnboardingService.hasCredential(tenantId)) {
            return false;
        }
        try {
            String cookie = tenantPenpotOnboardingService.getOrCreateTenantCookie(tenantId);
            penpotRpcClient.commandWithTenantCookie("get-teams", new HashMap<>(), cookie);
            return true;
        } catch (Exception e) {
            log.warn("Penpot probe failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isConfiguredOnly() {
        return penpotProperties.isEnabled();
    }

    public boolean isTenantPenpotReady(long tenantId) {
        return tenantPenpotOnboardingService.hasCredential(tenantId);
    }

    /** 主管理员首次进入时完成 Penpot 注册并发牌。 */
    public void ensureTenantPenpotOnboarded() {
        if (!penpotProperties.isEnabled()) {
            throw new IllegalStateException("Penpot 未启用");
        }
        tenantPenpotOnboardingService.getOrCreateTenantAccessToken(tid());
    }

    /**
     * 完整链路：LLM 规划 → 创建文件 → update-file 写入 → export-binfile。
     */
    public long enqueueGenerate(long projectId, String prompt, String note) {
        if (!penpotProperties.isEnabled()) {
            throw new IllegalStateException("Penpot 未启用");
        }
        long tenantId = tid();
        String tenantCookie = tenantPenpotOnboardingService.getOrCreateTenantCookie(tenantId);
        UiPrototypeProject p = projectMapper.findById(tenantId, projectId);
        if (p == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        String snapshot = buildSnapshot(prompt, note);
        UiPrototypePenpotJob row = new UiPrototypePenpotJob();
        row.setTenantId(tenantId);
        row.setProjectId(projectId);
        row.setStatus("pending");
        row.setStage("queued");
        row.setProgress(0);
        row.setPromptSnapshot(snapshot);
        penpotJobMapper.insert(row);
        Long jobId = row.getId();
        if (jobId == null) {
            throw new IllegalStateException("创建 Penpot 任务失败");
        }
        CompletableFuture.runAsync(() -> runJobSafe(tenantId, jobId, projectId, p.getName(), prompt, note, tenantCookie),
                penpotTaskExecutor);
        return jobId;
    }

    private static String buildSnapshot(String prompt, String note) {
        String p = prompt != null ? prompt.trim() : "";
        if (!p.isEmpty()) {
            return truncate(p, 60000);
        }
        String n = note != null ? note.trim() : "";
        if (!n.isEmpty()) {
            return truncate(n, 60000);
        }
        return "(empty prompt)";
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    public UiPrototypePenpotJobStatus getJobStatus(long projectId, long jobId) {
        long tenantId = tid();
        UiPrototypePenpotJob j = penpotJobMapper.findById(jobId, tenantId, projectId);
        if (j == null) {
            return null;
        }
        UiPrototypePenpotJobStatus s = new UiPrototypePenpotJobStatus();
        s.setJobId(j.getId());
        s.setStatus(j.getStatus());
        s.setStage(j.getStage());
        s.setProgress(j.getProgress());
        s.setRetryCount(j.getRetryCount());
        s.setErrorMessage(j.getErrorMessage());
        s.setPenpotFileId(j.getPenpotFileId());
        s.setPenpotPreviewUrl(j.getPenpotPreviewUrl());
        s.setExportBinfileUrl(j.getExportBinfileUrl());
        return s;
    }

    /** 返回 export-binfile 临时 URL（需已绑定 penpot_file_id） */
    public String exportBinfileForProject(long projectId) {
        if (!penpotProperties.isEnabled()) {
            throw new IllegalStateException("Penpot 未启用");
        }
        long tenantId = tid();
        String tenantCookie = tenantPenpotOnboardingService.getOrCreateTenantCookie(tenantId);
        UiPrototypeProject p = projectMapper.findById(tenantId, projectId);
        if (p == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (!StringUtils.hasText(p.getPenpotFileId())) {
            throw new IllegalStateException("尚未生成 Penpot 文件");
        }
        return exportBinfile(p.getPenpotFileId(), tenantCookie);
    }

    private String exportBinfile(String fileId, String tenantCookie) {
        Map<String, Object> body = new HashMap<>();
        body.put("fileId", fileId);
        body.put("includeLibraries", false);
        body.put("embedAssets", true);
        String url = penpotRpcClient.commandForStringResult("export-binfile", body, tenantCookie);
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("export-binfile 未返回下载地址");
        }
        return url.trim();
    }

    private void runJobSafe(long tenantId, long jobId, long projectId, String projectName, String prompt, String note,
                            String tenantCookie) {
        try {
            TenantContext.runWithTenantId(tenantId, () -> runJob(jobId, projectId, projectName, prompt, note, tenantCookie));
        } catch (Throwable t) {
            log.error("penpot job {} crashed", jobId, t);
            try {
                TenantContext.runWithTenantId(tenantId, () -> {
                    String msg = t.getMessage() != null ? t.getMessage() : "内部错误";
                    penpotJobMapper.updateFailed(jobId, tenantId, projectId, msg);
                });
            } catch (Exception e) {
                log.warn("update penpot job failed: {}", e.getMessage());
            }
        }
    }

    private void runJob(long jobId, long projectId, String projectName, String prompt, String note, String tenantCookie) {
        long tenantId = tid();
        penpotJobMapper.updateStage(jobId, tenantId, projectId, "running", "planning", 10, null);

        String reqText = StringUtils.hasText(prompt) ? prompt.trim() : (note != null ? note.trim() : "");
        if (!StringUtils.hasText(reqText)) {
            reqText = "请根据项目名称生成信息架构草图。";
        }

        PenpotLayoutPlan plan;
        try {
            plan = plannerService.plan(reqText, projectName);
        } catch (Exception e) {
            penpotJobMapper.updateFailed(jobId, tenantId, projectId,
                    "规划失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            return;
        }

        penpotJobMapper.updateStage(jobId, tenantId, projectId, "running", "creating", 30, null);

        UiPrototypeProject p = projectMapper.findById(tenantId, projectId);
        if (p == null) {
            penpotJobMapper.updateFailed(jobId, tenantId, projectId, "项目不存在");
            return;
        }

        PenpotWorkspaceService.TeamProject tp;
        try {
            tp = workspaceService.resolveTeamAndProject(
                    p.getPenpotTeamId(), p.getPenpotProjectId(),
                    projectName != null ? projectName : ("project-" + projectId),
                    tenantCookie);
        } catch (Exception e) {
            penpotJobMapper.updateFailed(jobId, tenantId, projectId,
                    "Penpot 团队/项目：" + (e.getMessage() != null ? e.getMessage() : ""));
            return;
        }

        String fileName = "快原型-" + projectId + "-" + System.currentTimeMillis();
        String fileId;
        try {
            fileId = workspaceService.createDesignFile(tp.projectId(), fileName, tenantCookie);
        } catch (Exception e) {
            penpotJobMapper.updateFailed(jobId, tenantId, projectId,
                    "创建文件失败：" + (e.getMessage() != null ? e.getMessage() : ""));
            return;
        }

        penpotJobMapper.updateStage(jobId, tenantId, projectId, "running", "writing", 50, null);

        Exception lastWrite = null;
        for (int attempt = 1; attempt <= MAX_WRITE_ATTEMPTS; attempt++) {
            try {
                fileWriterService.applyLayout(fileId, plan, tenantCookie);
                lastWrite = null;
                break;
            } catch (Exception e) {
                lastWrite = e;
                penpotJobMapper.incrementRetry(jobId, tenantId, projectId);
                log.warn("penpot update-file attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        if (lastWrite != null) {
            penpotJobMapper.updateFailed(jobId, tenantId, projectId,
                    "写入画布失败（已重试 " + MAX_WRITE_ATTEMPTS + " 次）："
                            + (lastWrite.getMessage() != null ? lastWrite.getMessage() : ""));
            return;
        }

        penpotJobMapper.updateStage(jobId, tenantId, projectId, "running", "exporting", 85, null);

        String previewUrl = workspaceService.buildWorkspaceUrl(tp.projectId(), fileId);
        String exportUrl = null;
        try {
            exportUrl = exportBinfile(fileId, tenantCookie);
        } catch (Exception e) {
            log.warn("export-binfile skipped: {}", e.getMessage());
        }

        projectMapper.updatePenpotBinding(tenantId, projectId, tp.teamId(), tp.projectId(), fileId, previewUrl, exportUrl);
        penpotJobMapper.updateSuccess(jobId, tenantId, projectId, fileId, previewUrl, exportUrl);
    }
}
