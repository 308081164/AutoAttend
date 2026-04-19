package org.example.atuo_attend_backend.marketplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.marketplace.MarketplaceAccessService;
import org.example.atuo_attend_backend.marketplace.MarketplaceProjectService;
import org.example.atuo_attend_backend.marketplace.domain.MarketplaceProject;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/marketplace")
public class AdminMarketplaceController {

    private final MarketplaceProjectService marketplaceProjectService;
    private final MarketplaceAccessService marketplaceAccessService;

    public AdminMarketplaceController(MarketplaceProjectService marketplaceProjectService,
                                      MarketplaceAccessService marketplaceAccessService) {
        this.marketplaceProjectService = marketplaceProjectService;
        this.marketplaceAccessService = marketplaceAccessService;
    }

    private static long userId(HttpServletRequest request) {
        return (Long) request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
    }

    private static long tenantId(HttpServletRequest request) {
        return (Long) request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
    }

    /** 当前登录管理员对本模块的可见性与发布能力（供控制台入口展示） */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> moduleStatus(HttpServletRequest request) {
        long tid = tenantId(request);
        long uid = userId(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("platformEnabled", marketplaceAccessService.moduleEnabledFlag());
        data.put("tenantBrowseEnabled", marketplaceAccessService.isTenantBrowseAllowed(tid));
        data.put("tenantPublishEnabled", marketplaceAccessService.isTenantPublishAllowed(tid));
        data.put("moduleVisible", marketplaceAccessService.isModuleVisibleForAdmin(tid, uid));
        data.put("canPublish", marketplaceProjectService.canPublishProject(tid, uid));
        data.put("disclaimerVersion", marketplaceAccessService.disclaimerVersion());
        return ApiResponse.ok(data);
    }

    /** 须放在 /projects/{projectId} 之前，避免 "pending" 被当成 id */
    @GetMapping("/projects/pending")
    public ApiResponse<List<Map<String, Object>>> pending(HttpServletRequest request) {
        try {
            return ApiResponse.ok(marketplaceProjectService.listPending(tenantId(request), userId(request)));
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        }
    }

    @GetMapping("/projects")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String tech,
                                                 @RequestParam(required = false) String location,
                                                 @RequestParam(required = false, defaultValue = "newest") String sort,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false, defaultValue = "1") int page,
                                                 @RequestParam(required = false, defaultValue = "20") int pageSize,
                                                 HttpServletRequest request) {
        try {
            return ApiResponse.ok(marketplaceProjectService.list(tenantId(request), userId(request), q, tech, location, sort, status, page, pageSize));
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/projects/{projectId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long projectId, HttpServletRequest request) {
        try {
            return ApiResponse.ok(marketplaceProjectService.getDetail(tenantId(request), userId(request), projectId, true));
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40400, e.getMessage());
        }
    }

    @PostMapping("/projects")
    public ApiResponse<Map<String, Object>> create(@RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        try {
            MarketplaceProject created = marketplaceProjectService.create(tenantId(request), userId(request), body != null ? body : Map.of());
            return ApiResponse.ok(marketplaceProjectService.getDetail(tenantId(request), userId(request), created.getId(), false));
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PutMapping("/projects/{projectId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable long projectId,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        try {
            marketplaceProjectService.update(tenantId(request), userId(request), projectId, body != null ? body : Map.of());
            return ApiResponse.ok(marketplaceProjectService.getDetail(tenantId(request), userId(request), projectId, false));
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/projects/{projectId}/close")
    public ApiResponse<Void> close(@PathVariable long projectId, HttpServletRequest request) {
        try {
            marketplaceProjectService.close(tenantId(request), userId(request), projectId);
            return ApiResponse.ok(null);
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/me/published-projects")
    public ApiResponse<Map<String, Object>> myPublished(@RequestParam(required = false, defaultValue = "1") int page,
                                                      @RequestParam(required = false, defaultValue = "20") int pageSize,
                                                      HttpServletRequest request) {
        try {
            return ApiResponse.ok(marketplaceProjectService.listMine(tenantId(request), userId(request), page, pageSize));
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        }
    }

    @PostMapping("/projects/{projectId}/approve")
    public ApiResponse<Void> approve(@PathVariable long projectId, HttpServletRequest request) {
        try {
            marketplaceProjectService.approve(tenantId(request), userId(request), projectId);
            return ApiResponse.ok(null);
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/projects/{projectId}/reject")
    public ApiResponse<Void> reject(@PathVariable long projectId,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        try {
            String reason = body != null && body.get("reason") != null ? String.valueOf(body.get("reason")) : null;
            marketplaceProjectService.reject(tenantId(request), userId(request), projectId, reason);
            return ApiResponse.ok(null);
        } catch (MarketplaceProjectService.ForbiddenException e) {
            return ApiResponse.error(40300, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

}
