package org.example.atuo_attend_backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.collab.service.CollabPasswordService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 系统配置（如 GitHub Token），供管理后台填写、GithubDiffFetcher 等使用。
 */
@Service
public class SystemConfigService {

    private static final String KEY_GITHUB_TOKEN = "github.token";
    private static final String KEY_GITHUB_API_PROXY = "github.api.proxy";
    // ===== Mail (SMTP) =====
    public static final String KEY_MAIL_SMTP_HOST = "mail.smtp.host";
    public static final String KEY_MAIL_SMTP_PORT = "mail.smtp.port";
    public static final String KEY_MAIL_SMTP_USERNAME = "mail.smtp.username";
    public static final String KEY_MAIL_SMTP_PASSWORD = "mail.smtp.password";
    public static final String KEY_MAIL_FROM_ADDRESS = "mail.from.address";
    public static final String KEY_MAIL_FROM_NAME = "mail.from.name";
    /** 公开访问的系统基地址（用于邮件中的退订/预览链接），例如 https://your-domain.com */
    public static final String KEY_PUBLIC_BASE_URL = "app.public.base_url";
    /** 报价/合同：乙方（受托方）工商与收款信息 JSON，管理后台维护 */
    public static final String KEY_QUOTE_PARTY_B_PROFILE = "quote.party_b_profile_json";
    // ===== Membership plan quotas =====
    public static final String KEY_PLAN_FREE_MAX_MEMBERS = "plan.free.max_members";
    public static final String KEY_PLAN_FREE_MAX_GITHUB_REPOS = "plan.free.max_github_repos";
    public static final String KEY_PLAN_TEAM_MAX_MEMBERS = "plan.team.max_members";
    public static final String KEY_PLAN_TEAM_MAX_GITHUB_REPOS = "plan.team.max_github_repos";
    public static final String KEY_PLAN_PRO_MAX_MEMBERS = "plan.pro.max_members";
    public static final String KEY_PLAN_PRO_MAX_GITHUB_REPOS = "plan.pro.max_github_repos";
    /** BCrypt 哈希：导出敏感配置前须验证的二级密钥；未设置则禁止导出明文密钥 */
    public static final String KEY_EXPORT_SECONDARY_PASSWORD_HASH = "security.export_secondary_password.bcrypt";

    private final SystemConfigMapper mapper;
    private final CollabPasswordService passwordHasher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SystemConfigService(SystemConfigMapper mapper, CollabPasswordService passwordHasher) {
        this.mapper = mapper;
        this.passwordHasher = passwordHasher;
    }

    private static long tenantId() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    /** 获取 GitHub Token 原始值（供拉取 Diff 使用）；未配置返回 null。 */
    public String getGitHubToken() {
        String v = mapper.findByKey(tenantId(), KEY_GITHUB_TOKEN);
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
        mapper.upsert(tenantId(), KEY_GITHUB_TOKEN, value);
    }

    public String getGitHubApiProxy() {
        String v = mapper.findByKey(tenantId(), KEY_GITHUB_API_PROXY);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public void setGitHubApiProxy(String proxy) {
        String value = (proxy != null && !proxy.isBlank()) ? proxy.trim() : null;
        mapper.upsert(tenantId(), KEY_GITHUB_API_PROXY, value != null ? value : "");
    }

    // ===== Mail (SMTP) config stored in aa_system_config =====
    public String getMailSmtpHost() {
        String v = mapper.findByKey(tenantId(), KEY_MAIL_SMTP_HOST);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public Integer getMailSmtpPort() {
        String v = mapper.findByKey(tenantId(), KEY_MAIL_SMTP_PORT);
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return null; }
    }

    public String getMailSmtpUsername() {
        String v = mapper.findByKey(tenantId(), KEY_MAIL_SMTP_USERNAME);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getMailSmtpPassword() {
        String v = mapper.findByKey(tenantId(), KEY_MAIL_SMTP_PASSWORD);
        return (v != null && !v.isBlank()) ? v : null;
    }

    public String getMailFromAddress() {
        String v = mapper.findByKey(tenantId(), KEY_MAIL_FROM_ADDRESS);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getMailFromName() {
        String v = mapper.findByKey(tenantId(), KEY_MAIL_FROM_NAME);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getPublicBaseUrl() {
        String v = mapper.findByKey(tenantId(), KEY_PUBLIC_BASE_URL);
        if (v == null || v.isBlank()) return null;
        String s = v.trim();
        // 去掉尾部 /
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.isBlank() ? null : s;
    }

    /** 写入 SMTP 配置；password 若包含 **** 则视为未修改。 */
    public void saveMailSmtpConfig(String host, Integer port, String username, String password, String fromAddress, String fromName) {
        if (host != null) mapper.upsert(tenantId(), KEY_MAIL_SMTP_HOST, host.trim());
        if (port != null) mapper.upsert(tenantId(), KEY_MAIL_SMTP_PORT, String.valueOf(port));
        if (username != null) mapper.upsert(tenantId(), KEY_MAIL_SMTP_USERNAME, username.trim());
        if (password != null && !password.contains("****")) mapper.upsert(tenantId(), KEY_MAIL_SMTP_PASSWORD, password);
        if (fromAddress != null) mapper.upsert(tenantId(), KEY_MAIL_FROM_ADDRESS, fromAddress.trim());
        if (fromName != null) mapper.upsert(tenantId(), KEY_MAIL_FROM_NAME, fromName.trim());
    }

    public void setPublicBaseUrl(String baseUrl) {
        if (baseUrl == null) return;
        mapper.upsert(tenantId(), KEY_PUBLIC_BASE_URL, baseUrl.trim());
    }

    public String getMailSmtpPasswordMasked() {
        String v = getMailSmtpPassword();
        return v != null && !v.isBlank() ? "****" : null;
    }

    // ===== 二级密钥：敏感配置导出保护 =====

    public boolean isExportSecondaryPasswordConfigured() {
        String h = mapper.findByKey(tenantId(), KEY_EXPORT_SECONDARY_PASSWORD_HASH);
        return h != null && !h.isBlank();
    }

    /**
     * 设置或更新二级密钥（BCrypt 存储）。至少 8 个字符。
     */
    public void setExportSecondaryPassword(String plain) {
        if (plain == null || plain.length() < 8) {
            throw new IllegalArgumentException("二级密钥至少 8 个字符");
        }
        mapper.upsert(tenantId(), KEY_EXPORT_SECONDARY_PASSWORD_HASH, passwordHasher.hash(plain));
    }

    public boolean verifyExportSecondaryPassword(String plain) {
        if (plain == null || plain.isBlank()) {
            return false;
        }
        String h = mapper.findByKey(tenantId(), KEY_EXPORT_SECONDARY_PASSWORD_HASH);
        if (h == null || h.isBlank()) {
            return false;
        }
        return passwordHasher.verify(plain, h);
    }

    /** 清除二级密钥前须验证当前密钥 */
    public void clearExportSecondaryPassword(String currentPlain) {
        if (!isExportSecondaryPasswordConfigured()) {
            return;
        }
        if (!verifyExportSecondaryPassword(currentPlain)) {
            throw new IllegalArgumentException("二级密钥不正确");
        }
        mapper.upsert(tenantId(), KEY_EXPORT_SECONDARY_PASSWORD_HASH, "");
    }

    // ===== Membership plan quota config =====
    public Integer getPlanQuota(String key) {
        String v = mapper.findByKey(tenantId(), key);
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return null; }
    }

    public void saveMembershipPlanConfig(Map<String, Object> body) {
        if (body == null) return;
        upsertPlanInt(KEY_PLAN_FREE_MAX_MEMBERS, body.get("freeMaxMembers"));
        upsertPlanInt(KEY_PLAN_FREE_MAX_GITHUB_REPOS, body.get("freeMaxGithubRepos"));
        upsertPlanInt(KEY_PLAN_TEAM_MAX_MEMBERS, body.get("teamMaxMembers"));
        upsertPlanInt(KEY_PLAN_TEAM_MAX_GITHUB_REPOS, body.get("teamMaxGithubRepos"));
        upsertPlanInt(KEY_PLAN_PRO_MAX_MEMBERS, body.get("proMaxMembers"));
        upsertPlanInt(KEY_PLAN_PRO_MAX_GITHUB_REPOS, body.get("proMaxGithubRepos"));
    }

    private void upsertPlanInt(String key, Object raw) {
        if (raw == null) return;
        Integer n = null;
        if (raw instanceof Number num) {
            n = num.intValue();
        } else {
            try { n = Integer.parseInt(String.valueOf(raw)); } catch (Exception ignored) { }
        }
        if (n == null || n <= 0) return;
        mapper.upsert(tenantId(), key, String.valueOf(n));
    }

    /**
     * 乙方（开发方）主体模板 JSON：
     * 法人/组织：legalName、creditCode、address、contactName、contactPhone、bankName、bankAccount；
     * 自然人：嵌套对象 naturalPerson（fullName、idNumber、address、contactPhone、bankName、bankAccount、email 等）。
     */
    public Map<String, Object> getQuotePartyBProfile() {
        String raw = mapper.findByKey(tenantId(), KEY_QUOTE_PARTY_B_PROFILE);
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 与已有 JSON 合并写入：仅更新请求体中出现的顶层字段；{@code naturalPerson} 为 Map 时与库内同名对象做浅合并，
     * 避免「只保存法人模板」时清空自然人模板（及反向）。
     */
    @SuppressWarnings("unchecked")
    public void saveQuotePartyBProfile(Map<String, Object> incoming) throws JsonProcessingException {
        if (incoming == null) {
            return;
        }
        Map<String, Object> merged = new LinkedHashMap<>(getQuotePartyBProfile());
        for (Map.Entry<String, Object> e : incoming.entrySet()) {
            String k = e.getKey();
            if (Objects.equals("naturalPerson", k) && e.getValue() instanceof Map<?, ?> rawNp) {
                Map<String, Object> npBase = new LinkedHashMap<>();
                Object oldNp = merged.get("naturalPerson");
                if (oldNp instanceof Map<?, ?> om) {
                    for (Map.Entry<?, ?> ie : om.entrySet()) {
                        if (ie.getKey() != null) {
                            npBase.put(ie.getKey().toString(), ie.getValue());
                        }
                    }
                }
                for (Map.Entry<?, ?> ie : rawNp.entrySet()) {
                    if (ie.getKey() != null) {
                        npBase.put(ie.getKey().toString(), ie.getValue());
                    }
                }
                merged.put("naturalPerson", npBase);
            } else if (!Objects.equals("naturalPerson", k)) {
                merged.put(k, e.getValue());
            }
        }
        String json = merged.isEmpty() ? "{}" : objectMapper.writeValueAsString(merged);
        mapper.upsert(tenantId(), KEY_QUOTE_PARTY_B_PROFILE, json);
    }
}
