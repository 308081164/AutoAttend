package org.example.atuo_attend_backend.prototype.penpot;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.example.atuo_attend_backend.prototype.config.PenpotProperties;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.domain.TenantPenpotCredential;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantPenpotCredentialMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 租户首次使用 Penpot Beta 时：在 Penpot 注册独立账号、登录并创建 Access Token，密文落库（一租户一 Token）。
 */
@Service
public class TenantPenpotOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(TenantPenpotOnboardingService.class);

    private final PenpotProperties penpotProperties;
    private final PenpotRpcClient rpcClient;
    private final NexusCryptoService cryptoService;
    private final TenantPenpotCredentialMapper credentialMapper;
    private final TenantMapper tenantMapper;

    public TenantPenpotOnboardingService(PenpotProperties penpotProperties,
                                         PenpotRpcClient rpcClient,
                                         NexusCryptoService cryptoService,
                                         TenantPenpotCredentialMapper credentialMapper,
                                         TenantMapper tenantMapper) {
        this.penpotProperties = penpotProperties;
        this.rpcClient = rpcClient;
        this.cryptoService = cryptoService;
        this.credentialMapper = credentialMapper;
        this.tenantMapper = tenantMapper;
    }

    /**
     * 返回解密后的租户 Access Token；必要时完成开户。
     */
    public boolean hasCredential(long tenantId) {
        TenantPenpotCredential row = credentialMapper.findByTenantId(tenantId);
        return row != null && StringUtils.hasText(row.getAccessTokenEnc());
    }

    public synchronized String getOrCreateTenantAccessToken(long tenantId) {
        TenantPenpotCredential row = credentialMapper.findByTenantId(tenantId);
        if (row != null && StringUtils.hasText(row.getAccessTokenEnc())) {
            return cryptoService.decrypt(row.getAccessTokenEnc());
        }
        provisionTenant(tenantId);
        TenantPenpotCredential again = credentialMapper.findByTenantId(tenantId);
        if (again == null || !StringUtils.hasText(again.getAccessTokenEnc())) {
            throw new IllegalStateException("Penpot 租户凭证落库失败");
        }
        return cryptoService.decrypt(again.getAccessTokenEnc());
    }

    private void provisionTenant(long tenantId) {
        Tenant t = tenantMapper.findById(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        String email = buildEmail(tenantId);
        String password = deriveTenantPassword(tenantId);
        String fullname = buildFullName(t);

        String cookie;
        try {
            cookie = rpcClient.loginFetchAuthCookie(email, password);
        } catch (IllegalStateException loginFirst) {
            log.debug("Penpot 登录失败，尝试注册: {}", loginFirst.getMessage());
            JsonNode prepare = rpcClient.commandNoAuth("prepare-register-profile", Map.of(
                    "fullname", fullname,
                    "email", email,
                    "password", password,
                    "createWelcomeFile", false,
                    "acceptNewsletterUpdates", false
            ));
            String token = textField(prepare, "token");
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("prepare-register-profile 未返回 token");
            }
            try {
                rpcClient.commandNoAuth("register-profile", Map.of("token", token));
            } catch (IllegalStateException ex) {
                log.warn("register-profile: {}", ex.getMessage());
            }
            cookie = rpcClient.loginFetchAuthCookie(email, password);
        }
        String tokenName = safeTokenName(tenantId);
        JsonNode tokNode = rpcClient.commandWithCookie("create-access-token",
                Map.of("name", tokenName),
                cookie);
        String accessToken = textField(tokNode, "token");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("create-access-token 未返回 token");
        }

        TenantPenpotCredential row = new TenantPenpotCredential();
        row.setTenantId(tenantId);
        row.setPenpotEmail(email);
        row.setPasswordEnc(cryptoService.encrypt(password));
        row.setAccessTokenEnc(cryptoService.encrypt(accessToken.trim()));
        credentialMapper.insert(row);
    }

    private String buildEmail(long tenantId) {
        String domain = penpotProperties.getTenantEmailDomain() != null
                ? penpotProperties.getTenantEmailDomain().trim()
                : "penpot.local";
        if (domain.isEmpty()) {
            domain = "penpot.local";
        }
        return "aa-t" + tenantId + "@" + domain;
    }

    private String buildFullName(Tenant t) {
        String n = t.getName() != null ? t.getName().trim() : "";
        if (n.isEmpty()) {
            n = "租户 " + t.getId();
        }
        if (n.length() > 120) {
            n = n.substring(0, 120);
        }
        return n + " / Penpot";
    }

    private String safeTokenName(long tenantId) {
        String p = penpotProperties.getTenantTokenNamePrefix() != null
                ? penpotProperties.getTenantTokenNamePrefix().trim()
                : "autoattend";
        if (p.isEmpty()) {
            p = "autoattend";
        }
        String base = p + "-tenant-" + tenantId;
        return base.length() <= 250 ? base : base.substring(0, 250);
    }

    /**
     * 确定性密码：同一 tenantId 可重复推导，便于幂等与异常恢复（仍仅存密文入库）。
     */
    private String deriveTenantPassword(long tenantId) {
        String pepper = penpotProperties.getTenantCredentialPepper();
        if (pepper == null || pepper.isBlank()) {
            pepper = "autoattend-penpot-tenant-v1";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest((pepper + "|" + tenantId).getBytes(StandardCharsets.UTF_8));
            String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(h);
            // Penpot 密码策略：长度与复杂度；加入固定后缀满足常见「字母+数字」要求
            String core = b64.length() >= 24 ? b64.substring(0, 24) : b64;
            return core + "Aa1";
        } catch (Exception e) {
            throw new IllegalStateException("派生租户 Penpot 密码失败", e);
        }
    }

    private static String textField(JsonNode n, String field) {
        if (n == null || n.isNull()) {
            return null;
        }
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            v = n.get(capitalize(field));
        }
        if (v == null || v.isNull()) {
            return null;
        }
        return v.asText();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
