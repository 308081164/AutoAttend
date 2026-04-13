package org.example.atuo_attend_backend.admin.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用阿里云 SendSms（验证码类模板）。
 */
@Service
public class AliyunSmsClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AdminSmsProperties props;

    public AliyunSmsClient(AdminSmsProperties props) {
        this.props = props;
    }

    /**
     * @param phoneE164 如 +86138xxxx
     * @param code      6 位数字验证码
     * @return 阿里云返回的 Code，成功一般为 OK
     */
    public String sendVerificationCode(String phoneE164, String code) throws Exception {
        if (!props.isConfigured()) {
            throw new IllegalStateException("短信未配置");
        }
        String domestic = toDomestic11(phoneE164);
        Config config = new Config()
                .setAccessKeyId(props.getAccessKeyId().trim())
                .setAccessKeySecret(props.getAccessKeySecret().trim())
                .setEndpoint(props.getEndpoint().trim());
        Client client = new Client(config);
        Map<String, String> param = new HashMap<>();
        param.put(props.getTemplateParamCodeKey(), code);
        String templateParam = objectMapper.writeValueAsString(param);
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(domestic)
                .setSignName(props.getSignName().trim())
                .setTemplateCode(props.getTemplateCode().trim())
                .setTemplateParam(templateParam);
        RuntimeOptions runtime = new RuntimeOptions();
        SendSmsResponse resp = client.sendSmsWithOptions(request, runtime);
        String bizCode = resp.getBody() != null ? resp.getBody().getCode() : null;
        String msg = resp.getBody() != null ? resp.getBody().getMessage() : null;
        if (bizCode == null || !"OK".equalsIgnoreCase(bizCode)) {
            log.warn("Aliyun SendSms failed: code={} message={} phoneSuffix={}", bizCode, msg,
                    domestic.length() > 4 ? domestic.substring(domestic.length() - 4) : "****");
            throw new IllegalStateException(msg != null ? msg : "短信发送失败");
        }
        return bizCode;
    }

    /** 模板要求国内 11 位，不带 +86 前缀 */
    private static String toDomestic11(String e164) {
        if (e164 == null) {
            return "";
        }
        String d = e164.trim();
        if (d.startsWith("+86") && d.length() >= 14) {
            return d.substring(3);
        }
        if (d.startsWith("86") && d.length() == 13) {
            return d.substring(2);
        }
        if (d.matches("^1[3-9]\\d{9}$")) {
            return d;
        }
        return d.replace("+", "").replaceAll("\\s+", "");
    }
}
