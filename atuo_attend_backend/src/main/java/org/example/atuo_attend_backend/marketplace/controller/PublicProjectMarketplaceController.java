package org.example.atuo_attend_backend.marketplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.marketplace.MarketplaceAccessService;
import org.example.atuo_attend_backend.tenant.domain.AdminSession;
import org.example.atuo_attend_backend.tenant.mapper.AdminSessionMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/project-marketplace")
public class PublicProjectMarketplaceController {

    private final MarketplaceAccessService marketplaceAccessService;
    private final AdminSessionMapper adminSessionMapper;

    public PublicProjectMarketplaceController(MarketplaceAccessService marketplaceAccessService,
                                              AdminSessionMapper adminSessionMapper) {
        this.marketplaceAccessService = marketplaceAccessService;
        this.adminSessionMapper = adminSessionMapper;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest request) {
        Long userId = request.getAttribute(AdminAuthFilter.ATTR_USER_ID) instanceof Long u ? u : null;
        Long tenantId = request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID) instanceof Long t ? t : null;
        if (userId == null || tenantId == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7).trim();
                if (!token.isEmpty()) {
                    AdminSession s = adminSessionMapper.findValidByToken(token);
                    if (s != null) {
                        userId = s.getUserId();
                        tenantId = s.getTenantId();
                    }
                }
            }
        }
        boolean adminAuth = userId != null && tenantId != null;
        long tid = tenantId != null ? tenantId : 0L;
        boolean visible = marketplaceAccessService.isVisibleToCaller(adminAuth, tid, userId);
        boolean moduleEnabled = marketplaceAccessService.moduleEnabledFlag();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleEnabled", moduleEnabled);
        data.put("visibleToCurrentPrincipal", visible);
        data.put("guestCanBrowse", marketplaceAccessService.isGuestBrowseAllowed());
        data.put("disclaimerVersion", marketplaceAccessService.disclaimerVersion());
        return ApiResponse.ok(data);
    }
}
