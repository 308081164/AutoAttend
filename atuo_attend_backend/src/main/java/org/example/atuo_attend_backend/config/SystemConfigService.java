package org.example.atuo_attend_backend.config;

import org.springframework.stereotype.Service;

/**
 * 系统配置（如 GitHub Token），供管理后台填写、GithubDiffFetcher 等使用。
 */
@Service
public class SystemConfigService {

    private static final String KEY_GITHUB_TOKEN = "github.token";
    private static final String KEY_GITHUB_API_PROXY = "github.api.proxy";

    private final SystemConfigMapper mapper;

    public SystemConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 获取 GitHub Token 原始值（供拉取 Diff 使用）；未配置返回 null。 */
    public String getGitHubToken() {
        String v = mapper.findByKey(KEY_GITHUB_TOKEN);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    /** 脱敏展示：前 4 位 + **** + 后 4 位；无则返回 null。 */
    public String getGitHubTokenMasked() {
        String v = getGitHubToken();
        if (v == null || v.length() <= 8) return v != null && !v.isBlank() ? "****" : null;
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    /** 传入新 token 则更新，传入空字符串则清空；含 **** 的脱敏串视为未修改不更新。 */
    public void setGitHubToken(String token) {
        if (token != null && token.contains("****")) return;
        String value = token == null ? "" : token.trim();
        mapper.upsert(KEY_GITHUB_TOKEN, value);
    }

    public String getGitHubApiProxy() {
        String v = mapper.findByKey(KEY_GITHUB_API_PROXY);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public void setGitHubApiProxy(String proxy) {
        String value = (proxy != null && !proxy.isBlank()) ? proxy.trim() : null;
        mapper.upsert(KEY_GITHUB_API_PROXY, value != null ? value : "");
    }
}
