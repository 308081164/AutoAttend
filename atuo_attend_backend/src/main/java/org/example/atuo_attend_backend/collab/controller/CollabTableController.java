package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.auth.CollabAuthFilter;

import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/collab/projects")
public class CollabTableController {

    private final CollabProjectService projectService;
    private final CollabTableService tableService;

    public CollabTableController(CollabProjectService projectService, CollabTableService tableService) {
        this.projectService = projectService;
        this.tableService = tableService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @GetMapping("/{projectId}/tables")
    public ApiResponse<?> listTables(@PathVariable long projectId, HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        return ApiResponse.ok(tableService.listTableSummaries(projectId));
    }

    @GetMapping("/{projectId}/table")
    public ApiResponse<?> getTable(@PathVariable long projectId,
                                   @RequestParam(defaultValue = "issue_tracking") String purpose,
                                   HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        var table = tableService.getTableWithColumns(projectId, purpose);
        if (table == null) return ApiResponse.error(40400, "项目未绑定该用途的表格");
        return ApiResponse.ok(table);
    }
}
