package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.admin.dto.AdminLoginRequest;
import org.example.atuo_attend_backend.admin.dto.AdminLoginResponse;
import org.example.atuo_attend_backend.admin.model.AdminUser;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
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
        return ApiResponse.ok(resp);
    }

    @GetMapping("/me")
    public ApiResponse<AdminUser> me() {
        AdminUser admin = adminAuthService.currentAdmin();
        return ApiResponse.ok(admin);
    }
}

