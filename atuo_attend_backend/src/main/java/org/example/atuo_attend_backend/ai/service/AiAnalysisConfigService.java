package org.example.atuo_attend_backend.ai.service;

import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.mapper.AiAnalysisConfigMapper;
import org.springframework.stereotype.Service;

/**
 * AI 分析配置：当前版本仅 DeepSeek，API Key 由管理员在配置页填写。
 */
@Service
public class AiAnalysisConfigService {

    private static final String PROVIDER_DEEPSEEK = "deepseek";
    private static final String PROVIDER_QWEN = "qwen";

    private final AiAnalysisConfigMapper configMapper;

    public AiAnalysisConfigService(AiAnalysisConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public AiAnalysisConfig getConfig() {
        AiAnalysisConfig c = configMapper.findByProvider(PROVIDER_DEEPSEEK);
        if (c == null) {
            c = new AiAnalysisConfig();
            c.setProvider(PROVIDER_DEEPSEEK);
            c.setApiKey(null);
            c.setEnabled(false);
            c.setModel("deepseek-chat");
            c.setPromptVersion("v1");
            c.setMaxDiffChars(100000);
        }
        return c;
    }

    public AiAnalysisConfig getQwenConfig() {
        AiAnalysisConfig c = configMapper.findByProvider(PROVIDER_QWEN);
        if (c == null) {
            c = new AiAnalysisConfig();
            c.setProvider(PROVIDER_QWEN);
            c.setApiKey(null);
            c.setEnabled(false);
            c.setModel("qwen-vl-plus");
        }
        return c;
    }

    public AiAnalysisConfig getQwenConfigMasked() {
        AiAnalysisConfig c = getQwenConfig();
        if (c != null && c.getApiKey() != null && !c.getApiKey().isBlank()) {
            String k = c.getApiKey();
            if (k.length() <= 8) {
                c.setApiKey("****");
            } else {
                c.setApiKey(k.substring(0, 4) + "****" + k.substring(k.length() - 4));
            }
        }
        return c;
    }

    /** 返回用于 API 响应的配置，API Key 脱敏（仅显示前后几位）。 */
    public AiAnalysisConfig getConfigMasked() {
        AiAnalysisConfig c = getConfig();
        if (c != null && c.getApiKey() != null && !c.getApiKey().isBlank()) {
            String k = c.getApiKey();
            if (k.length() <= 8) {
                c.setApiKey("****");
            } else {
                c.setApiKey(k.substring(0, 4) + "****" + k.substring(k.length() - 4));
            }
        }
        return c;
    }

    public void updateConfig(String apiKey, Boolean enabled, String model, String promptVersion, Integer maxDiffChars) {
        AiAnalysisConfig existing = configMapper.findByProvider(PROVIDER_DEEPSEEK);
        // 仅当传入新的完整 API Key 时更新（不含 **** 的脱敏值）
        String keyToSave = (apiKey != null && !apiKey.isBlank() && !apiKey.contains("****")) ? apiKey.trim() : (existing != null ? existing.getApiKey() : null);
        boolean en = enabled != null && enabled;
        String m = (model != null && !model.isBlank()) ? model : "deepseek-chat";
        String pv = (promptVersion != null && !promptVersion.isBlank()) ? promptVersion : "v1";
        int maxChars = (maxDiffChars != null && maxDiffChars > 0) ? maxDiffChars : 100000;
        if (existing != null) {
            configMapper.update(PROVIDER_DEEPSEEK, keyToSave, en, m, pv, maxChars);
        } else {
            AiAnalysisConfig newConfig = new AiAnalysisConfig();
            newConfig.setProvider(PROVIDER_DEEPSEEK);
            newConfig.setApiKey(keyToSave);
            newConfig.setEnabled(en);
            newConfig.setModel(m);
            newConfig.setPromptVersion(pv);
            newConfig.setMaxDiffChars(maxChars);
            configMapper.insert(newConfig);
        }
    }

    public void updateQwenConfig(String apiKey, Boolean enabled, String model) {
        AiAnalysisConfig existing = configMapper.findByProvider(PROVIDER_QWEN);
        String keyToSave = (apiKey != null && !apiKey.isBlank() && !apiKey.contains("****"))
                ? apiKey.trim()
                : (existing != null ? existing.getApiKey() : null);
        boolean en = enabled != null && enabled;
        String m = (model != null && !model.isBlank()) ? model : "qwen-vl-plus";
        int maxChars = existing != null && existing.getMaxDiffChars() != null ? existing.getMaxDiffChars() : 100000;
        String pv = existing != null && existing.getPromptVersion() != null && !existing.getPromptVersion().isBlank()
                ? existing.getPromptVersion()
                : "v1";
        if (existing != null) {
            configMapper.update(PROVIDER_QWEN, keyToSave, en, m, pv, maxChars);
        } else {
            AiAnalysisConfig c = new AiAnalysisConfig();
            c.setProvider(PROVIDER_QWEN);
            c.setApiKey(keyToSave);
            c.setEnabled(en);
            c.setModel(m);
            c.setPromptVersion(pv);
            c.setMaxDiffChars(maxChars);
            configMapper.insert(c);
        }
    }
}
