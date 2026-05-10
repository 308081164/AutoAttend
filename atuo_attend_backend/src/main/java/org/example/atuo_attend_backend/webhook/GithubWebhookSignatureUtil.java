package org.example.atuo_attend_backend.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 校验 GitHub Webhook {@code X-Hub-Signature-256}（HmacSHA256）。
 */
public final class GithubWebhookSignatureUtil {

    private GithubWebhookSignatureUtil() {
    }

    /**
     * @param payload         原始请求体字节
     * @param signatureHeader 请求头 {@code X-Hub-Signature-256}，形如 {@code sha256=...}
     * @param secret          与 GitHub Webhook 配置中 Secret 一致；为 null 或空白则跳过校验（返回 true）
     */
    public static boolean isValid(byte[] payload, String signatureHeader, String secret) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expectedHex = signatureHeader.substring("sha256=".length()).trim();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(payload);
            String actualHex = HexFormat.of().formatHex(sig);
            return constantTimeEquals(expectedHex.toLowerCase(), actualHex.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
