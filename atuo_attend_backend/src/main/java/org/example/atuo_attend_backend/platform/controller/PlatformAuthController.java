package org.example.atuo_attend_backend.platform.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.platform.dto.PlatformLoginRequest;
import org.example.atuo_attend_backend.platform.dto.PlatformLoginResponse;
import org.example.atuo_attend_backend.platform.service.PlatformAuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/auth")
public class PlatformAuthController {

    private final PlatformAuthService platformAuthService;

    public PlatformAuthController(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<PlatformLoginResponse> login(@RequestBody PlatformLoginRequest request) {
        String token = platformAuthService.login(request != null ? request.getPassword() : null);
        if (token == null) {
            return ApiResponse.error(40100, "invalid password or platform login disabled");
        }
        PlatformLoginResponse resp = new PlatformLoginResponse();
        resp.setToken(token);
        resp.setExpiresIn(platformAuthService.getExpiresInSeconds());
        return ApiResponse.ok(resp);
    }
}
