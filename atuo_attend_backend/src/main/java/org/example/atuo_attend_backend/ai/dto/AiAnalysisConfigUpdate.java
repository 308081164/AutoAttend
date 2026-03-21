package org.example.atuo_attend_backend.ai.dto;

/**
 * 管理员更新 AI 分析配置时的请求体。API Key 可选，仅当用户填写时更新。
 */
public class AiAnalysisConfigUpdate {
    private String apiKey;       // 可选，不传或空则不更新
    private Boolean enabled;
    /** 是否启用每日项目进展总结（与单次提交分析开关独立） */
    private Boolean dailySummaryEnabled;
    private String model;
    private String promptVersion;
    private Integer maxDiffChars;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getDailySummaryEnabled() { return dailySummaryEnabled; }
    public void setDailySummaryEnabled(Boolean dailySummaryEnabled) { this.dailySummaryEnabled = dailySummaryEnabled; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public Integer getMaxDiffChars() { return maxDiffChars; }
    public void setMaxDiffChars(Integer maxDiffChars) { this.maxDiffChars = maxDiffChars; }
}
