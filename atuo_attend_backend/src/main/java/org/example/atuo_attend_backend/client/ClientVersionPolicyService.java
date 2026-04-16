package org.example.atuo_attend_backend.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.springframework.stereotype.Service;


/**
 * 解析平台配置中的壳客户端策略，并判断版本是否允许访问 API。
 */
@Service
public class ClientVersionPolicyService {

    public static final String KEY_CLIENT_SHELL_POLICY_JSON = "client.shell.policy_json";
    public static final String KEY_CLIENT_DOWNLOADS_JSON = "client.shell.downloads_json";

    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientVersionPolicyService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    public ClientShellPolicy getPolicy() {
        String raw = systemConfigService.getRawPlatformConfig(KEY_CLIENT_SHELL_POLICY_JSON);
        if (raw == null || raw.isBlank()) {
            return new ClientShellPolicy();
        }
        try {
            return objectMapper.readValue(raw, ClientShellPolicy.class);
        } catch (JsonProcessingException e) {
            return new ClientShellPolicy();
        }
    }

    public void savePolicy(ClientShellPolicy policy) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(policy != null ? policy : new ClientShellPolicy());
        systemConfigService.upsertPlatformConfig(KEY_CLIENT_SHELL_POLICY_JSON, json);
    }

    public ClientShellDownloadsConfig getDownloadsConfig() {
        String raw = systemConfigService.getRawPlatformConfig(KEY_CLIENT_DOWNLOADS_JSON);
        if (raw == null || raw.isBlank()) {
            return new ClientShellDownloadsConfig();
        }
        try {
            return objectMapper.readValue(raw, ClientShellDownloadsConfig.class);
        } catch (JsonProcessingException e) {
            return new ClientShellDownloadsConfig();
        }
    }

    public void saveDownloadsConfig(ClientShellDownloadsConfig cfg) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(cfg != null ? cfg : new ClientShellDownloadsConfig());
        systemConfigService.upsertPlatformConfig(KEY_CLIENT_DOWNLOADS_JSON, json);
    }

    /**
     * @return null 若允许；否则返回拒绝原因与升级链接
     */
    public BlockResult evaluate(String version, Integer build) {
        ClientShellPolicy p = getPolicy();
        if (version != null && !version.isBlank()) {
            for (String b : p.getBlockedVersions()) {
                if (version.equalsIgnoreCase(b != null ? b.trim() : "")) {
                    return new BlockResult(p.getBlockMessage(), p.getUpgradeUrl());
                }
            }
        }
        if (build != null && p.getBlockedBuilds() != null && p.getBlockedBuilds().contains(build)) {
            return new BlockResult(p.getBlockMessage(), p.getUpgradeUrl());
        }
        String min = p.getMinSupportedVersion();
        if (min != null && !min.isBlank() && version != null && !version.isBlank()) {
            if (compareSemver(version.trim(), min.trim()) < 0) {
                return new BlockResult(
                        p.getBlockMessage() != null && !p.getBlockMessage().isBlank()
                                ? p.getBlockMessage()
                                : "当前版本低于最低支持版本 " + min + "，请升级客户端。",
                        p.getUpgradeUrl());
            }
        }
        return null;
    }

    /** &lt;0 if a &lt; b, 0 if equal, &gt;0 if a &gt; b */
    public static int compareSemver(String a, String b) {
        int[] pa = parseParts(a);
        int[] pb = parseParts(b);
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(pa[i], pb[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    private static int[] parseParts(String v) {
        String s = v == null ? "" : v.trim();
        int[] out = new int[]{0, 0, 0};
        if (s.isEmpty()) return out;
        String[] sp = s.split("\\.");
        for (int i = 0; i < 3 && i < sp.length; i++) {
            String p = sp[i].replaceAll("[^0-9].*", "");
            try {
                out[i] = p.isEmpty() ? 0 : Integer.parseInt(p);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    public static final class BlockResult {
        private final String message;
        private final String upgradeUrl;

        public BlockResult(String message, String upgradeUrl) {
            this.message = message;
            this.upgradeUrl = upgradeUrl;
        }

        public String getMessage() {
            return message;
        }

        public String getUpgradeUrl() {
            return upgradeUrl;
        }
    }
}
