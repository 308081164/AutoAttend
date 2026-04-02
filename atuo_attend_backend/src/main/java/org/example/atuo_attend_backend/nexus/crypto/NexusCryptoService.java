package org.example.atuo_attend_backend.nexus.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Nexus 凭证加密/解密（开发用 key 默认值）。
 * 说明：加密算法仅用于落库保护，生产环境请通过环境变量/密钥管理替换 dev key。
 */
@Service
public class NexusCryptoService {

    private static final int IV_LEN_BYTES = 12;
    private static final int GCM_TAG_LEN_BITS = 128;

    private final String devKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public NexusCryptoService(@Value("${nexus.crypto.dev-key}") String devKey) {
        this.devKey = devKey;
    }

    public String encrypt(String plain) {
        if (plain == null) throw new IllegalArgumentException("plain is null");
        try {
            byte[] keyBytes = deriveAesKey(devKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] iv = new byte[IV_LEN_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LEN_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            ByteBuffer bb = ByteBuffer.allocate(iv.length + cipherText.length);
            bb.put(iv);
            bb.put(cipherText);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new IllegalStateException("encrypt failed", e);
        }
    }

    public String decrypt(String enc) {
        if (enc == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(enc);
            if (all.length < IV_LEN_BYTES + 16) {
                throw new IllegalArgumentException("cipher text too short");
            }
            ByteBuffer bb = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LEN_BYTES];
            bb.get(iv);
            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            byte[] keyBytes = deriveAesKey(devKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LEN_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt failed", e);
        }
    }

    private static byte[] deriveAesKey(String key) throws Exception {
        // 统一派生 256-bit AES key：SHA-256(keyString)
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest((key == null ? "" : key).getBytes(StandardCharsets.UTF_8));
    }
}

