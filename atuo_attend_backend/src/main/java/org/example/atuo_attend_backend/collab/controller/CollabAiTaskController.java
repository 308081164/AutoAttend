package org.example.atuo_attend_backend.collab.controller;


import org.example.atuo_attend_backend.collab.auth.CollabAccessContext;
import org.example.atuo_attend_backend.collab.auth.CollabAuthFilter;

import org.example.atuo_attend_backend.collab.dto.CollabAiTaskModels;
import org.example.atuo_attend_backend.collab.service.CollabAiTaskIngestService;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 协作任务表 AI 录入模式：调用通义·千问多模态，将自然语言 + 截图整理为任务草稿。
 */
@RestController
@RequestMapping("/api/collab")
public class CollabAiTaskController {

    private final CollabProjectService projectService;
    private final CollabAiTaskIngestService ingestService;

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public CollabAiTaskController(CollabProjectService projectService, CollabAiTaskIngestService ingestService) {
        this.projectService = projectService;
        this.ingestService = ingestService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @PostMapping("/projects/{projectId}/ai-tasks/preview")
    public ApiResponse<?> preview(@PathVariable long projectId,
                                  @RequestParam(defaultValue = "issue_tracking") String purpose,
                                  @RequestBody CollabAiTaskModels.AiTaskPreviewRequest body,
                                  HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(CollabAccessContext.from(req), projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        return ingestService.preview(tid(), projectId, purpose, body);
    }

    @PostMapping("/projects/{projectId}/ai-tasks/commit")
    public ApiResponse<?> commit(@PathVariable long projectId,
                                 @RequestParam(defaultValue = "issue_tracking") String purpose,
                                 @RequestBody CollabAiTaskModels.AiTaskCommitRequest body,
                                 HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(CollabAccessContext.from(req), projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        return ingestService.commit(tid(), projectId, purpose, userId, null, body);
    }
}
