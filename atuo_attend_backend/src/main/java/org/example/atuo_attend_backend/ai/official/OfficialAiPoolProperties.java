package org.example.atuo_attend_backend.ai.official;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 官方 API 池计费。
 * 单位：元 / 百万 tokens。
 *
 * 定价策略：在官网 API 成本基础上加价，保障平台盈利。
 * - DeepSeek V3.2 官网成本：输入 2.0、输出 3.0（缓存未命中）
 * - Qwen3.5-Flash 官网成本：输入 0.2、输出 2.0
 * - Qwen3.5-Plus 官网成本：输入 0.8、输出 4.8
 * 系统定价取中位偏上，覆盖不同模型版本的成本差异并预留利润空间。
 */
@Component
@ConfigurationProperties(prefix = "app.official-ai")
public class OfficialAiPoolProperties {

    private boolean enabled = true;
    /** 新租户注册赠送官方额度（元） */
    private BigDecimal registrationGrantCny = new BigDecimal("20");
    /** DeepSeek：输入 / 百万 tokens（元）— 官网成本 2.0，定价含利润空间 */
    private BigDecimal deepseekInputPer1mCny = new BigDecimal("4.0");
    private BigDecimal deepseekOutputPer1mCny = new BigDecimal("6.0");
    /** Qwen（多模态等）：输入 / 百万 tokens（元）— 介于 Flash(0.2/2.0) 和 Plus(0.8/4.8) 之间，含利润空间 */
    private BigDecimal qwenInputPer1mCny = new BigDecimal("1.0");
    private BigDecimal qwenOutputPer1mCny = new BigDecimal("4.0");

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
