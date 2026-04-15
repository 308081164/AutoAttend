package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.admin.PhoneNormalizer;
import org.example.atuo_attend_backend.admin.sms.AdminSmsService;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.domain.TenantInvite;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantInviteMapper;
import org.example.atuo_attend_backend.tenant.quota.TenantQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 协作模块登录：成员（邮箱/手机）；管理员协作 JWT 由 {@link #issueCollabTokenForPhone} 签发。
 */
@Service
public class CollabAuthService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String ROLE_MEMBER = "member";

    private final BizUserMapper userMapper;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final TenantInviteMapper tenantInviteMapper;
    private final TenantQuotaService tenantQuotaService;
    private final CollabPasswordService passwordService;
    private final CollabJwtService jwtService;
    private final AdminSmsService adminSmsService;

    public CollabAuthService(BizUserMapper userMapper,
                             TenantAdminUserMapper tenantAdminUserMapper,
                             TenantInviteMapper tenantInviteMapper,
                             TenantQuotaService tenantQuotaService,
                             CollabPasswordService passwordService,
                             CollabJwtService jwtService,
                             AdminSmsService adminSmsService) {
        this.userMapper = userMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.tenantInviteMapper = tenantInviteMapper;
        this.tenantQuotaService = tenantQuotaService;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.adminSmsService = adminSmsService;
    }

    /**
     * 成员登录：仅邮箱+密码，或已绑手机时手机号+短信验证码。不使用本接口登录管理员协作影子账号。
     *
     * @param emailOrPhone 邮箱，或已绑定成员账号的 E.164 手机号
     */
    public String loginMember(String emailOrPhone, String password, String smsCode, Long tenantId) {
        if (emailOrPhone == null || emailOrPhone.isBlank()) {
            return null;
        }
        String trimmed = emailOrPhone.trim();

        String phone = PhoneNormalizer.normalize(trimmed);
        if (phone != null) {
            return loginMemberByPhoneSms(phone, smsCode);
        }

        if (password == null || password.isEmpty() || password.length() > 24) {
            return null;
        }
        long tid = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT_ID;
        BizUser user = userMapper.findByTenantAndEmail(tid, trimmed);
        if (user == null || !passwordService.verify(password, user.getPasswordHash())) {
            return null;
        }
        if (user.getPhoneE164() != null && !user.getPhoneE164().isBlank()) {
            return null;
        }
        return jwtService.createToken(user.getId(), user.getEmail(), user.getRole(),
                CollabJwtService.JWT_MODE_MEMBER, CollabJwtService.PROJECT_SCOPE_ALL);
    }

    private String loginMemberByPhoneSms(String phoneE164, String smsCode) {
        if (smsCode == null || smsCode.isBlank()) {
            return null;
        }
        List<BizUser> rows = userMapper.listByPhoneE164(phoneE164);
        if (rows.isEmpty()) {
            return null;
        }
        // 管理员协作影子账号：email 与 phone_e164 同为 E.164，与真实成员账号共用一号时需排除，否则与成员行邮箱不一致会拦截登录
        List<BizUser> memberRows = new ArrayList<>();
        for (BizUser u : rows) {
            if (!isAdminBizUserShadowForBoundPhone(u)) {
                memberRows.add(u);
            }
        }
        if (memberRows.isEmpty()) {
            return null;
        }
        String err = adminSmsService.verifyAndConsume(phoneE164, AdminSmsService.PURPOSE_LOGIN, smsCode);
        if (err != null) {
            return null;
        }
        BizUser canonical = pickCanonicalForMemberPhoneLogin(memberRows);
        String emailNorm = normalizeEmailKey(canonical.getEmail());
        for (BizUser u : memberRows) {
            if (!Objects.equals(normalizeEmailKey(u.getEmail()), emailNorm)) {
                return null;
            }
        }
        return jwtService.createToken(canonical.getId(), canonical.getEmail(), canonical.getRole(),
                CollabJwtService.JWT_MODE_MEMBER, CollabJwtService.PROJECT_SCOPE_ALL);
    }

    /**
     * 租户管理员在协作侧的「影子」行：用手机号作 email，与 {@link #issueCollabTokenForPhone} 一致。
     * 成员绑定同一号码时会与该行共存，不应参与「邮箱必须一致」的占用校验与成员短信登录的邮箱对齐。
     */
    private boolean isAdminBizUserShadowForBoundPhone(BizUser u) {
        if (u == null || !ROLE_SUPER_ADMIN.equals(u.getRole())) {
            return false;
        }
        String phone = u.getPhoneE164();
        String em = u.getEmail();
        if (phone == null || phone.isBlank() || em == null || em.isBlank()) {
            return false;
        }
        return normalizeEmailKey(em).equals(normalizeEmailKey(phone));
    }

    private static BizUser pickCanonicalForMemberPhoneLogin(List<BizUser> memberRows) {
        for (BizUser u : memberRows) {
            if (ROLE_MEMBER.equals(u.getRole())) {
                return u;
            }
        }
        return memberRows.get(0);
    }

    private static String normalizeEmailKey(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    /**
     * 供协作登录接口显式调用（与旧 {@link #login} 区分）。
     */
    public String loginMemberExplicit(String emailOrPhone, String password, String smsCode, Long tenantId) {
        return loginMember(emailOrPhone, password, smsCode, tenantId);
    }

    /**
     * @deprecated 仅兼容旧调用；应使用 {@link #loginMember} 或管理员专用签发。
     */
    @Deprecated
    public String login(String emailOrPhone, String password, Long tenantId) {
        return loginMember(emailOrPhone, password, null, tenantId);
    }

    /**
     * 同步协作侧超级管理员影子账号。{@code rawPassword} 为空时仅将 {@code biz_user.password_hash} 与租户管理员表对齐（用于仅短信登录）。
     */
    public void ensureBizUserForTenantAdmin(String phoneE164, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            syncBizUserShadowPasswordFromTau(phoneE164);
            return;
        }
        TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phoneE164);
        long t = tau != null ? tau.getTenantId() : TenantConstants.DEFAULT_TENANT_ID;
        ensureBizUserForTenantAdminInternal(phoneE164, rawPassword, t);
    }

    /** 无明文密码时：以租户管理员密码哈希为准更新协作账号（用于短信验证码登录） */
    private void syncBizUserShadowPasswordFromTau(String phoneE164) {
        TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phoneE164);
        if (tau == null) {
            return;
        }
        long tid = tau.getTenantId();
        String tauHash = tau.getPasswordHash();
        BizUser user = userMapper.findByTenantAndEmail(tid, phoneE164);
        if (user == null) {
            user = new BizUser();
            user.setTenantId(tid);
            user.setEmail(phoneE164);
            user.setName("管理员");
            user.setPasswordHash(tauHash);
            user.setRole(ROLE_SUPER_ADMIN);
            userMapper.insert(user);
        } else {
            if (!ROLE_SUPER_ADMIN.equals(user.getRole())) {
                user.setRole(ROLE_SUPER_ADMIN);
            }
            if (tauHash != null && !tauHash.equals(user.getPasswordHash())) {
                user.setPasswordHash(tauHash);
            }
            userMapper.update(user);
        }
    }

    public String issueCollabTokenForPhone(String phoneE164) {
        return issueCollabTokenForPhone(phoneE164, CollabJwtService.JWT_MODE_ADMIN, CollabJwtService.PROJECT_SCOPE_TENANT);
    }

    /**
     * 为当前租户管理员签发协作 JWT（项目管理等仅看本租户）。
     */
    public String issueCollabTokenForPhone(String phoneE164, String mode, String projectScope) {
        if (phoneE164 == null || phoneE164.isBlank()) {
            return null;
        }
        String phone = phoneE164.trim();
        TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phone);
        if (tau == null) {
            return null;
        }
        long tid = tau.getTenantId();
        BizUser biz = userMapper.findByTenantAndEmail(tid, phone);
        if (biz == null) {
            biz = new BizUser();
            biz.setTenantId(tid);
            biz.setEmail(phone);
            biz.setName("管理员");
            biz.setPasswordHash(tau.getPasswordHash());
            biz.setRole(ROLE_SUPER_ADMIN);
            userMapper.insert(biz);
        } else {
            if (!ROLE_SUPER_ADMIN.equals(biz.getRole())) {
                biz.setRole(ROLE_SUPER_ADMIN);
            }
            if (!tau.getPasswordHash().equals(biz.getPasswordHash())) {
                biz.setPasswordHash(tau.getPasswordHash());
            }
            userMapper.update(biz);
        }
        return jwtService.createToken(biz.getId(), biz.getEmail(), biz.getRole(), mode, projectScope);
    }

    private BizUser ensureBizUserForTenantAdminInternal(String phoneE164, String rawPassword, long tenantId) {
        BizUser user = userMapper.findByTenantAndEmail(tenantId, phoneE164);
        String hashed = passwordService.hash(rawPassword);
        if (user == null) {
            user = new BizUser();
            user.setTenantId(tenantId);
            user.setEmail(phoneE164);
            user.setName("管理员");
            user.setPasswordHash(hashed);
            user.setRole(ROLE_SUPER_ADMIN);
            userMapper.insert(user);
            return user;
        }
        if (!ROLE_SUPER_ADMIN.equals(user.getRole())) {
            user.setRole(ROLE_SUPER_ADMIN);
        }
        if (!passwordService.verify(rawPassword, user.getPasswordHash())) {
            user.setPasswordHash(hashed);
        }
        userMapper.update(user);
        return user;
    }

    public BizUser getCurrentUser(long userId) {
        return userMapper.findById(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePasswordForCollabUser(long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isEmpty() || newPassword.length() > 24) {
            throw new IllegalArgumentException("新密码长度须在 1～24");
        }
        BizUser u = userMapper.findById(userId);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (currentPassword == null || !passwordService.verify(currentPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码错误");
        }
        String hashed = passwordService.hash(newPassword);
        userMapper.updatePasswordHashByEmailNormalized(u.getEmail(), hashed);
    }

    /**
     * 租户管理员在工作台绑定手机：协作影子账号 email 与管理员手机号一致，同步所有同邮箱成员行。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindPhoneForAdminSession(String adminPhoneE164, String phoneRaw, String smsCode) {
        if (adminPhoneE164 == null || adminPhoneE164.isBlank()) {
            throw new IllegalArgumentException("未找到管理员账号");
        }
        long tid = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        String phone = adminPhoneE164.trim();
        BizUser row = userMapper.findByTenantAndEmail(tid, phone);
        if (row == null || !ROLE_SUPER_ADMIN.equals(row.getRole())) {
            throw new IllegalArgumentException("协作管理员账号不存在");
        }
        bindPhoneCore(row.getEmail(), phoneRaw, smsCode);
    }

    /**
     * 成员绑定手机（协作端已登录）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindPhoneForMember(long collabUserId, String phoneRaw, String smsCode) {
        BizUser row = userMapper.findById(collabUserId);
        if (row == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        bindPhoneCore(row.getEmail(), phoneRaw, smsCode);
    }

    private void bindPhoneCore(String email, String phoneRaw, String smsCode) {
        String phone = PhoneNormalizer.normalize(phoneRaw);
        if (phone == null) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phone);
        if (tau == null) {
            throw new IllegalArgumentException("该手机号未注册管理员账号，无法完成关联绑定");
        }
        List<BizUser> existing = userMapper.listByPhoneE164(phone);
        String emailKey = normalizeEmailKey(email);
        for (BizUser o : existing) {
            if (isAdminBizUserShadowForBoundPhone(o)) {
                continue;
            }
            if (!normalizeEmailKey(o.getEmail()).equals(emailKey)) {
                throw new IllegalArgumentException("该手机号已被其他成员占用");
            }
        }
        String err = adminSmsService.verifyAndConsume(phone, AdminSmsService.PURPOSE_BIND_PHONE, smsCode);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        Long adminId = tau.getId();
        userMapper.updatePhoneLinkByEmailNormalized(email, phone, adminId);
    }

    /**
     * 通过邀请令牌注册协作账号（绑定邀请所属租户）。
     *
     * @return JWT；失败返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public String registerByInvite(String token, String email, String password, String name) {
        if (token == null || token.isBlank() || email == null || email.isBlank() || password == null || password.isEmpty()) {
            return null;
        }
        if (password.length() > 24) {
            return null;
        }
        TenantInvite inv = tenantInviteMapper.findByToken(token.trim());
        if (inv == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(now)) {
            return null;
        }
        if (inv.getUsedCount() >= inv.getMaxUses()) {
            return null;
        }
        long tid = inv.getTenantId();
        var quota = tenantQuotaService.checkCanAddMember(tid);
        if (!quota.allowed()) {
            return null;
        }
        String em = email.trim();
        if (userMapper.findByTenantAndEmail(tid, em) != null) {
            throw new IllegalArgumentException("该邮箱已在组织中注册");
        }
        BizUser user = new BizUser();
        user.setTenantId(tid);
        user.setEmail(em);
        user.setName(name != null && !name.isBlank() ? name.trim() : em);
        user.setPasswordHash(passwordService.hash(password));
        user.setRole(ROLE_MEMBER);
        user.setJobTitle("开发工程师");
        userMapper.insert(user);
        tenantInviteMapper.incrementUsed(inv.getId());
        return jwtService.createToken(user.getId(), user.getEmail(), user.getRole(),
                CollabJwtService.JWT_MODE_MEMBER, CollabJwtService.PROJECT_SCOPE_ALL);
    }
}
