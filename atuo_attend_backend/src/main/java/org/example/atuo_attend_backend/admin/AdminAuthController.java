package org.example.atuo_attend_backend.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.PhoneNormalizer;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.admin.dto.*;
import org.example.atuo_attend_backend.admin.model.AdminUser;
import org.example.atuo_attend_backend.admin.sms.AdminSmsProperties;
import org.example.atuo_attend_backend.admin.sms.AdminSmsService;
import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.AdminSessionMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final CollabAuthService collabAuthService;
    private final TenantMapper tenantMapper;
    private final AdminSessionMapper adminSessionMapper;
    private final AdminSmsService adminSmsService;
    private final AdminSmsProperties adminSmsProperties;
    private final TenantAdminUserMapper tenantAdminUserMapper;

    public AdminAuthController(AdminAuthService adminAuthService,
                               CollabAuthService collabAuthService,
                               TenantMapper tenantMapper,
                               AdminSessionMapper adminSessionMapper,
                               AdminSmsService adminSmsService,
                               AdminSmsProperties adminSmsProperties,
                               TenantAdminUserMapper tenantAdminUserMapper) {
        this.adminAuthService = adminAuthService;
        this.collabAuthService = collabAuthService;
        this.tenantMapper = tenantMapper;
        this.adminSessionMapper = adminSessionMapper;
        this.adminSmsService = adminSmsService;
        this.adminSmsProperties = adminSmsProperties;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
    }

    /** 是否启用短信验证码（前端据此展示验证码输入框） */
    @GetMapping("/sms/config")
    public ApiResponse<Map<String, Object>> smsConfig() {
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", adminSmsService.smsLoginEnabled());
        m.put("resendIntervalSeconds", adminSmsProperties.getResendIntervalSeconds());
        m.put("maxSendsPerPhonePerDay", adminSmsProperties.getMaxSendsPerPhonePerDay());
        return ApiResponse.ok(m);
    }

    /** 发送登录/注册验证码 */
    @PostMapping("/sms/send")
    public ApiResponse<Void> sendSms(@RequestBody AdminSmsSendRequest body) {
        try {
            adminSmsService.sendCode(body.getPhone(), body.getPurpose());
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

    /** 绑定/校验协作侧手机号（需已登录管理员） */
    @PostMapping("/sms/send-bind-phone")
    public ApiResponse<Void> sendBindPhoneSms(@RequestBody AdminSmsSendRequest body, HttpServletRequest request) {
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

    @PostMapping("/bind-phone")
    public ApiResponse<Void> bindPhone(@RequestBody AdminBindPhoneRequest body, HttpServletRequest request) {
        String phone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        try {
            collabAuthService.bindPhoneForAdminSession(phone, body.getPhone(), body.getSmsCode());
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        String account = request.resolveLoginAccount();
        try {
            AdminAuthOutcome out = adminAuthService.login(account, request.getPassword(), request.getSmsCode());
            if (out == null) {
                return ApiResponse.error(40100, "invalid phone or password");
            }
            return ApiResponse.ok(buildLoginResponse(out));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40003, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(40301, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    adminSessionMapper.deleteByToken(token);
                } catch (Exception ignored) {
                }
            }
        }
        return ApiResponse.ok(null);
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

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody AdminChangePasswordRequest body) {
        try {
            adminAuthService.changeOwnPassword(
                    body != null ? body.getCurrentPassword() : null,
                    body != null ? body.getNewPassword() : null,
                    body != null ? body.getNewPasswordConfirm() : null);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
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
