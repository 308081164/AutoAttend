package org.example.atuo_attend_backend.admin.sms;

import org.example.atuo_attend_backend.admin.PhoneNormalizer;
import org.example.atuo_attend_backend.admin.domain.AdminSmsCode;
import org.example.atuo_attend_backend.admin.mapper.AdminSmsCodeMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;

/**
 * 管理员登录/注册短信验证码：发送与校验。
 */
@Service
public class AdminSmsService {

    public static final String PURPOSE_LOGIN = "login";
    public static final String PURPOSE_REGISTER = "register";
    /** 协作成员绑定手机号 */
    public static final String PURPOSE_BIND_PHONE = "bind_phone";

    private static final int CODE_LENGTH = 6;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private final SecureRandom random = new SecureRandom();

    private final AdminSmsProperties props;
    private final AliyunSmsClient aliyunSmsClient;
    private final AdminSmsCodeMapper smsCodeMapper;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final BizUserMapper bizUserMapper;

    public AdminSmsService(AdminSmsProperties props,
                           AliyunSmsClient aliyunSmsClient,
                           AdminSmsCodeMapper smsCodeMapper,
                           TenantAdminUserMapper tenantAdminUserMapper,
                           BizUserMapper bizUserMapper) {
        this.props = props;
        this.aliyunSmsClient = aliyunSmsClient;
        this.smsCodeMapper = smsCodeMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.bizUserMapper = bizUserMapper;
    }

    public boolean smsLoginEnabled() {
        return props.isConfigured();
    }

    /**
     * 发送验证码（含频控与业务校验）。
     */
    @Transactional
    public void sendCode(String phoneRaw, String purpose) throws Exception {
        if (!props.isConfigured()) {
            throw new IllegalStateException("短信服务未配置");
        }
        String purposeNorm = normalizePurpose(purpose);
        String phone = PhoneNormalizer.normalize(phoneRaw);
        if (phone == null) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (PURPOSE_LOGIN.equals(purposeNorm)) {
            if (tenantAdminUserMapper.findByPhone(phone) != null) {
                // 管理员登录
            } else {
                long n = bizUserMapper.countByPhoneE164(phone);
                if (n == 1) {
                    // 已绑定手机的协作成员登录
                } else if (n == 0) {
                    throw new IllegalArgumentException("该手机号未注册或未绑定成员账号");
                } else {
                    throw new IllegalArgumentException("该手机号存在多条成员记录，请联系管理员处理");
                }
            }
        } else if (PURPOSE_REGISTER.equals(purposeNorm)) {
            if (tenantAdminUserMapper.countByPhone(phone) > 0) {
                throw new IllegalArgumentException("该手机号已注册");
            }
        } else if (PURPOSE_BIND_PHONE.equals(purposeNorm)) {
            // 由业务层预先校验；此处仅发送验证码
        } else {
            throw new IllegalArgumentException("无效的用途");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = smsCodeMapper.findLastSendTime(phone, purposeNorm);
        if (last != null) {
            long elapsed = java.time.Duration.between(last, now).getSeconds();
            if (elapsed < props.getResendIntervalSeconds()) {
                throw new IllegalStateException("发送过于频繁，请稍后再试");
            }
        }

        ZoneId zone = props.getSmsRateLimitZone();
        LocalDateTime dayStart = LocalDate.now(zone).atStartOfDay();
        long sentToday = smsCodeMapper.countSendsSince(phone, dayStart);
        int dailyCap = props.getMaxSendsPerPhonePerDay();
        if (dailyCap > 0 && sentToday >= dailyCap) {
            throw new IllegalStateException("今日验证码发送次数已达上限，请明日再试");
        }

        String code = randomDigits(random, CODE_LENGTH);
        String hash = sha256Hex(code);
        LocalDateTime expires = now.plusMinutes(props.getCodeTtlMinutes());

        smsCodeMapper.deleteUnusedForPhonePurpose(phone, purposeNorm);
        smsCodeMapper.insert(phone, purposeNorm, hash, expires);

        try {
            aliyunSmsClient.sendVerificationCode(phone, code);
        } catch (Exception e) {
            smsCodeMapper.deleteUnusedForPhonePurpose(phone, purposeNorm);
            throw e;
        }
    }

    /**
     * 校验验证码；成功则标记已使用（一次性）。
     *
     * @return 错误信息，null 表示通过
     */
    @Transactional
    public String verifyAndConsume(String phoneRaw, String purpose, String smsCode) {
        if (!props.isConfigured()) {
            return null;
        }
        if (smsCode == null || smsCode.isBlank()) {
            return "请输入短信验证码";
        }
        String purposeNorm = normalizePurpose(purpose);
        String phone = PhoneNormalizer.normalize(phoneRaw);
        if (phone == null) {
            return "手机号格式不正确";
        }
        String digits = smsCode.trim().replaceAll("\\s+", "");
        if (!digits.matches("^\\d{6}$")) {
            return "验证码须为 6 位数字";
        }

        LocalDateTime now = LocalDateTime.now();
        AdminSmsCode row = smsCodeMapper.findLatestValid(phone, purposeNorm, now);
        if (row == null) {
            return "验证码无效或已过期，请重新获取";
        }
        if (row.getVerifyAttempts() != null && row.getVerifyAttempts() >= MAX_VERIFY_ATTEMPTS) {
            return "验证次数过多，请重新获取验证码";
        }
        String expectedHash = row.getCodeHash();
        String actualHash = sha256Hex(digits);
        if (!MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8))) {
            smsCodeMapper.incrementAttempts(row.getId());
            return "验证码错误";
        }
        int n = smsCodeMapper.markUsed(row.getId(), now);
        if (n <= 0) {
            return "验证码已使用或已过期";
        }
        return null;
    }

    private static String normalizePurpose(String purpose) {
        if (purpose == null) {
            return "";
        }
        String p = purpose.trim().toLowerCase();
        if (PURPOSE_REGISTER.equals(p) || "reg".equals(p)) {
            return PURPOSE_REGISTER;
        }
        if (PURPOSE_BIND_PHONE.equals(p) || "bind-phone".equals(p) || "bindphone".equals(p)) {
            return PURPOSE_BIND_PHONE;
        }
        return PURPOSE_LOGIN;
    }

    private static String randomDigits(SecureRandom rnd, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
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
}
