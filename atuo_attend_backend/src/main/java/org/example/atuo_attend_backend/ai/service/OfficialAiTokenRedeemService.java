package org.example.atuo_attend_backend.ai.service;

import org.example.atuo_attend_backend.ai.domain.TokenRedeemCode;
import org.example.atuo_attend_backend.ai.mapper.TokenRedeemCodeMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 官方 API 额度兑换码（监测台生成，租户管理员兑换）。
 */
@Service
public class OfficialAiTokenRedeemService {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LEN = 12;

    private final TokenRedeemCodeMapper redeemCodeMapper;
    private final TenantMapper tenantMapper;
    private final SecureRandom random = new SecureRandom();

    public OfficialAiTokenRedeemService(TokenRedeemCodeMapper redeemCodeMapper, TenantMapper tenantMapper) {
        this.redeemCodeMapper = redeemCodeMapper;
        this.tenantMapper = tenantMapper;
    }

    public String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    /**
     * @return 明文码（仅创建时返回一次）
     */
    @Transactional
    public Map<String, Object> createCode(BigDecimal grantCny, int maxUses, LocalDateTime expiresAt, String note) {
        if (grantCny == null || grantCny.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("grantCny 须为正数");
        }
        if (maxUses < 1) {
            throw new IllegalArgumentException("maxUses 至少为 1");
        }
        String plain = randomCode();
        String hash = sha256Hex(plain);
        if (redeemCodeMapper.findByCodeHash(hash) != null) {
            throw new IllegalStateException("请重试生成");
        }
        TokenRedeemCode row = new TokenRedeemCode();
        row.setCodeHash(hash);
        row.setGrantCny(grantCny.setScale(4, java.math.RoundingMode.HALF_UP));
        row.setMaxUses(maxUses);
        row.setExpiresAt(expiresAt);
        row.setNote(note != null && !note.isBlank() ? note.trim() : null);
        redeemCodeMapper.insert(row);
        Map<String, Object> out = new HashMap<>();
        out.put("id", row.getId());
        out.put("code", plain);
        out.put("grantCny", row.getGrantCny());
        out.put("maxUses", maxUses);
        out.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
        return out;
    }

    @Transactional
    public BigDecimal redeemForTenant(long tenantId, String rawCode) {
        String c = normalizeCode(rawCode);
        if (c.length() < 4) {
            throw new IllegalArgumentException("兑换码格式不正确");
        }
        String hash = sha256Hex(c);
        Map<String, Object> row = redeemCodeMapper.findByCodeHash(hash);
        if (row == null) {
            throw new IllegalArgumentException("兑换码无效");
        }
        long id = ((Number) row.get("id")).longValue();
        Object exp = row.get("expiresAt");
        if (exp != null) {
            LocalDateTime expAt = exp instanceof LocalDateTime
                    ? (LocalDateTime) exp
                    : LocalDateTime.parse(String.valueOf(exp));
            if (expAt.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("兑换码已过期");
            }
        }
        int n = redeemCodeMapper.tryIncrementUse(id);
        if (n == 0) {
            throw new IllegalArgumentException("兑换码已用尽或已过期");
        }
        BigDecimal grant = row.get("grantCny") instanceof BigDecimal b
                ? b : new BigDecimal(String.valueOf(row.get("grantCny")));
        tenantMapper.addOfficialApiCnyBalance(tenantId, grant);
        redeemCodeMapper.insertLog(id, tenantId, grant);
        return grant;
    }

    public static LocalDateTime parseExpires(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("expiresAt 须为 ISO-8601 日期时间，或留空");
        }
    }
}
