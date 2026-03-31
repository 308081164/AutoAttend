package org.example.atuo_attend_backend.prototype.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectDetail;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectListItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectCreateRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectRenameRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeSpecGenerateResult;
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

    @PostMapping("/projects/{id}/specs/generate")
    public ApiResponse<UiPrototypeSpecGenerateResult> generateSpec(@PathVariable long id,
                                                                     @RequestBody(required = false) UiPrototypeGenerateSpecRequest body) {
        String prompt = body != null ? body.getPrompt() : null;
        UiPrototypeSpecGenerateResult r = uiPrototypeService.generateSpec(id, prompt);
        return ApiResponse.ok(r);
    }
}

