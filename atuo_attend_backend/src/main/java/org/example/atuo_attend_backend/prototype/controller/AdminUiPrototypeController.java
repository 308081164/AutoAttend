package org.example.atuo_attend_backend.prototype.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.platform.service.PlatformComponentEventService;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectDetail;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectListItem;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectCreateRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeProjectRenameRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateJobStatus;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateSpecRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeGenerateMockupRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeImportQuoteRequirementRequest;
import org.example.atuo_attend_backend.prototype.dto.UiPrototypeSaveMockupMessagesRequest;
import org.example.atuo_attend_backend.quote.service.QuoteService;
import org.example.atuo_attend_backend.prototype.service.UiPrototypeService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ui-prototype")
public class AdminUiPrototypeController {

    private final UiPrototypeService uiPrototypeService;
    private final QuoteService quoteService;
    private final PlatformComponentEventService componentEventService;

    public AdminUiPrototypeController(UiPrototypeService uiPrototypeService,
                                      QuoteService quoteService,
                                      PlatformComponentEventService componentEventService) {
        this.uiPrototypeService = uiPrototypeService;
        this.quoteService = quoteService;
        this.componentEventService = componentEventService;
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
                                                                 @RequestBody(required = false) UiPrototypeGenerateSpecRequest body,
                                                                 HttpServletRequest req) {
        String prompt = body != null ? body.getPrompt() : null;
        try {
            long jobId = uiPrototypeService.enqueueGenerateSpec(id, prompt);

            // usage：实际调用核心接口（即“生成结构化 spec”）
            Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
            String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
            componentEventService.recordUsage(adminUserId, adminPhone, "hub_prototype", "ui_prototype_generate_spec");

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

    @GetMapping("/projects/{id}/mockup")
    public ApiResponse<Map<String, Object>> getMockup(@PathVariable long id) {
        UiPrototypeProjectDetail d = uiPrototypeService.getProjectDetail(id);
        if (d == null) return ApiResponse.error(40400, "项目不存在");
        org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockup m = uiPrototypeService.getMockup(id);
        Map<String, Object> data = new HashMap<>();
        if (m != null) {
            data.put("html", m.getHtml());
            data.put("css", m.getCss());
            data.put("rawAiContent", m.getRawAiContent());
            data.put("messagesJson", m.getMessagesJson());
            data.put("modelUsed", m.getModelUsed());
            data.put("repaired", m.isRepaired());
            data.put("updatedAt", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
        } else {
            data.put("html", "");
            data.put("css", "");
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/projects/{id}/mockup/messages")
    public ApiResponse<Void> saveMockupMessages(@PathVariable long id,
                                                @RequestBody(required = false) UiPrototypeSaveMockupMessagesRequest body) {
        if (uiPrototypeService.getProjectDetail(id) == null) {
            return ApiResponse.error(40400, "项目不存在");
        }
        String messagesJson = body != null ? body.getMessagesJson() : null;
        uiPrototypeService.saveMockupMessages(id, messagesJson);
        return ApiResponse.ok(null);
    }

    /**
     * HTML+CSS mockup：异步生成任务（与 mvp-vue 体验一致，但用 jobId 轮询避免 504）。
     */
    @PostMapping("/projects/{id}/mockups/generate")
    public ApiResponse<Map<String, Object>> enqueueGenerateMockup(@PathVariable long id,
                                                                  @RequestBody(required = false) UiPrototypeGenerateMockupRequest body,
                                                                  HttpServletRequest req) {
        String prompt = body != null ? body.getPrompt() : null;
        String model = body != null ? body.getModel() : null;
        String messagesJson = body != null ? body.getMessagesJson() : null;
        try {
            long jobId = uiPrototypeService.enqueueGenerateMockup(id, prompt, model, messagesJson);

            // usage：实际调用核心接口（即“生成 HTML/CSS mockup”）
            Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
            String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
            componentEventService.recordUsage(adminUserId, adminPhone, "hub_prototype", "ui_prototype_generate_mockup");

            Map<String, Object> data = new HashMap<>();
            data.put("jobId", jobId);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    @GetMapping("/projects/{id}/mockups/jobs/{jobId}")
    public ApiResponse<Map<String, Object>> getMockupGenerateJob(@PathVariable long id, @PathVariable long jobId) {
        org.example.atuo_attend_backend.prototype.domain.UiPrototypeMockupGenerateJob j =
                uiPrototypeService.getMockupGenerateJobStatus(id, jobId);
        if (j == null) return ApiResponse.error(40400, "任务不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", j.getId());
        data.put("status", j.getStatus());
        data.put("errorMessage", j.getErrorMessage());
        data.put("createdAt", j.getCreatedAt() != null ? j.getCreatedAt().toString() : null);
        data.put("updatedAt", j.getUpdatedAt() != null ? j.getUpdatedAt().toString() : null);
        return ApiResponse.ok(data);
    }

    /**
     * 从“报价项目”导入结构化需求，转换为适合“快原型”页面需求框的文本。
     */
    @PostMapping("/projects/{id}/import-quote-requirement")
    public ApiResponse<Map<String, Object>> importQuoteRequirement(@PathVariable long id,
                                                                   @RequestBody(required = false) UiPrototypeImportQuoteRequirementRequest body) {
        try {
            if (uiPrototypeService.getProjectDetail(id) == null) {
                return ApiResponse.error(40400, "快原型项目不存在");
            }
            Long quoteProjectId = body != null ? body.getQuoteProjectId() : null;
            if (quoteProjectId == null || quoteProjectId <= 0) {
                return ApiResponse.error(40000, "quoteProjectId 不能为空");
            }
            return ApiResponse.ok(quoteService.buildPrototypeRequirementFromQuote(quoteProjectId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }
}

