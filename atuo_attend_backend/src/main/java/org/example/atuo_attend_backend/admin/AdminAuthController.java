package org.example.atuo_attend_backend.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.admin.dto.*;
import org.example.atuo_attend_backend.admin.model.AdminUser;
import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final CollabAuthService collabAuthService;
    private final TenantMapper tenantMapper;

    public AdminAuthController(AdminAuthService adminAuthService,
                               CollabAuthService collabAuthService,
                               TenantMapper tenantMapper) {
        this.adminAuthService = adminAuthService;
        this.collabAuthService = collabAuthService;
        this.tenantMapper = tenantMapper;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        String account = request.resolveLoginAccount();
        AdminAuthOutcome out = adminAuthService.login(account, request.getPassword());
        if (out == null) {
            return ApiResponse.error(40100, "invalid phone or password");
        }
        return ApiResponse.ok(buildLoginResponse(out));
    }

    @PostMapping("/register")
    public ApiResponse<AdminLoginResponse> register(@RequestBody AdminRegisterRequest request) {
        try {
            AdminAuthOutcome out = adminAuthService.register(request);
            return ApiResponse.ok(buildLoginResponse(out));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40001, e.getMessage());
        }
    }

    @GetMapping("/me")
    public ApiResponse<AdminUser> me() {
        AdminUser admin = adminAuthService.currentAdmin();
        return ApiResponse.ok(admin);
    }

    @GetMapping("/collab-token")
    public ApiResponse<AdminLoginResponse> collabToken(HttpServletRequest request) {
        adminAuthService.currentAdmin();
        String phone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        AdminLoginResponse resp = new AdminLoginResponse();
        resp.setCollabToken(collabAuthService.issueCollabTokenForPhone(phone));
        return ApiResponse.ok(resp);
    }

    private AdminLoginResponse buildLoginResponse(AdminAuthOutcome out) {
        AdminLoginResponse resp = new AdminLoginResponse();
        resp.setToken(out.getToken());
        resp.setExpiresIn(out.getExpiresInSeconds());
        resp.setPhone(out.getPhone());
        Tenant t = tenantMapper.findById(out.getTenantId());
        if (t != null) {
            resp.setTenantId(t.getId());
            resp.setTenantName(t.getName());
            resp.setSlug(t.getSlug());
        }
        resp.setCollabToken(collabAuthService.issueCollabTokenForPhone(out.getPhone()));
        return resp;
    }
}
