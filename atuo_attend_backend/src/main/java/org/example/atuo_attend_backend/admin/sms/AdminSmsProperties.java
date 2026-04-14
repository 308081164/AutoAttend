package org.example.atuo_attend_backend.admin.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * 阿里云短信：优先读环境变量（与部署约定一致），未配置时验证码功能关闭。
 */
@Component
public class AdminSmsProperties {

    @Value("${ALIYUN_ACCESS_KEY_ID:}")
    private String accessKeyId;

    @Value("${ALIYUN_ACCESS_KEY_SECRET:}")
    private String accessKeySecret;

    @Value("${ALIYUN_SMS_SIGN_NAME:}")
    private String signName;

    @Value("${ALIYUN_SMS_TEMPLATE_CODE:}")
    private String templateCode;

    /** OpenAPI 接入点，默认 dysmsapi.aliyuncs.com */
    @Value("${ALIYUN_SMS_ENDPOINT:dysmsapi.aliyuncs.com}")
    private String endpoint;

    /** 模板中验证码占位 JSON 的键名，常见为 code */
    @Value("${ALIYUN_SMS_TEMPLATE_PARAM_CODE:code}")
    private String templateParamCodeKey;

    /** 短信有效时间（分钟） */
    @Value("${ADMIN_SMS_CODE_TTL_MINUTES:5}")
    private int codeTtlMinutes;

    /** 同一号码发送间隔（秒），默认 120（两分钟） */
    @Value("${ADMIN_SMS_RESEND_INTERVAL_SECONDS:120}")
    private int resendIntervalSeconds;

    /** 同一号码自然日内最多发送次数（按 {@link #smsRateLimitZoneId} 计日） */
    @Value("${ADMIN_SMS_MAX_SENDS_PER_PHONE_PER_DAY:10}")
    private int maxSendsPerPhonePerDay;

    /** 日限额统计所用时区（与「自然日」对齐） */
    @Value("${ADMIN_SMS_RATE_LIMIT_ZONE:Asia/Shanghai}")
    private String smsRateLimitZone;

    public boolean isConfigured() {
        return accessKeyId != null && !accessKeyId.isBlank()
                && accessKeySecret != null && !accessKeySecret.isBlank()
                && signName != null && !signName.isBlank()
                && templateCode != null && !templateCode.isBlank();
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getSignName() {
        return signName;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getTemplateParamCodeKey() {
        return templateParamCodeKey;
    }

    public int getCodeTtlMinutes() {
        return codeTtlMinutes;
    }

    public int getResendIntervalSeconds() {
        return resendIntervalSeconds;
    }

    public int getMaxSendsPerPhonePerDay() {
        return maxSendsPerPhonePerDay;
    }

    public ZoneId getSmsRateLimitZone() {
        try {
            return ZoneId.of(smsRateLimitZone != null && !smsRateLimitZone.isBlank()
                    ? smsRateLimitZone.trim() : "Asia/Shanghai");
        } catch (Exception e) {
            return ZoneId.of("Asia/Shanghai");
        }
    }
}
