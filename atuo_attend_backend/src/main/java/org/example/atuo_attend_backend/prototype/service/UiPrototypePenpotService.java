package org.example.atuo_attend_backend.prototype.service;

import org.example.atuo_attend_backend.prototype.config.PenpotProperties;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypePenpotJob;
import org.example.atuo_attend_backend.prototype.domain.UiPrototypeProject;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypePenpotJobStatus;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypePenpotJobMapper;
import org.example.atuo_attend_backend.prototype.mapper.UiPrototypeProjectMapper;
import org.example.atuo_attend_backend.prototype.penpot.PenpotRpcClient;
import org.example.atuo_attend_backend.prototype.penpot.PenpotWorkspaceService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class UiPrototypePenpotService {

    private static final Logger log = LoggerFactory.getLogger(UiPrototypePenpotService.class);

    private final PenpotProperties penpotProperties;
    private final PenpotRpcClient penpotRpcClient;
    private final PenpotWorkspaceService workspaceService;
    private final UiPrototypeProjectMapper projectMapper;
    private final UiPrototypePenpotJobMapper penpotJobMapper;

    public UiPrototypePenpotService(PenpotProperties penpotProperties,
                                    PenpotRpcClient penpotRpcClient,
                                    PenpotWorkspaceService workspaceService,
                                    UiPrototypeProjectMapper projectMapper,
                                    UiPrototypePenpotJobMapper penpotJobMapper) {
        this.penpotProperties = penpotProperties;
        this.penpotRpcClient = penpotRpcClient;
        this.workspaceService = workspaceService;
        this.projectMapper = projectMapper;
        this.penpotJobMapper = penpotJobMapper;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public boolean isFeatureEnabled() {
        return penpotProperties.isEnabled() && penpotRpcClient.isConfigured();
    }

    /**
     * 轻量探活：调用 get-teams（需有效 Token/Cookie）。仅用于 ?probe=1，避免每次页面加载都打 Penpot。
     */
    public boolean probeConnection() {
        if (!isFeatureEnabled()) {
            return false;
        }
        try {
            penpotRpcClient.command("get-teams", new java.util.HashMap<>());
            return true;
        } catch (Exception e) {
            log.warn("Penpot probe failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isConfiguredOnly() {
        return penpotRpcClient.isConfigured();
    }

    public long enqueueCreateFile(long projectId, String note) {
        if (!isFeatureEnabled()) {
            throw new IllegalStateException("Penpot 未启用或未配置 access-token/账号");
        }
        long tenantId = tid();
        UiPrototypeProject p = projectMapper.findById(tenantId, projectId);
        if (p == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        String snapshot = note != null && !note.isBlank() ? note.trim() : "(penpot placeholder file)";
        if (snapshot.length() > 60000) {
            snapshot = snapshot.substring(0, 60000) + "…";
        }
        UiPrototypePenpotJob row = new UiPrototypePenpotJob();
        row.setTenantId(tenantId);
        row.setProjectId(projectId);
        row.setStatus("pending");
        row.setPromptSnapshot(snapshot);
        penpotJobMapper.insert(row);
        Long jobId = row.getId();
        if (jobId == null) {
            throw new IllegalStateException("创建 Penpot 任务失败");
        }
        CompletableFuture.runAsync(() -> runJobSafe(tenantId, jobId, projectId, p.getName()));
        return jobId;
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
        s.setErrorMessage(j.getErrorMessage());
        s.setPenpotFileId(j.getPenpotFileId());
        s.setPenpotPreviewUrl(j.getPenpotPreviewUrl());
        return s;
    }

    private void runJobSafe(long tenantId, long jobId, long projectId, String projectName) {
        try {
            TenantContext.runWithTenantId(tenantId, () -> runJob(jobId, projectId, projectName));
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

    private void runJob(long jobId, long projectId, String projectName) {
        long tenantId = tid();
        penpotJobMapper.updateStatus(jobId, tenantId, projectId, "running");
        UiPrototypeProject p = projectMapper.findById(tenantId, projectId);
        if (p == null) {
            penpotJobMapper.updateFailed(jobId, tenantId, projectId, "项目不存在");
            return;
        }
        try {
            PenpotWorkspaceService.TeamProject tp = workspaceService.resolveTeamAndProject(
                    p.getPenpotTeamId(), p.getPenpotProjectId(), projectName != null ? projectName : ("project-" + projectId));
            String fileName = "快原型-" + projectId + "-v" + System.currentTimeMillis();
            String fileId = workspaceService.createDesignFile(tp.projectId(), fileName);
            String previewUrl = workspaceService.buildWorkspaceUrl(tp.projectId(), fileId);

            projectMapper.updatePenpotBinding(tenantId, projectId, tp.teamId(), tp.projectId(), fileId, previewUrl);
            penpotJobMapper.updateSuccess(jobId, tenantId, projectId, fileId, previewUrl);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Penpot 创建失败";
            penpotJobMapper.updateFailed(jobId, tenantId, projectId, msg);
        }
    }
}
