package org.example.atuo_attend_backend.platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.platform.dto.PlatformComponentClickRequest;
import org.example.atuo_attend_backend.platform.service.PlatformComponentEventService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ops/events")
public class AdminOpsEventsController {

    private final PlatformComponentEventService eventService;

    public AdminOpsEventsController(PlatformComponentEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/component-click")
    public ApiResponse<Void> componentClick(@RequestBody(required = false) PlatformComponentClickRequest body,
                                            HttpServletRequest req) {
        if (body == null || body.getComponentKey() == null || body.getComponentKey().isBlank()) {
            return ApiResponse.error(40000, "componentKey required");
        }
        String componentKey = body.getComponentKey().trim();
        String coreApiKey = body.getCoreApiKey() != null ? body.getCoreApiKey().trim() : null;

        Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
        eventService.recordClick(adminUserId, adminPhone, componentKey, coreApiKey);
        return ApiResponse.ok(null);
    }
}

