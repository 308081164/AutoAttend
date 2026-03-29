package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.admin.PhoneNormalizer;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.domain.TenantInvite;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantInviteMapper;
import org.example.atuo_attend_backend.tenant.quota.TenantQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 协作模块登录：普通用户（邮箱+密码）、租户管理员（手机号 E.164 + 密码，与后台一致）。
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

    public CollabAuthService(BizUserMapper userMapper,
                             TenantAdminUserMapper tenantAdminUserMapper,
                             TenantInviteMapper tenantInviteMapper,
                             TenantQuotaService tenantQuotaService,
                             CollabPasswordService passwordService,
                             CollabJwtService jwtService) {
        this.userMapper = userMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.tenantInviteMapper = tenantInviteMapper;
        this.tenantQuotaService = tenantQuotaService;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    /**
     * @param tenantId 邮箱登录时限定租户；为 null 时使用默认租户 1。手机号管理员登录以租户管理员表为准，忽略此参数。
     */
    public String login(String emailOrPhone, String password, Long tenantId) {
        if (emailOrPhone == null || emailOrPhone.isBlank()) {
            return null;
        }
        String trimmed = emailOrPhone.trim();

        String phone = PhoneNormalizer.normalize(trimmed);
        if (phone != null && password != null && password.length() <= 24) {
            TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phone);
            if (tau != null && passwordService.verify(password, tau.getPasswordHash())) {
                BizUser biz = ensureBizUserForTenantAdminInternal(phone, password, tau.getTenantId());
                return jwtService.createToken(biz.getId(), biz.getEmail(), biz.getRole());
            }
        }

        long tid = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT_ID;
        BizUser user = userMapper.findByTenantAndEmail(tid, trimmed);
        if (user == null || !passwordService.verify(password, user.getPasswordHash())) {
            return null;
        }
        return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
    }

    public void ensureBizUserForTenantAdmin(String phoneE164, String rawPassword) {
        TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phoneE164);
        long t = tau != null ? tau.getTenantId() : TenantConstants.DEFAULT_TENANT_ID;
        ensureBizUserForTenantAdminInternal(phoneE164, rawPassword, t);
    }

    public String issueCollabTokenForPhone(String phoneE164) {
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
        return jwtService.createToken(biz.getId(), biz.getEmail(), biz.getRole());
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
        tenantQuotaService.assertCanAddMember(tid);
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
        return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
    }
}
