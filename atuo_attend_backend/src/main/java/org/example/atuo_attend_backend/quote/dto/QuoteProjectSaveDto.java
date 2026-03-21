package org.example.atuo_attend_backend.quote.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuoteProjectSaveDto {
    private String name;
    private String projectType;
    private String techStack;
    private String designType;
    private String dataMigration;
    private String concurrency;
    private String securityLevel;
    private String deployType;
    private String status;
    private Long linkTableId;
    private String prdSummary;
    /** 可选：报价计算偏好，与 PATCH calc-prefs 结构一致 */
    private Map<String, Object> quoteCalcPrefs;
    private List<QuoteModuleSaveDto> modules = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
    public String getDesignType() { return designType; }
    public void setDesignType(String designType) { this.designType = designType; }
    public String getDataMigration() { return dataMigration; }
    public void setDataMigration(String dataMigration) { this.dataMigration = dataMigration; }
    public String getConcurrency() { return concurrency; }
    public void setConcurrency(String concurrency) { this.concurrency = concurrency; }
    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public String getDeployType() { return deployType; }
    public void setDeployType(String deployType) { this.deployType = deployType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLinkTableId() { return linkTableId; }
    public void setLinkTableId(Long linkTableId) { this.linkTableId = linkTableId; }
    public String getPrdSummary() { return prdSummary; }
    public void setPrdSummary(String prdSummary) { this.prdSummary = prdSummary; }
    public Map<String, Object> getQuoteCalcPrefs() { return quoteCalcPrefs; }
    public void setQuoteCalcPrefs(Map<String, Object> quoteCalcPrefs) { this.quoteCalcPrefs = quoteCalcPrefs; }
    public List<QuoteModuleSaveDto> getModules() { return modules; }
    public void setModules(List<QuoteModuleSaveDto> modules) { this.modules = modules != null ? modules : new ArrayList<>(); }
}
