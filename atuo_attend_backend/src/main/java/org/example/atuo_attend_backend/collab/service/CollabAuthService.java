package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 协作模块登录：支持普通用户（邮箱+密码）与管理员账号（admin 同后台账号）。
 * 管理员以 admin 账号登录协作时，自动获得 super_admin 角色，可查看全部项目与多维表。
 */
@Service
public class CollabAuthService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";

    @Value("${autoattend.admin.username:admin}")
    private String adminUsername;

    @Value("${autoattend.admin.password:admin123}")
    private String adminPassword;

    private final BizUserMapper userMapper;
    private final CollabPasswordService passwordService;
    private final CollabJwtService jwtService;

    public CollabAuthService(BizUserMapper userMapper,
                             CollabPasswordService passwordService,
                             CollabJwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    public String login(String email, String password) {
        if (email == null || email.isBlank()) return null;
        String trimmedEmail = email.trim();

        // 管理员账号：用后台同一账号密码登录协作，获得 super_admin，可访问全部项目
        if (adminUsername.equals(trimmedEmail) && adminPassword.equals(password)) {
            BizUser adminUser = getOrCreateAdminBizUser();
            return jwtService.createToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole());
        }

        BizUser user = userMapper.findByEmail(trimmedEmail);
        if (user == null || !passwordService.verify(password, user.getPasswordHash())) {
            return null;
        }
        return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
    }

    /**
     * 获取或创建协作侧「管理员」用户（email=admin 用户名，role=super_admin）。
     * 密码与后台配置同步，保证管理员用同一套账号可登录协作并拥有全部项目权限。
     */
    private BizUser getOrCreateAdminBizUser() {
        BizUser user = userMapper.findByEmail(adminUsername);
        String hashed = passwordService.hash(adminPassword);
        if (user == null) {
            user = new BizUser();
            user.setEmail(adminUsername);
            user.setName("管理员");
            user.setPasswordHash(hashed);
            user.setRole(ROLE_SUPER_ADMIN);
            userMapper.insert(user);
            return user;
        }
        if (!ROLE_SUPER_ADMIN.equals(user.getRole())) {
            user.setRole(ROLE_SUPER_ADMIN);
            userMapper.update(user);
        }
        if (!passwordService.verify(adminPassword, user.getPasswordHash())) {
            user.setPasswordHash(hashed);
            userMapper.update(user);
        }
        return user;
    }

    /**
     * 为当前配置的管理员账号签发协作 JWT，用于管理员登录后台时一并下发，无需在项目协作再次登录。
     */
    public String issueCollabTokenForAdmin() {
        BizUser adminUser = getOrCreateAdminBizUser();
        return jwtService.createToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole());
    }

    public BizUser getCurrentUser(long userId) {
        return userMapper.findById(userId);
    }
}
