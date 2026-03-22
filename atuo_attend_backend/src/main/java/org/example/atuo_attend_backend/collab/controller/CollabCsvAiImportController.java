package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.dto.CollabCsvChunkRequest;
import org.example.atuo_attend_backend.collab.service.CollabCsvAiImportService;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * CSV + DeepSeek：先 {@link #createSession} 解析入内存，再按批 {@link #processChunk}，避免单次请求内多次 AI 调用导致网关 504。
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

    /** 上传 CSV，服务端解析并创建会话（不调用 AI） */
    @PostMapping(value = "/projects/{projectId}/csv-ai-import/session", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> createSession(@PathVariable long projectId,
                                                          @RequestParam("file") MultipartFile file,
                                                          HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        try {
            return ApiResponse.ok(csvAiImportService.createImportSession(userId, projectId, file));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, "处理失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /** 处理一批（单次 DeepSeek），chunkIndex 从 0 开始 */
    @PostMapping(value = "/projects/{projectId}/csv-ai-import/session/{sessionId}/chunk", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> processChunk(@PathVariable long projectId,
                                                         @PathVariable String sessionId,
                                                         @RequestBody(required = false) CollabCsvChunkRequest body,
                                                         HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        if (body == null) {
            body = new CollabCsvChunkRequest();
        }
        try {
            return ApiResponse.ok(csvAiImportService.processChunk(userId, projectId, sessionId, body.getChunkIndex()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, "处理失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @DeleteMapping("/projects/{projectId}/csv-ai-import/session/{sessionId}")
    public ApiResponse<Void> discardSession(@PathVariable long projectId,
                                            @PathVariable String sessionId,
                                            HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        csvAiImportService.discardSession(userId, projectId, sessionId);
        return ApiResponse.ok(null);
    }
}
