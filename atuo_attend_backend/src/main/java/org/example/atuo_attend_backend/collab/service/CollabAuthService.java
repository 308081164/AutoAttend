package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.admin.PhoneNormalizer;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.springframework.stereotype.Service;

/**
 * 协作模块登录：普通用户（邮箱+密码）、租户管理员（手机号 E.164 + 密码，与后台一致）。
 */
@Service
public class CollabAuthService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";

    private final BizUserMapper userMapper;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final CollabPasswordService passwordService;
    private final CollabJwtService jwtService;

    public CollabAuthService(BizUserMapper userMapper,
                             TenantAdminUserMapper tenantAdminUserMapper,
                             CollabPasswordService passwordService,
                             CollabJwtService jwtService) {
        this.userMapper = userMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    public String login(String emailOrPhone, String password) {
        if (emailOrPhone == null || emailOrPhone.isBlank()) {
            return null;
        }
        String trimmed = emailOrPhone.trim();

        String phone = PhoneNormalizer.normalize(trimmed);
        if (phone != null && password != null && password.length() <= 24) {
            TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phone);
            if (tau != null && passwordService.verify(password, tau.getPasswordHash())) {
                BizUser biz = ensureBizUserForTenantAdminInternal(phone, password);
                return jwtService.createToken(biz.getId(), biz.getEmail(), biz.getRole());
            }
        }

        BizUser user = userMapper.findByEmail(trimmed);
        if (user == null || !passwordService.verify(password, user.getPasswordHash())) {
            return null;
        }
        return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
    }

    /**
     * 同步租户管理员到 biz_user（email 字段存 E.164 手机号），保证协作侧 super_admin 与后台密码一致。
     */
    public void ensureBizUserForTenantAdmin(String phoneE164, String rawPassword) {
        ensureBizUserForTenantAdminInternal(phoneE164, rawPassword);
    }

    /**
     * 为当前租户管理员签发协作 JWT（后台已登录会话）。
     */
    public String issueCollabTokenForPhone(String phoneE164) {
        if (phoneE164 == null || phoneE164.isBlank()) {
            return null;
        }
        String phone = phoneE164.trim();
        TenantAdminUser tau = tenantAdminUserMapper.findByPhone(phone);
        if (tau == null) {
            return null;
        }
        BizUser biz = userMapper.findByEmail(phone);
        if (biz == null) {
            biz = new BizUser();
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

    private BizUser ensureBizUserForTenantAdminInternal(String phoneE164, String rawPassword) {
        BizUser user = userMapper.findByEmail(phoneE164);
        String hashed = passwordService.hash(rawPassword);
        if (user == null) {
            user = new BizUser();
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
}
