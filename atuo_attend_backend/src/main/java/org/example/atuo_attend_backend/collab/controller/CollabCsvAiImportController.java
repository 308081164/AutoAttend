package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.service.CollabCsvAiImportService;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 协作表：CSV + DeepSeek 智能清洗预览（分片 ≤30 行/批），落库复用 {@link CollabAiTaskController#commit}。
 */
@RestController
@RequestMapping("/api/collab")
public class CollabCsvAiImportController {

    private final CollabProjectService projectService;
    private final CollabCsvAiImportService csvAiImportService;

    public CollabCsvAiImportController(CollabProjectService projectService,
                                     CollabCsvAiImportService csvAiImportService) {
        this.projectService = projectService;
        this.csvAiImportService = csvAiImportService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @PostMapping(value = "/projects/{projectId}/csv-ai-import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> previewCsv(@PathVariable long projectId,
                                                        @RequestParam("file") MultipartFile file,
                                                        HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        try {
            return ApiResponse.ok(csvAiImportService.previewFromCsv(projectId, file));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, "处理失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
