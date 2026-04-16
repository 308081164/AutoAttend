package org.example.atuo_attend_backend.marketplace;

import org.example.atuo_attend_backend.config.SystemConfigService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 项目信息发布：平台开关 + 租户/用户白名单。
 */
@Service
public class MarketplaceAccessService {

    private final SystemConfigService systemConfigService;

    public MarketplaceAccessService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    public Map<String, Object> rawConfig() {
        return systemConfigService.getMarketplaceProjectInfoConfig();
    }

    public boolean isModuleEnabledForRequest(long tenantId, Long userId) {
        Map<String, Object> c = rawConfig();
        Object en = c.get("enabled");
        boolean enabled = en instanceof Boolean ? (Boolean) en : Boolean.parseBoolean(String.valueOf(en != null ? en : "false"));
        if (!enabled) {
            return false;
        }
        String scope = String.valueOf(c.getOrDefault("scope", "off"));
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
        return false;
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
        Map<String, Object> c = rawConfig();
        Object en = c.get("enabled");
        boolean enabled = en instanceof Boolean ? (Boolean) en : Boolean.parseBoolean(String.valueOf(en != null ? en : "false"));
        if (!enabled) {
            return false;
        }
        if (!adminAuthenticated) {
            return isGuestBrowseAllowed();
        }
        return isModuleEnabledForRequest(tenantId, userId);
    }

    public boolean moduleEnabledFlag() {
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
