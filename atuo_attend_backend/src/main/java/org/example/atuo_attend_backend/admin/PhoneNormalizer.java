package org.example.atuo_attend_backend.admin;

/**
 * 将输入规范为 E.164（含国际区号）。支持已带 + 的号码、中国大陆 11 位手机号。
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    /**
     * @return E.164 如 +8613812345678，无法识别时返回 null
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("\\s+", "");
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("+")) {
            String digits = s.substring(1).replaceAll("[^0-9]", "");
            if (digits.length() < 8 || digits.length() > 15) {
                return null;
            }
            return "+" + digits;
        }
        if (s.matches("^1[3-9]\\d{9}$")) {
            return "+86" + s;
        }
        if (s.matches("^\\d{8,15}$")) {
            return "+" + s;
        }
        return null;
    }
}
