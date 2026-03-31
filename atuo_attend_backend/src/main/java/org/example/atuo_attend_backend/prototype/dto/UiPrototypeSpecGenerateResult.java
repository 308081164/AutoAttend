package org.example.atuo_attend_backend.prototype.dto;

public class UiPrototypeSpecGenerateResult {
    private Integer specVersion;
    private String specJson;
    private String rawAiContent;

    public Integer getSpecVersion() { return specVersion; }
    public void setSpecVersion(Integer specVersion) { this.specVersion = specVersion; }

    public String getSpecJson() { return specJson; }
    public void setSpecJson(String specJson) { this.specJson = specJson; }

    public String getRawAiContent() { return rawAiContent; }
    public void setRawAiContent(String rawAiContent) { this.rawAiContent = rawAiContent; }
}

