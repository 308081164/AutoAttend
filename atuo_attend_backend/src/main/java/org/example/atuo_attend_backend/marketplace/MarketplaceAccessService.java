package org.example.atuo_attend_backend.marketplace;

import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 项目信息发布：平台总开关（全局 JSON）+ 每租户开关（aa_tenant）+ 可选用户白名单（全局 JSON，兼容旧配置）。
 */
@Service
public class MarketplaceAccessService {

    private final SystemConfigService systemConfigService;
    private final TenantMapper tenantMapper;

    public MarketplaceAccessService(SystemConfigService systemConfigService, TenantMapper tenantMapper) {
        this.systemConfigService = systemConfigService;
        this.tenantMapper = tenantMapper;
    }

    public Map<String, Object> rawConfig() {
        return systemConfigService.getMarketplaceProjectInfoConfig();
    }

    /**
     * 当前租户是否允许浏览项目信息（列表/详情）。平台关闭或租户关闭则为 false。
     */
    public boolean isTenantBrowseAllowed(long tenantId) {
        if (!platformModuleEnabled()) {
            return false;
        }
        Tenant t = tenantMapper.findById(tenantId);
        if (t == null) {
            return false;
        }
        // 未迁移列时可能为 null，默认允许浏览
        if (t.getProjectMarketplaceEnabled() == null) {
            return true;
        }
        return Boolean.TRUE.equals(t.getProjectMarketplaceEnabled());
    }

    /**
     * 本租户是否允许「尝试发布」（仍须管理员 can_publish_project_info）。
     */
    public boolean isTenantPublishAllowed(long tenantId) {
        if (!isTenantBrowseAllowed(tenantId)) {
            return false;
        }
        Tenant t = tenantMapper.findById(tenantId);
        if (t == null) {
            return false;
        }
        if (t.getProjectMarketplaceAllowPublish() == null) {
            return false;
        }
        return Boolean.TRUE.equals(t.getProjectMarketplaceAllowPublish());
    }

    /**
     * 已登录管理员是否可见模块入口与列表/详情（不要求租户开启「发布」）。
     */
    public boolean isModuleVisibleForAdmin(long tenantId, Long userId) {
        if (!platformModuleEnabled()) {
            return false;
        }
        if (!isTenantBrowseAllowed(tenantId)) {
            return false;
        }
        return globalJsonAllowsUser(tenantId, userId);
    }

    /**
     * 发布/编辑/关闭/「我的发布」等写操作路径：平台 + 租户浏览 + 租户允许发布 + 全局 JSON 策略。
     */
    public boolean isPublishWorkflowAllowed(long tenantId, Long userId) {
        if (!platformModuleEnabled()) {
            return false;
        }
        if (!isTenantPublishAllowed(tenantId)) {
            return false;
        }
        return globalJsonAllowsUser(tenantId, userId);
    }

    private boolean globalJsonAllowsUser(long tenantId, Long userId) {
        Map<String, Object> c = rawConfig();
        String scope = String.valueOf(c.getOrDefault("scope", "all"));
        if ("off".equalsIgnoreCase(scope)) {
            return false;
        }
        if ("all".equalsIgnoreCase(scope)) {
            return userAllowed(c, userId);
        }
        if ("tenant_whitelist".equalsIgnoreCase(scope)) {
            if (!tenantInWhitelist(c, tenantId)) {
                return false;
            }
            return userAllowed(c, userId);
        }
        if ("user_whitelist".equalsIgnoreCase(scope)) {
            if (userId == null) {
                return false;
            }
            List<?> ids = listLongs(c.get("userIds"));
            return ids.contains(userId);
        }
        return userAllowed(c, userId);
    }

    public boolean isGuestBrowseAllowed() {
        Map<String, Object> c = rawConfig();
        Object v = c.get("allowGuestBrowseList");
        return v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v != null ? v : "false"));
    }

    public String disclaimerVersion() {
        Object v = rawConfig().get("disclaimerVersion");
        return v != null ? String.valueOf(v) : "2026-04-01";
    }

    public boolean requireContentReview() {
        Object v = rawConfig().get("requireContentReview");
        if (v == null) {
            return true;
        }
        return v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v));
    }

    /** 能力探测：未登录访客仅当 allowGuestBrowseList 且总开关开启时为可见；已登录管理员走白名单逻辑。 */
    public boolean isVisibleToCaller(boolean adminAuthenticated, long tenantId, Long userId) {
        if (!platformModuleEnabled()) {
            return false;
        }
        if (!adminAuthenticated) {
            return isGuestBrowseAllowed();
        }
        return isModuleVisibleForAdmin(tenantId, userId);
    }

    public boolean moduleEnabledFlag() {
        return platformModuleEnabled();
    }

    private boolean platformModuleEnabled() {
        Map<String, Object> c = rawConfig();
        Object en = c.get("enabled");
        return en instanceof Boolean ? (Boolean) en : Boolean.parseBoolean(String.valueOf(en != null ? en : "false"));
    }

    private static boolean tenantInWhitelist(Map<String, Object> c, long tenantId) {
        List<Long> ids = listLongs(c.get("tenantIds"));
        return ids.contains(tenantId);
    }

    /** 若 userIds 为空则不对用户再做限制（在租户已通过的前提下）。 */
    private static boolean userAllowed(Map<String, Object> c, Long userId) {
        List<Long> userIds = listLongs(c.get("userIds"));
        if (userIds.isEmpty()) {
            return true;
        }
        return userId != null && userIds.contains(userId);
    }

    @SuppressWarnings("unchecked")
    private static List<Long> listLongs(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(o -> {
                if (o instanceof Number n) {
                    return n.longValue();
                }
                try {
                    return Long.parseLong(String.valueOf(o));
                } catch (Exception e) {
                    return null;
                }
            }).filter(java.util.Objects::nonNull).toList();
        }
        return List.of();
    }
}
