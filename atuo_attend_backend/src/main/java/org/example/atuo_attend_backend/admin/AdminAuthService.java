package org.example.atuo_attend_backend.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.admin.dto.AdminAuthOutcome;
import org.example.atuo_attend_backend.admin.dto.AdminRegisterRequest;
import org.example.atuo_attend_backend.admin.sms.AdminSmsService;
import org.example.atuo_attend_backend.ai.official.OfficialAiPoolService;
import org.example.atuo_attend_backend.tenant.referral.InviteCodeService;
import org.example.atuo_attend_backend.admin.model.AdminUser;
import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.collab.service.CollabPasswordService;
import org.example.atuo_attend_backend.tenant.domain.AdminSession;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.mapper.AdminSessionMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AdminAuthService {

    private static final long SESSION_SECONDS = 7200L;

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,62}[a-z0-9])?$|^[a-z0-9]{2}$");

    private final TenantMapper tenantMapper;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final AdminSessionMapper adminSessionMapper;
    private final CollabPasswordService passwordService;
    private final CollabAuthService collabAuthService;
    private final AdminSmsService adminSmsService;
    private final InviteCodeService inviteCodeService;
    private final OfficialAiPoolService officialAiPoolService;

    public AdminAuthService(TenantMapper tenantMapper,
                            TenantAdminUserMapper tenantAdminUserMapper,
                            AdminSessionMapper adminSessionMapper,
                            CollabPasswordService passwordService,
                            CollabAuthService collabAuthService,
                            AdminSmsService adminSmsService,
                            InviteCodeService inviteCodeService,
                            OfficialAiPoolService officialAiPoolService) {
        this.tenantMapper = tenantMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.adminSessionMapper = adminSessionMapper;
        this.passwordService = passwordService;
        this.collabAuthService = collabAuthService;
        this.adminSmsService = adminSmsService;
        this.inviteCodeService = inviteCodeService;
        this.officialAiPoolService = officialAiPoolService;
    }

    /**
     * @param smsCode 短信验证码；仅当非空时走短信登录。短信功能开启时仍可走密码登录（不传或空验证码）。
     * @return 凭证；账号或密码错误时返回 null
     */
    public AdminAuthOutcome login(String phoneRaw, String password, String smsCode) {
        String phone = PhoneNormalizer.normalize(phoneRaw);
        if (phone == null) {
            return null;
        }
        TenantAdminUser user = tenantAdminUserMapper.findByPhone(phone);
        if (user == null) {
            return null;
        }
        Tenant tenant = tenantMapper.findById(user.getTenantId());
        if (tenant != null && "suspended".equalsIgnoreCase(tenant.getStatus())) {
            throw new IllegalStateException("组织已暂停服务，请联系平台支持");
        }

        boolean useSms = smsCode != null && !smsCode.isBlank();

        if (adminSmsService.smsLoginEnabled() && useSms) {
            String err = adminSmsService.verifyAndConsume(phoneRaw, AdminSmsService.PURPOSE_LOGIN, smsCode);
            if (err != null) {
                throw new IllegalArgumentException(err);
            }
            // 短信登录：不校验密码；协作影子账号无明文密码时与管理员表哈希对齐
            collabAuthService.ensureBizUserForTenantAdmin(phone,
                    password != null && !password.isBlank() ? password : null);
            if (!Boolean.TRUE.equals(user.getSmsLoginOnboarded())) {
                tenantAdminUserMapper.updateSmsLoginOnboarded(user.getId(), true);
            }
            return createSessionOutcome(user);
        }

        // 密码登录（含：未启用短信；或已启用短信但未传验证码——与前端「密码登录」标签一致）
        if (password == null || password.isEmpty()) {
            return null;
        }
        if (password.length() > 24) {
            return null;
        }
        if (!passwordService.verify(password, user.getPasswordHash())) {
            return null;
        }
        collabAuthService.ensureBizUserForTenantAdmin(phone, password);
        return createSessionOutcome(user);
    }

    @Transactional
    public AdminAuthOutcome register(AdminRegisterRequest req) {
        String pwdErr = validatePasswordForRegister(req.getPassword());
        if (pwdErr != null) {
            throw new IllegalArgumentException(pwdErr);
        }
        String phone = PhoneNormalizer.normalize(req.getPhone());
        if (phone == null) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (adminSmsService.smsLoginEnabled()) {
            String err = adminSmsService.verifyAndConsume(req.getPhone(), AdminSmsService.PURPOSE_REGISTER, req.getSmsCode());
            if (err != null) {
                throw new IllegalArgumentException(err);
            }
        }
        if (req.getOrgName() == null || req.getOrgName().isBlank()) {
            throw new IllegalArgumentException("组织名称不能为空");
        }
        String slug = normalizeSlug(req.getSlug());
        if (!isValidSlug(slug)) {
            throw new IllegalArgumentException("slug 格式不正确（2～64 位小写字母、数字、连字符，首尾须为字母或数字）");
        }
        if (tenantMapper.countBySlug(slug) > 0) {
            throw new IllegalArgumentException("该 slug 已被占用");
        }
        if (tenantAdminUserMapper.countByPhone(phone) > 0) {
            throw new IllegalArgumentException("该手机号已注册");
        }

        Tenant tenant = new Tenant();
        tenant.setName(req.getOrgName().trim());
        tenant.setSlug(slug);
        tenant.setPlanCode("free");
        tenant.setBillingBaselinePlanCode("free");
        tenant.setStatus("active");
        tenantMapper.insert(tenant);
        officialAiPoolService.grantRegistrationBonus(tenant.getId());

        TenantAdminUser user = new TenantAdminUser();
        user.setTenantId(tenant.getId());
        user.setPhone(phone);
        user.setPasswordHash(passwordService.hash(req.getPassword()));
        tenantAdminUserMapper.insert(user);

        collabAuthService.ensureBizUserForTenantAdmin(phone, req.getPassword());
        if (req.getInviteCode() != null && !req.getInviteCode().isBlank()) {
            try {
                inviteCodeService.onTenantRegisteredWithInvite(tenant.getId(), req.getInviteCode());
            } catch (Exception ignored) {
                // 邀请码无效时不阻断注册
            }
        }
        return createSessionOutcome(user);
    }

    public AdminUser currentAdmin() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        HttpServletRequest req = attrs.getRequest();
        Long userId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        Long tenantId = (Long) req.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        String phone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
        if (userId == null || tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        Tenant t = tenantMapper.findById(tenantId);
        TenantAdminUser tau = tenantAdminUserMapper.findById(userId);
        AdminUser u = new AdminUser();
        u.setUserId(userId);
        u.setPhone(phone);
        u.setUsername(phone != null ? phone : "");
        u.setRole("TENANT_ADMIN");
        u.setTenantId(tenantId);
        if (tau != null) {
            u.setCanPublishProjectInfo(tau.getCanPublishProjectInfo());
        }
        if (t != null) {
            u.setTenantName(t.getName());
            u.setSlug(t.getSlug());
        }
        return u;
    }

    private AdminAuthOutcome createSessionOutcome(TenantAdminUser user) {
        adminSessionMapper.deleteExpired();
        String token = UUID.randomUUID().toString();
        AdminSession session = new AdminSession();
        session.setToken(token);
        session.setUserId(user.getId());
        session.setTenantId(user.getTenantId());
        // expires_at 现在由数据库的 NOW() + INTERVAL 7200 SECOND 计算，确保时区一致
        adminSessionMapper.insert(session);

        AdminAuthOutcome o = new AdminAuthOutcome();
        o.setToken(token);
        o.setExpiresInSeconds(SESSION_SECONDS);
        o.setPhone(user.getPhone());
        o.setUserId(user.getId());
        o.setTenantId(user.getTenantId());
        return o;
    }

    private static String validatePasswordForRegister(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() > 24) {
            return "密码长度不能超过 24 个字符";
        }
        return null;
    }

    private static boolean isValidSlug(String slug) {
        return slug != null && slug.length() >= 2 && slug.length() <= 64 && SLUG_PATTERN.matcher(slug).matches();
    }

    private static String normalizeSlug(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
