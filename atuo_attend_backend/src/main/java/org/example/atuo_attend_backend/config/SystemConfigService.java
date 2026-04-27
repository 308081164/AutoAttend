package org.example.atuo_attend_backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.collab.service.CollabPasswordService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 系统配置（如 GitHub Token），供管理后台填写、GithubDiffFetcher 等使用。
 */
@Service
public class SystemConfigService {

    /** 平台级系统配置（SMTP、公网地址、日报调度等），与租户控制台解耦 */
    public static final long PLATFORM_SYSTEM_CONFIG_TENANT_ID = 0L;

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

    /** 官方 API 池总开关（tenant_id=0） */
    public static final String KEY_OFFICIAL_AI_POOL_ENABLED = "official_ai.pool.enabled";
    /** 平台代付 DeepSeek API Key（tenant_id=0） */
    public static final String KEY_OFFICIAL_DEEPSEEK_API_KEY = "official_ai.deepseek.api_key";
    /** 平台代付 Qwen/DashScope API Key（tenant_id=0） */
    public static final String KEY_OFFICIAL_QWEN_API_KEY = "official_ai.qwen.api_key";

    /** 日报邮件定时任务（存于平台配置 tenant_id=0，由监测台维护） */
    public static final String KEY_REPORT_MAIL_ENABLED = "app.report_mail.enabled";
    public static final String KEY_REPORT_MAIL_CRON = "app.report_mail.cron";
    public static final String KEY_REPORT_MAIL_TIMEZONE = "app.report_mail.timezone";

    /** 项目信息发布：平台级 JSON 配置（tenant_id=0） */
    public static final String KEY_MARKETPLACE_PROJECT_INFO_CONFIG = "marketplace.project_info.config_json";

    // ===== 租户级通知配置 =====
    /** 来单通知邮箱（租户级，按 tenantId 存储） */
    public static final String KEY_QUICK_QUOTE_NOTIFY_EMAIL = "quote.quick_quote.notify_email";
    /** 是否启用来单邮件通知 */
    public static final String KEY_QUICK_QUOTE_NOTIFY_ENABLED = "quote.quick_quote.notify_enabled";

    // ===== 团队能力展示配置（平台级） =====
    public static final String KEY_SHOWCASE_ENABLED = "quote.showcase.enabled";
    public static final String KEY_SHOWCASE_MODE = "quote.showcase.mode";
    public static final String KEY_SHOWCASE_TEMPLATE_ID = "quote.showcase.template_id";
    public static final String KEY_SHOWCASE_CONTENT_JSON = "quote.showcase.content_json";
    public static final String KEY_SHOWCASE_CUSTOM_HTML = "quote.showcase.custom_html";

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

    private static long platformTenantId() {
        return PLATFORM_SYSTEM_CONFIG_TENANT_ID;
    }

    public boolean isOfficialAiPoolEnabled() {
        String v = mapper.findByKey(platformTenantId(), KEY_OFFICIAL_AI_POOL_ENABLED);
        if (v == null || v.isBlank()) {
            return true;
        }
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
    }

    public void setOfficialAiPoolEnabled(boolean enabled) {
        mapper.upsert(platformTenantId(), KEY_OFFICIAL_AI_POOL_ENABLED, enabled ? "true" : "false");
    }

    public String getOfficialDeepseekApiKey() {
        String v = mapper.findByKey(platformTenantId(), KEY_OFFICIAL_DEEPSEEK_API_KEY);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getOfficialDeepseekApiKeyMasked() {
        String v = getOfficialDeepseekApiKey();
        if (v == null || v.length() <= 8) return v != null && !v.isBlank() ? "****" : null;
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    public void setOfficialDeepseekApiKey(String key) {
        if (key != null && key.contains("****")) return;
        String value = key == null ? "" : key.trim();
        mapper.upsert(platformTenantId(), KEY_OFFICIAL_DEEPSEEK_API_KEY, value);
    }

    public String getOfficialQwenApiKey() {
        String v = mapper.findByKey(platformTenantId(), KEY_OFFICIAL_QWEN_API_KEY);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getOfficialQwenApiKeyMasked() {
        String v = getOfficialQwenApiKey();
        if (v == null || v.length() <= 8) return v != null && !v.isBlank() ? "****" : null;
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    public void setOfficialQwenApiKey(String key) {
        if (key != null && key.contains("****")) return;
        String value = key == null ? "" : key.trim();
        mapper.upsert(platformTenantId(), KEY_OFFICIAL_QWEN_API_KEY, value);
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

    // ===== Mail (SMTP) — 平台级 tenant_id=0（监测台维护）=====
    public String getMailSmtpHost() {
        String v = mapper.findByKey(platformTenantId(), KEY_MAIL_SMTP_HOST);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public Integer getMailSmtpPort() {
        String v = mapper.findByKey(platformTenantId(), KEY_MAIL_SMTP_PORT);
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return null; }
    }

    public String getMailSmtpUsername() {
        String v = mapper.findByKey(platformTenantId(), KEY_MAIL_SMTP_USERNAME);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getMailSmtpPassword() {
        String v = mapper.findByKey(platformTenantId(), KEY_MAIL_SMTP_PASSWORD);
        return (v != null && !v.isBlank()) ? v : null;
    }

    public String getMailFromAddress() {
        String v = mapper.findByKey(platformTenantId(), KEY_MAIL_FROM_ADDRESS);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getMailFromName() {
        String v = mapper.findByKey(platformTenantId(), KEY_MAIL_FROM_NAME);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    public String getPublicBaseUrl() {
        String v = mapper.findByKey(platformTenantId(), KEY_PUBLIC_BASE_URL);
        if (v == null || v.isBlank()) return null;
        String s = v.trim();
        // 去掉尾部 /
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.isBlank() ? null : s;
    }

    /** 写入 SMTP 配置；password 若包含 **** 则视为未修改。 */
    public void saveMailSmtpConfig(String host, Integer port, String username, String password, String fromAddress, String fromName) {
        long tid = platformTenantId();
        if (host != null) mapper.upsert(tid, KEY_MAIL_SMTP_HOST, host.trim());
        if (port != null) mapper.upsert(tid, KEY_MAIL_SMTP_PORT, String.valueOf(port));
        if (username != null) mapper.upsert(tid, KEY_MAIL_SMTP_USERNAME, username.trim());
        if (password != null && !password.contains("****")) mapper.upsert(tid, KEY_MAIL_SMTP_PASSWORD, password);
        if (fromAddress != null) mapper.upsert(tid, KEY_MAIL_FROM_ADDRESS, fromAddress.trim());
        if (fromName != null) mapper.upsert(tid, KEY_MAIL_FROM_NAME, fromName.trim());
    }

    public void setPublicBaseUrl(String baseUrl) {
        if (baseUrl == null) return;
        mapper.upsert(platformTenantId(), KEY_PUBLIC_BASE_URL, baseUrl.trim());
    }

    public String getMailSmtpPasswordMasked() {
        String v = getMailSmtpPassword();
        return v != null && !v.isBlank() ? "****" : null;
    }

    public boolean getPlatformReportMailEnabled() {
        String v = mapper.findByKey(platformTenantId(), KEY_REPORT_MAIL_ENABLED);
        if (v == null || v.isBlank()) {
            return true;
        }
        return Boolean.parseBoolean(v.trim());
    }

    public String getPlatformReportMailCron() {
        String v = mapper.findByKey(platformTenantId(), KEY_REPORT_MAIL_CRON);
        return (v != null && !v.isBlank()) ? v.trim() : "0 30 4 * * *";
    }

    public String getPlatformReportMailTimezone() {
        String v = mapper.findByKey(platformTenantId(), KEY_REPORT_MAIL_TIMEZONE);
        return (v != null && !v.isBlank()) ? v.trim() : "Asia/Shanghai";
    }

    public void savePlatformReportMailSettings(Boolean enabled, String cron, String timezone) {
        long tid = platformTenantId();
        if (enabled != null) {
            mapper.upsert(tid, KEY_REPORT_MAIL_ENABLED, enabled ? "true" : "false");
        }
        if (cron != null && !cron.isBlank()) {
            mapper.upsert(tid, KEY_REPORT_MAIL_CRON, cron.trim());
        }
        if (timezone != null && !timezone.isBlank()) {
            mapper.upsert(tid, KEY_REPORT_MAIL_TIMEZONE, timezone.trim());
        }
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

    /**
     * 注册成功后：若乙方主体模板中法人名称未填，用组织名称预填，避免用户在工作台再次手填。
     */
    public void seedPartyBLegalNameIfEmpty(String orgName) throws JsonProcessingException {
        if (orgName == null || orgName.isBlank()) {
            return;
        }
        Map<String, Object> cur = getQuotePartyBProfile();
        Object ln = cur.get("legalName");
        if (ln != null && !String.valueOf(ln).trim().isEmpty()) {
            return;
        }
        Map<String, Object> patch = new LinkedHashMap<>();
        patch.put("legalName", orgName.trim());
        saveQuotePartyBProfile(patch);
    }

    // ===== 项目信息发布（平台级 tenant_id=0）=====

    /**
     * 默认配置：关闭模块；白名单为空；需先审后发。
     */
    public Map<String, Object> getMarketplaceProjectInfoConfig() {
        String raw = mapper.findByKey(platformTenantId(), KEY_MARKETPLACE_PROJECT_INFO_CONFIG);
        if (raw == null || raw.isBlank()) {
            return defaultMarketplaceProjectInfoConfig();
        }
        try {
            Map<String, Object> m = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            if (m == null) {
                return defaultMarketplaceProjectInfoConfig();
            }
            return m;
        } catch (Exception e) {
            return defaultMarketplaceProjectInfoConfig();
        }
    }

    public void saveMarketplaceProjectInfoConfig(Map<String, Object> incoming) throws JsonProcessingException {
        if (incoming == null) {
            return;
        }
        Map<String, Object> base = new LinkedHashMap<>(getMarketplaceProjectInfoConfig());
        for (Map.Entry<String, Object> e : incoming.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                base.put(e.getKey(), e.getValue());
            }
        }
        String json = objectMapper.writeValueAsString(base);
        mapper.upsert(platformTenantId(), KEY_MARKETPLACE_PROJECT_INFO_CONFIG, json);
    }

    private static Map<String, Object> defaultMarketplaceProjectInfoConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", false);
        m.put("scope", "off");
        m.put("tenantIds", List.of());
        m.put("userIds", List.of());
        m.put("allowGuestBrowseList", false);
        m.put("requireContentReview", true);
        m.put("disclaimerVersion", "2026-04-01");
        return m;
    }

    /** 平台级（tenant_id=0）原始配置值 */
    public String getRawPlatformConfig(String key) {
        return mapper.findByKey(platformTenantId(), key);
    }

    public void upsertPlatformConfig(String key, String value) {
        mapper.upsert(platformTenantId(), key, value != null ? value : "");
    }

    // ===== 来单邮件通知（租户级） =====

    /** 获取指定租户的通知邮箱 */
    public String getQuickQuoteNotifyEmail(long tenantId) {
        String v = mapper.findByKey(tenantId, KEY_QUICK_QUOTE_NOTIFY_EMAIL);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    /** 保存通知邮箱 */
    public void setQuickQuoteNotifyEmail(long tenantId, String email) {
        if (email != null && !email.isBlank()) {
            mapper.upsert(tenantId, KEY_QUICK_QUOTE_NOTIFY_EMAIL, email.trim());
        } else {
            mapper.upsert(tenantId, KEY_QUICK_QUOTE_NOTIFY_EMAIL, "");
        }
    }

    /** 是否启用来单通知 */
    public boolean isQuickQuoteNotifyEnabled(long tenantId) {
        String v = mapper.findByKey(tenantId, KEY_QUICK_QUOTE_NOTIFY_ENABLED);
        return "true".equalsIgnoreCase(v);
    }

    /** 设置是否启用来单通知 */
    public void setQuickQuoteNotifyEnabled(long tenantId, boolean enabled) {
        mapper.upsert(tenantId, KEY_QUICK_QUOTE_NOTIFY_ENABLED, enabled ? "true" : "false");
    }

    // ===== 团队能力展示配置（平台级 tenant_id=0） =====

    public boolean isShowcaseEnabled() {
        return "true".equalsIgnoreCase(mapper.findByKey(0L, KEY_SHOWCASE_ENABLED));
    }

    public void setShowcaseEnabled(boolean enabled) {
        mapper.upsert(0L, KEY_SHOWCASE_ENABLED, enabled ? "true" : "false");
    }

    public String getShowcaseMode() {
        String v = mapper.findByKey(0L, KEY_SHOWCASE_MODE);
        return (v != null && !v.isBlank()) ? v.trim() : "off";
    }

    public void setShowcaseMode(String mode) {
        mapper.upsert(0L, KEY_SHOWCASE_MODE, mode != null ? mode.trim() : "off");
    }

    public String getShowcaseTemplateId() {
        String v = mapper.findByKey(0L, KEY_SHOWCASE_TEMPLATE_ID);
        return (v != null && !v.isBlank()) ? v.trim() : "enterprise";
    }

    public void setShowcaseTemplateId(String templateId) {
        mapper.upsert(0L, KEY_SHOWCASE_TEMPLATE_ID, templateId != null ? templateId.trim() : "enterprise");
    }

    public String getShowcaseContentJson() {
        return mapper.findByKey(0L, KEY_SHOWCASE_CONTENT_JSON);
    }

    public void setShowcaseContentJson(String json) {
        mapper.upsert(0L, KEY_SHOWCASE_CONTENT_JSON, json != null ? json : "");
    }

    public String getShowcaseCustomHtml() {
        return mapper.findByKey(0L, KEY_SHOWCASE_CUSTOM_HTML);
    }

    public void setShowcaseCustomHtml(String html) {
        mapper.upsert(0L, KEY_SHOWCASE_CUSTOM_HTML, html != null ? html : "");
    }

    // ===== 租户级团队能力展示配置 =====

    public boolean isTenantShowcaseEnabled(long tenantId) {
        // 先检查平台是否关停了该租户的展示功能
        String disabled = mapper.findByKey(0L, "quote.showcase.disabled_tenants");
        if (disabled != null) {
            String[] ids = disabled.split(",");
            for (String id : ids) {
                if (id.trim().equals(String.valueOf(tenantId))) return false;
            }
        }
        return "true".equalsIgnoreCase(mapper.findByKey(tenantId, KEY_SHOWCASE_ENABLED));
    }

    public String getTenantShowcaseMode(long tenantId) {
        String v = mapper.findByKey(tenantId, KEY_SHOWCASE_MODE);
        return (v != null && !v.isBlank()) ? v.trim() : "off";
    }

    public String getTenantShowcaseTemplateId(long tenantId) {
        String v = mapper.findByKey(tenantId, KEY_SHOWCASE_TEMPLATE_ID);
        return (v != null && !v.isBlank()) ? v.trim() : "enterprise";
    }

    public String getTenantShowcaseContentJson(long tenantId) {
        return mapper.findByKey(tenantId, KEY_SHOWCASE_CONTENT_JSON);
    }

    public String getTenantShowcaseCustomHtml(long tenantId) {
        return mapper.findByKey(tenantId, KEY_SHOWCASE_CUSTOM_HTML);
    }

    public void setTenantShowcaseEnabled(long tenantId, boolean enabled) {
        mapper.upsert(tenantId, KEY_SHOWCASE_ENABLED, enabled ? "true" : "false");
    }

    public void setTenantShowcaseMode(long tenantId, String mode) {
        mapper.upsert(tenantId, KEY_SHOWCASE_MODE, mode != null ? mode.trim() : "off");
    }

    public void setTenantShowcaseTemplateId(long tenantId, String templateId) {
        mapper.upsert(tenantId, KEY_SHOWCASE_TEMPLATE_ID, templateId != null ? templateId.trim() : "enterprise");
    }

    public void setTenantShowcaseContentJson(long tenantId, String json) {
        mapper.upsert(tenantId, KEY_SHOWCASE_CONTENT_JSON, json != null ? json : "");
    }

    public void setTenantShowcaseCustomHtml(long tenantId, String html) {
        mapper.upsert(tenantId, KEY_SHOWCASE_CUSTOM_HTML, html != null ? html : "");
    }

    /** 平台关停某租户的展示功能 */
    public void disableTenantShowcase(long tenantId) {
        String disabled = mapper.findByKey(0L, "quote.showcase.disabled_tenants");
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (disabled != null && !disabled.isBlank()) {
            for (String id : disabled.split(",")) {
                if (!id.trim().isEmpty()) set.add(id.trim());
            }
        }
        set.add(String.valueOf(tenantId));
        mapper.upsert(0L, "quote.showcase.disabled_tenants", String.join(",", set));
    }

    /** 平台恢复某租户的展示功能 */
    public void enableTenantShowcase(long tenantId) {
        String disabled = mapper.findByKey(0L, "quote.showcase.disabled_tenants");
        if (disabled == null || disabled.isBlank()) return;
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        for (String id : disabled.split(",")) {
            if (!id.trim().isEmpty() && !id.trim().equals(String.valueOf(tenantId))) set.add(id.trim());
        }
        if (set.isEmpty()) {
            mapper.upsert(0L, "quote.showcase.disabled_tenants", "");
        } else {
            mapper.upsert(0L, "quote.showcase.disabled_tenants", String.join(",", set));
        }
    }

    /** 获取被关停展示功能的租户 ID 列表 */
    public java.util.List<Long> getDisabledShowcaseTenantIds() {
        String disabled = mapper.findByKey(0L, "quote.showcase.disabled_tenants");
        java.util.List<Long> list = new java.util.ArrayList<>();
        if (disabled != null && !disabled.isBlank()) {
            for (String id : disabled.split(",")) {
                if (!id.trim().isEmpty()) {
                    try { list.add(Long.parseLong(id.trim())); } catch (NumberFormatException ignored) {}
                }
            }
        }
        return list;
    }
}
