package org.example.atuo_attend_backend.ai.official;

import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.client.QwenClient;
import org.example.atuo_attend_backend.ai.service.AiUsageRecordService;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 官方 API 池：优先使用用户自备 Key；无 Key 时用平台 Key 并从租户官方余额按量扣费（元）。
 */
@Service
public class OfficialAiPoolService {

    private static final Logger log = LoggerFactory.getLogger(OfficialAiPoolService.class);
    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private final OfficialAiPoolProperties properties;
    private final SystemConfigService systemConfigService;
    private final TenantMapper tenantMapper;
    private final AiUsageRecordService aiUsageRecordService;

    public OfficialAiPoolService(OfficialAiPoolProperties properties,
                                 SystemConfigService systemConfigService,
                                 TenantMapper tenantMapper,
                                 AiUsageRecordService aiUsageRecordService) {
        this.properties = properties;
        this.systemConfigService = systemConfigService;
        this.tenantMapper = tenantMapper;
        this.aiUsageRecordService = aiUsageRecordService;
    }

    public boolean isFeatureEnabled() {
        return properties.isEnabled() && systemConfigService.isOfficialAiPoolEnabled();
    }

    /**
     * 新租户注册赠送官方额度（元）。
     */
    public void grantRegistrationBonus(long tenantId) {
        if (!isFeatureEnabled()) {
            return;
        }
        BigDecimal grant = properties.getRegistrationGrantCny();
        if (grant == null || grant.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            tenantMapper.addOfficialApiCnyBalance(tenantId, grant);
        } catch (Exception e) {
            log.warn("grantRegistrationBonus failed tenantId={}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * 存量租户首次进入工作台：余额为 0 时补发欢迎额度（与注册赠送同额）。
     */
    public void ensureWelcomeOfficialBalance(long tenantId) {
        if (!isFeatureEnabled()) {
            return;
        }
        BigDecimal grant = properties.getRegistrationGrantCny();
        if (grant == null || grant.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            Tenant t = tenantMapper.findById(tenantId);
            if (t == null) {
                return;
            }
            BigDecimal bal = t.getOfficialApiCnyBalance() != null ? t.getOfficialApiCnyBalance() : BigDecimal.ZERO;
            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                return;
            }
            tenantMapper.addOfficialApiCnyBalance(tenantId, grant);
        } catch (Exception e) {
            log.warn("ensureWelcomeOfficialBalance failed tenantId={}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * DeepSeek：官方池优先（开关开启、已配置平台 Key 且租户余额大于 0）；否则使用用户自备 Key。
     */
    public DeepSeekResolution resolveDeepSeek(long tenantId, String userApiKey) {
        if (isFeatureEnabled()) {
            String officialKey = systemConfigService.getOfficialDeepseekApiKey();
            if (officialKey != null && !officialKey.isBlank()) {
                Tenant t = tenantMapper.findById(tenantId);
                BigDecimal bal = t != null && t.getOfficialApiCnyBalance() != null
                        ? t.getOfficialApiCnyBalance() : BigDecimal.ZERO;
                if (bal.compareTo(BigDecimal.ZERO) > 0) {
                    return new DeepSeekResolution(officialKey.trim(), true, null);
                }
            }
        }
        if (userApiKey != null && !userApiKey.isBlank()) {
            return new DeepSeekResolution(userApiKey.trim(), false, null);
        }
        return new DeepSeekResolution(null, false, null);
    }

    /**
     * Qwen：官方池优先，否则用户 Key。
     */
    public QwenResolution resolveQwen(long tenantId, String userApiKey) {
        if (isFeatureEnabled()) {
            String officialKey = systemConfigService.getOfficialQwenApiKey();
            if (officialKey != null && !officialKey.isBlank()) {
                Tenant t = tenantMapper.findById(tenantId);
                BigDecimal bal = t != null && t.getOfficialApiCnyBalance() != null
                        ? t.getOfficialApiCnyBalance() : BigDecimal.ZERO;
                if (bal.compareTo(BigDecimal.ZERO) > 0) {
                    return new QwenResolution(officialKey.trim(), true, null);
                }
            }
        }
        if (userApiKey != null && !userApiKey.isBlank()) {
            return new QwenResolution(userApiKey.trim(), false, null);
        }
        return new QwenResolution(null, false, null);
    }

    public BigDecimal estimateCostCny(String provider, int inputTokens, int outputTokens) {
        if (inputTokens < 0) inputTokens = 0;
        if (outputTokens < 0) outputTokens = 0;
        BigDecimal inM = BigDecimal.valueOf(inputTokens).divide(MILLION, 12, RoundingMode.HALF_UP);
        BigDecimal outM = BigDecimal.valueOf(outputTokens).divide(MILLION, 12, RoundingMode.HALF_UP);
        String p = provider != null ? provider.toLowerCase() : "";
        if ("qwen".equals(p)) {
            return inM.multiply(nz(properties.getQwenInputPer1mCny()))
                    .add(outM.multiply(nz(properties.getQwenOutputPer1mCny())))
                    .setScale(8, RoundingMode.HALF_UP);
        }
        return inM.multiply(nz(properties.getDeepseekInputPer1mCny()))
                .add(outM.multiply(nz(properties.getDeepseekOutputPer1mCny())))
                .setScale(8, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * 调用成功后扣减官方余额（不低于 0）。
     */
    public void deductAfterOfficialCall(long tenantId, BigDecimal costCny) {
        if (costCny == null || costCny.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            tenantMapper.deductOfficialApiCnyCapped(tenantId, costCny);
        } catch (Exception e) {
            log.warn("deduct official balance failed tenantId={} cost={}: {}", tenantId, costCny, e.getMessage());
        }
    }

    /** 调用 DeepSeek（用户 Key 优先，否则官方池） */
    public DeepSeekChatOutcome chatDeepSeek(DeepSeekClient client, long tenantId, String userApiKey, String model,
                                            List<DeepSeekClient.ChatMessage> messages, boolean requestJson, Integer maxCompletionTokens) {
        DeepSeekResolution r = resolveDeepSeek(tenantId, userApiKey);
        if (r.apiKey() == null) {
            return new DeepSeekChatOutcome(null, false);
        }
        DeepSeekClient.ChatResult res = client.chatWithUsage(r.apiKey(), model, messages, requestJson, maxCompletionTokens);
        return new DeepSeekChatOutcome(res, r.official());
    }

    public void recordDeepSeekUsage(long tenantId, DeepSeekClient.ChatResult result, boolean officialPool,
                                    String repoFullName, String commitSha) {
        if (result == null) {
            return;
        }
        if (officialPool) {
            BigDecimal cost = estimateCostCny("deepseek", result.getInputTokens(), result.getOutputTokens());
            deductAfterOfficialCall(tenantId, cost);
            aiUsageRecordService.recordOfficial(tenantId, "deepseek", result.getModel(),
                    result.getInputTokens(), result.getOutputTokens(), repoFullName, commitSha, cost);
        } else {
            aiUsageRecordService.recordPersonal(tenantId, "deepseek", result.getModel(),
                    result.getInputTokens(), result.getOutputTokens(), repoFullName, commitSha);
        }
    }

    public QwenChatOutcome chatQwen(QwenClient client, long tenantId, String userApiKey, String model,
                                    List<QwenClient.ChatMessage> messages, boolean responseJson) {
        QwenResolution r = resolveQwen(tenantId, userApiKey);
        if (r.apiKey() == null) {
            return new QwenChatOutcome(null, false);
        }
        QwenClient.ChatResult res = client.chat(r.apiKey(), model, messages, responseJson);
        return new QwenChatOutcome(res, r.official());
    }

    public void recordQwenUsage(long tenantId, QwenClient.ChatResult result, boolean officialPool) {
        if (result == null || result.isError()) {
            return;
        }
        if (officialPool) {
            BigDecimal cost = estimateCostCny("qwen", result.getInputTokens(), result.getOutputTokens());
            deductAfterOfficialCall(tenantId, cost);
            aiUsageRecordService.recordOfficial(tenantId, "qwen", result.getModel(),
                    result.getInputTokens(), result.getOutputTokens(), null, null, cost);
        } else {
            aiUsageRecordService.recordPersonal(tenantId, "qwen", result.getModel(),
                    result.getInputTokens(), result.getOutputTokens(), null, null);
        }
    }

    public record DeepSeekResolution(String apiKey, boolean official, BigDecimal reservedCost) {}
    public record QwenResolution(String apiKey, boolean official, BigDecimal reservedCost) {}
    public record DeepSeekChatOutcome(DeepSeekClient.ChatResult result, boolean officialPool) {}
    public record QwenChatOutcome(QwenClient.ChatResult result, boolean officialPool) {}
}
