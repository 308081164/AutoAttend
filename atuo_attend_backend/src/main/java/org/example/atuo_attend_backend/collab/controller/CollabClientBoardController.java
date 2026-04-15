package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.auth.CollabAuthFilter;

import org.example.atuo_attend_backend.collab.service.ClientBoardShareService;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 协作端：客户阅览看板配置（需登录协作 JWT）。
 */
@RestController
@RequestMapping("/api/collab/projects")
public class CollabClientBoardController {

    private final CollabProjectService projectService;
    private final ClientBoardShareService shareService;

    public CollabClientBoardController(CollabProjectService projectService, ClientBoardShareService shareService) {
        this.projectService = projectService;
        this.shareService = shareService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    @GetMapping("/{projectId}/client-board")
    public ApiResponse<?> get(@PathVariable long projectId, HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req), CollabAuthFilter.phoneMemberIdsFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        return ApiResponse.ok(shareService.getBoardConfig(projectId));
    }

    @PutMapping("/{projectId}/client-board")
    public ApiResponse<?> put(@PathVariable long projectId,
                              @RequestBody(required = false) Map<String, Object> body,
                              HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req), CollabAuthFilter.phoneMemberIdsFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        try {
            return ApiResponse.ok(shareService.saveBoardConfig(projectId, tid(), body != null ? body : Map.of()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }
}
