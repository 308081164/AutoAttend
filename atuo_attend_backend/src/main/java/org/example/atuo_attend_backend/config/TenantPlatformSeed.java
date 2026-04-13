package org.example.atuo_attend_backend.config;

import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.collab.service.CollabPasswordService;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 首次启动时写入默认租户与指定手机号管理员（密码 123456），并同步协作 biz_user。
 */
@Component
@Order(100)
public class TenantPlatformSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantPlatformSeed.class);

    private static final String SEED_PHONE = "+8617753158844";
    private static final String SEED_PASSWORD = "123456";
    private static final String DEFAULT_SLUG = "default";

    private final TenantMapper tenantMapper;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final CollabPasswordService passwordService;
    private final CollabAuthService collabAuthService;

    public TenantPlatformSeed(TenantMapper tenantMapper,
                              TenantAdminUserMapper tenantAdminUserMapper,
                              CollabPasswordService passwordService,
                              CollabAuthService collabAuthService) {
        this.tenantMapper = tenantMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.passwordService = passwordService;
        this.collabAuthService = collabAuthService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (tenantMapper.findBySlug(DEFAULT_SLUG) == null) {
                Tenant t = new Tenant();
                t.setName("默认组织");
                t.setSlug(DEFAULT_SLUG);
                t.setPlanCode("free");
                t.setBillingBaselinePlanCode("free");
                t.setStatus("active");
                tenantMapper.insert(t);
                log.info("Seeded default tenant slug={}", DEFAULT_SLUG);
            }
            Tenant def = tenantMapper.findBySlug(DEFAULT_SLUG);
            if (def == null) {
                return;
            }
            if (tenantAdminUserMapper.findByPhone(SEED_PHONE) == null) {
                TenantAdminUser u = new TenantAdminUser();
                u.setTenantId(def.getId());
                u.setPhone(SEED_PHONE);
                u.setPasswordHash(passwordService.hash(SEED_PASSWORD));
                tenantAdminUserMapper.insert(u);
                log.info("Seeded tenant admin phone={}", SEED_PHONE);
            }
            collabAuthService.ensureBizUserForTenantAdmin(SEED_PHONE, SEED_PASSWORD);
        } catch (Exception e) {
            log.warn("Tenant platform seed skipped: {}", e.getMessage());
        }
    }
}
