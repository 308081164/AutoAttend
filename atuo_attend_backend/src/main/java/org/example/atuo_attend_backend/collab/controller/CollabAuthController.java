package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.dto.CollabLoginRequest;
import org.example.atuo_attend_backend.collab.dto.CollabLoginResponse;
import org.example.atuo_attend_backend.collab.dto.CollabRegisterInviteRequest;
import org.example.atuo_attend_backend.collab.dto.CollabUserDto;
import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/collab/auth")
public class CollabAuthController {

    private final CollabAuthService authService;

    public CollabAuthController(CollabAuthService authService) {
        this.authService = authService;
    }

    /**
     * 通过组织邀请令牌注册协作成员并登录。
     */
    @PostMapping("/register-invite")
    public ApiResponse<CollabLoginResponse> registerInvite(@RequestBody CollabRegisterInviteRequest request) {
        try {
            String token = authService.registerByInvite(
                    request.getToken(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getName());
            if (token == null) {
                return ApiResponse.error(40000, "邀请无效或已过期");
            }
            CollabLoginResponse resp = new CollabLoginResponse();
            resp.setToken(token);
            resp.setExpiresIn(7200);
            return ApiResponse.ok(resp);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage() != null ? e.getMessage() : "注册失败");
        }
    }

    @PostMapping("/login")
    public ApiResponse<CollabLoginResponse> login(@RequestBody CollabLoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword(), request.getTenantId());
        if (token == null) {
            return ApiResponse.error(40100, "邮箱或密码错误");
        }
        CollabLoginResponse resp = new CollabLoginResponse();
        resp.setToken(token);
        resp.setExpiresIn(7200);
        return ApiResponse.ok(resp);
    }

    @GetMapping("/me")
    public ApiResponse<CollabUserDto> me(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("collabUserId");
        if (userId == null) {
            return ApiResponse.error(40101, "未登录");
        }
        BizUser user = authService.getCurrentUser(userId);
        if (user == null) {
            return ApiResponse.error(40400, "用户不存在");
        }
        CollabUserDto dto = new CollabUserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        return ApiResponse.ok(dto);
    }
}
