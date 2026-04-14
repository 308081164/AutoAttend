package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.admin.PhoneNormalizer;
import org.example.atuo_attend_backend.admin.sms.AdminSmsProperties;
import org.example.atuo_attend_backend.admin.sms.AdminSmsService;
import org.example.atuo_attend_backend.collab.auth.CollabAuthFilter;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.dto.*;
import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/collab/auth")
public class CollabAuthController {

    private final CollabAuthService authService;
    private final AdminSmsService adminSmsService;
    private final AdminSmsProperties adminSmsProperties;
    private final TenantAdminUserMapper tenantAdminUserMapper;

    public CollabAuthController(CollabAuthService authService,
                                AdminSmsService adminSmsService,
                                AdminSmsProperties adminSmsProperties,
                                TenantAdminUserMapper tenantAdminUserMapper) {
        this.authService = authService;
        this.adminSmsService = adminSmsService;
        this.adminSmsProperties = adminSmsProperties;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
    }

    /**
     * 协作端短信配置（与管理员共用阿里云通道；成员可发 bind_phone / 已绑手机 login）。
     */
    @GetMapping("/sms/config")
    public ApiResponse<Map<String, Object>> smsConfig() {
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", adminSmsService.smsLoginEnabled());
        m.put("resendIntervalSeconds", adminSmsProperties.getResendIntervalSeconds());
        m.put("maxSendsPerPhonePerDay", adminSmsProperties.getMaxSendsPerPhonePerDay());
        return ApiResponse.ok(m);
    }

    /** 成员已绑手机：登录前发送验证码（无需登录） */
    @PostMapping("/sms/send-login")
    public ApiResponse<Void> sendLoginSms(@RequestBody CollabSmsSendRequest body) {
        try {
            adminSmsService.sendCode(body.getPhone(), AdminSmsService.PURPOSE_LOGIN);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40001, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(40002, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "短信发送失败";
            return ApiResponse.error(50001, msg);
        }
    }

    /** 绑定手机：需已登录协作账号 */
    @PostMapping("/sms/send")
    public ApiResponse<Void> sendBindSms(@RequestBody CollabSmsSendRequest body, HttpServletRequest req) {
        CollabAuthFilter.requireCollabUserId(req);
        try {
            String phone = PhoneNormalizer.normalize(body.getPhone());
            if (phone == null) {
                return ApiResponse.error(40001, "手机号格式不正确");
            }
            if (tenantAdminUserMapper.findByPhone(phone) == null) {
                return ApiResponse.error(40001, "该手机号未注册管理员账号，无法完成关联绑定");
            }
            adminSmsService.sendCode(body.getPhone(), AdminSmsService.PURPOSE_BIND_PHONE);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40001, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(40002, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "短信发送失败";
            return ApiResponse.error(50001, msg);
        }
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

    /** 成员登录：邮箱+密码，或已绑手机时手机号+短信验证码 */
    @PostMapping("/login")
    public ApiResponse<CollabLoginResponse> login(@RequestBody CollabLoginRequest request) {
        String token = authService.loginMemberExplicit(
                request.getEmail(),
                request.getPassword(),
                request.getSmsCode(),
                request.getTenantId());
        if (token == null) {
            return ApiResponse.error(40100, "账号或密码错误，或需使用短信验证码登录");
        }
        CollabLoginResponse resp = new CollabLoginResponse();
        resp.setToken(token);
        resp.setExpiresIn(7200);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody CollabChangePasswordRequest body, HttpServletRequest req) {
        long userId = CollabAuthFilter.requireCollabUserId(req);
        try {
            authService.changePasswordForCollabUser(userId, body.getCurrentPassword(), body.getNewPassword());
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/bind-phone")
    public ApiResponse<Void> bindPhone(@RequestBody CollabBindPhoneRequest body, HttpServletRequest req) {
        long userId = CollabAuthFilter.requireCollabUserId(req);
        try {
            authService.bindPhoneForMember(userId, body.getPhone(), body.getSmsCode());
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
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
        dto.setPhoneE164(user.getPhoneE164());
        boolean bound = user.getPhoneE164() != null && !user.getPhoneE164().isBlank();
        dto.setPhoneBound(bound);
        dto.setLinkedTenantAdminUserId(user.getLinkedTenantAdminUserId());
        return ApiResponse.ok(dto);
    }
}
