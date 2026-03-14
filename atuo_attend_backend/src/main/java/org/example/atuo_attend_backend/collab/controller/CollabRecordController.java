package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.domain.BizRecord;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collab")
public class CollabRecordController {

    private final CollabProjectService projectService;
    private final CollabTableService tableService;
    private final CollabRecordService recordService;

    public CollabRecordController(CollabProjectService projectService,
                                  CollabTableService tableService,
                                  CollabRecordService recordService) {
        this.projectService = projectService;
        this.tableService = tableService;
        this.recordService = recordService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @GetMapping("/projects/{projectId}/records")
    public ApiResponse<?> listRecords(@PathVariable long projectId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "50") int pageSize,
                                     HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        BizProjectTable table = tableService.getTableByProjectId(projectId);
        if (table == null) return ApiResponse.error(40400, "项目未绑定表格");
        List<Map<String, Object>> items = recordService.listRecords(table.getId(), page, pageSize);
        long total = recordService.countRecords(table.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    @PostMapping("/projects/{projectId}/records")
    public ApiResponse<?> createRecord(@PathVariable long projectId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        BizProjectTable table = tableService.getTableByProjectId(projectId);
        if (table == null) return ApiResponse.error(40400, "项目未绑定表格");
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) body.get("fields");
        BizRecord record = recordService.createRecord(table.getId(), userId, fields);
        Map<String, Object> data = new HashMap<>();
        data.put("id", record.getId());
        data.put("createdAt", record.getCreatedAt());
        return ApiResponse.ok(data);
    }

    @GetMapping("/records/{recordId}")
    public ApiResponse<?> getRecord(@PathVariable long recordId, HttpServletRequest req) {
        long userId = requireUserId(req);
        long projectId = recordService.getProjectIdByRecordId(recordId);
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        Map<String, Object> detail = recordService.getRecordDetail(recordId);
        if (detail == null) return ApiResponse.error(40400, "记录不存在");
        return ApiResponse.ok(detail);
    }

    @PutMapping("/records/{recordId}")
    public ApiResponse<?> updateRecord(@PathVariable long recordId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest req) {
        long userId = requireUserId(req);
        long projectId = recordService.getProjectIdByRecordId(recordId);
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) body.get("fields");
        recordService.updateRecord(recordId, fields);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/records/{recordId}")
    public ApiResponse<?> deleteRecord(@PathVariable long recordId, HttpServletRequest req) {
        long userId = requireUserId(req);
        long projectId = recordService.getProjectIdByRecordId(recordId);
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        recordService.deleteRecord(recordId);
        return ApiResponse.ok(null);
    }
}
