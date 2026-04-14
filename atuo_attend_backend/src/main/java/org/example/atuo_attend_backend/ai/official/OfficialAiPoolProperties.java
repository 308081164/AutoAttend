package org.example.atuo_attend_backend.ai.official;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 官方 API 池计费（默认按阿里云灵积/DashScope 常见量级近似；可在配置中覆盖）。
 * 单位：元 / 百万 tokens。
 */
@Component
@ConfigurationProperties(prefix = "app.official-ai")
public class OfficialAiPoolProperties {

    private boolean enabled = true;
    /** 新租户注册赠送官方额度（元） */
    private BigDecimal registrationGrantCny = new BigDecimal("20");
    /** DeepSeek：输入 / 百万 tokens（元） */
    private BigDecimal deepseekInputPer1mCny = new BigDecimal("1.0");
    private BigDecimal deepseekOutputPer1mCny = new BigDecimal("2.0");
    /** Qwen（多模态等）：输入 / 百万 tokens（元） */
    private BigDecimal qwenInputPer1mCny = new BigDecimal("0.3");
    private BigDecimal qwenOutputPer1mCny = new BigDecimal("0.6");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getRegistrationGrantCny() {
        return registrationGrantCny;
    }

    public void setRegistrationGrantCny(BigDecimal registrationGrantCny) {
        this.registrationGrantCny = registrationGrantCny;
    }

    public BigDecimal getDeepseekInputPer1mCny() {
        return deepseekInputPer1mCny;
    }

    public void setDeepseekInputPer1mCny(BigDecimal deepseekInputPer1mCny) {
        this.deepseekInputPer1mCny = deepseekInputPer1mCny;
    }

    public BigDecimal getDeepseekOutputPer1mCny() {
        return deepseekOutputPer1mCny;
    }

    public void setDeepseekOutputPer1mCny(BigDecimal deepseekOutputPer1mCny) {
        this.deepseekOutputPer1mCny = deepseekOutputPer1mCny;
    }

    public BigDecimal getQwenInputPer1mCny() {
        return qwenInputPer1mCny;
    }

    public void setQwenInputPer1mCny(BigDecimal qwenInputPer1mCny) {
        this.qwenInputPer1mCny = qwenInputPer1mCny;
    }

    public BigDecimal getQwenOutputPer1mCny() {
        return qwenOutputPer1mCny;
    }

    public void setQwenOutputPer1mCny(BigDecimal qwenOutputPer1mCny) {
        this.qwenOutputPer1mCny = qwenOutputPer1mCny;
    }
}
