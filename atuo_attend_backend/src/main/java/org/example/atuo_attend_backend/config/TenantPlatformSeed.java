package org.example.atuo_attend_backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动时写入默认租户与指定手机号管理员（密码 123456），并同步协作 biz_user。
 * 若平台尚未配置「项目信息发布」，则写入默认开启（scope=all），避免部署后功能对用户不可见。
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
    private final SystemConfigService systemConfigService;

    public TenantPlatformSeed(TenantMapper tenantMapper,
                              TenantAdminUserMapper tenantAdminUserMapper,
                              CollabPasswordService passwordService,
                              CollabAuthService collabAuthService,
                              SystemConfigService systemConfigService) {
        this.tenantMapper = tenantMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.passwordService = passwordService;
        this.collabAuthService = collabAuthService;
        this.systemConfigService = systemConfigService;
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

            seedMarketplaceProjectInfoIfUnset();
            seedShowcaseDefaultsIfUnset();
        } catch (Exception e) {
            log.warn("Tenant platform seed skipped: {}", e.getMessage());
        }
    }

    /**
     * 库中尚无 {@link SystemConfigService#KEY_MARKETPLACE_PROJECT_INFO_CONFIG} 时，落库默认「开启 + 全租户可用」，
     * 与「部署即上线」预期一致；若已在监测台保存过任意 JSON，则绝不覆盖。
     * 同步为种子管理员开启「可发布项目信息」，否则仅有浏览权限无法发首条。
     */
    private void seedMarketplaceProjectInfoIfUnset() {
        try {
            String raw = systemConfigService.getRawPlatformConfig(SystemConfigService.KEY_MARKETPLACE_PROJECT_INFO_CONFIG);
            // 键不存在或值为空：均视为「尚未在监测台配置」，写入默认开启
            if (raw != null && !raw.isBlank()) {
                return;
            }
            Map<String, Object> initial = new LinkedHashMap<>();
            initial.put("enabled", true);
            initial.put("scope", "all");
            initial.put("tenantIds", List.of());
            initial.put("userIds", List.of());
            initial.put("allowGuestBrowseList", false);
            initial.put("requireContentReview", true);
            initial.put("disclaimerVersion", "2026-04-01");
            systemConfigService.saveMarketplaceProjectInfoConfig(initial);
            log.info("Seeded platform marketplace.project_info (enabled=true, scope=all); override in monitor if needed");

            TenantAdminUser seed = tenantAdminUserMapper.findByPhone(SEED_PHONE);
            if (seed != null && !Boolean.TRUE.equals(seed.getCanPublishProjectInfo())) {
                tenantAdminUserMapper.updateCanPublishProjectInfo(seed.getId(), seed.getTenantId(), true);
                log.info("Granted can_publish_project_info to seed tenant admin");
            }
        } catch (JsonProcessingException e) {
            log.warn("Marketplace config seed skipped: {}", e.getMessage());
        }
    }

    /**
     * 若 showcase 配置键尚不存在，写入默认开启（template 模式 + enterprise 模板），
     * 使团队展示区功能在部署后立即可用，管理员可在监测台随时关闭或切换。
     */
    private void seedShowcaseDefaultsIfUnset() {
        try {
            String raw = systemConfigService.getRawPlatformConfig(SystemConfigService.KEY_SHOWCASE_ENABLED);
            if (raw != null && !raw.isBlank()) {
                return; // 已配置过，不覆盖
            }
            systemConfigService.setShowcaseEnabled(true);
            systemConfigService.setShowcaseMode("template");
            systemConfigService.setShowcaseTemplateId("enterprise");
            log.info("Seeded platform showcase defaults (enabled=true, mode=template, templateId=enterprise); override in monitor if needed");
        } catch (Exception e) {
            log.warn("Showcase config seed skipped: {}", e.getMessage());
        }
    }
}
