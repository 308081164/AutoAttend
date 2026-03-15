package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.admin.dto.AdminLoginRequest;
import org.example.atuo_attend_backend.admin.dto.AdminLoginResponse;
import org.example.atuo_attend_backend.admin.model.AdminUser;
import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final CollabAuthService collabAuthService;

    public AdminAuthController(AdminAuthService adminAuthService, CollabAuthService collabAuthService) {
        this.adminAuthService = adminAuthService;
        this.collabAuthService = collabAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        String token = adminAuthService.login(request.getUsername(), request.getPassword());
        if (token == null) {
            return ApiResponse.error(40100, "invalid username or password");
        }
        AdminLoginResponse resp = new AdminLoginResponse();
        resp.setToken(token);
        resp.setExpiresIn(7200);
        resp.setCollabToken(collabAuthService.issueCollabTokenForAdmin());
        return ApiResponse.ok(resp);
    }

    @GetMapping("/me")
    public ApiResponse<AdminUser> me() {
        AdminUser admin = adminAuthService.currentAdmin();
        return ApiResponse.ok(admin);
    }
}

