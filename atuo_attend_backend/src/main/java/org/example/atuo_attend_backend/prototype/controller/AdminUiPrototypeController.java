package org.example.atuo_attend_backend.prototype.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectDetail;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectListItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectCreateRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectRenameRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateJobStatus;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateSpecRequest;
import org.example.atuo_attend_backend.prototype.service.UiPrototypeService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ui-prototype")
public class AdminUiPrototypeController {

    private final UiPrototypeService uiPrototypeService;

    public AdminUiPrototypeController(UiPrototypeService uiPrototypeService) {
        this.uiPrototypeService = uiPrototypeService;
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
                                                                 @RequestBody(required = false) UiPrototypeGenerateSpecRequest body) {
        String prompt = body != null ? body.getPrompt() : null;
        try {
            long jobId = uiPrototypeService.enqueueGenerateSpec(id, prompt);
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
}

