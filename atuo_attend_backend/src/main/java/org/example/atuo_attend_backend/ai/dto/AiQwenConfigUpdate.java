package org.example.atuo_attend_backend.ai.dto;

/**
 * 管理员更新通义·千问多模态配置时的请求体。
 */
public class AiQwenConfigUpdate {

    private String apiKey;   // 可选，仅当用户填写时更新
    private Boolean enabled;
    private String model;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

