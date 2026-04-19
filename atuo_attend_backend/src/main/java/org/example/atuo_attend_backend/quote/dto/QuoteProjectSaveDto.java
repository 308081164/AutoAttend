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
    private String aiRequirementText;
    /** 可选：报价计算偏好，与 PATCH calc-prefs 结构一致 */
    private Map<String, Object> quoteCalcPrefs;
    /** 可选：合同补充（付款计划、质保、验收、交付物、里程碑等），见设计文档 §11.5 */
    private Map<String, Object> quoteContractContext;
    /** 报价单抬头：出具方名称 */
    private String quoteVendorName;
    /** 报价单抬头：电话/邮箱/微信等 */
    private String quoteContactInfo;
    /** 报价单抬头：有效期说明，如截止日期或「自出具日起30个自然日」 */
    private String quoteValidityNote;
    /** legal_entity | natural_person | manual */
    private String quoteSubjectMode;
    /** single | solution；默认 single */
    private String quoteKind;
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
    public String getAiRequirementText() { return aiRequirementText; }
    public void setAiRequirementText(String aiRequirementText) { this.aiRequirementText = aiRequirementText; }
    public Map<String, Object> getQuoteCalcPrefs() { return quoteCalcPrefs; }
    public void setQuoteCalcPrefs(Map<String, Object> quoteCalcPrefs) { this.quoteCalcPrefs = quoteCalcPrefs; }
    public Map<String, Object> getQuoteContractContext() { return quoteContractContext; }
    public void setQuoteContractContext(Map<String, Object> quoteContractContext) { this.quoteContractContext = quoteContractContext; }
    public String getQuoteVendorName() { return quoteVendorName; }
    public void setQuoteVendorName(String quoteVendorName) { this.quoteVendorName = quoteVendorName; }
    public String getQuoteContactInfo() { return quoteContactInfo; }
    public void setQuoteContactInfo(String quoteContactInfo) { this.quoteContactInfo = quoteContactInfo; }
    public String getQuoteValidityNote() { return quoteValidityNote; }
    public void setQuoteValidityNote(String quoteValidityNote) { this.quoteValidityNote = quoteValidityNote; }
    public String getQuoteSubjectMode() { return quoteSubjectMode; }
    public void setQuoteSubjectMode(String quoteSubjectMode) { this.quoteSubjectMode = quoteSubjectMode; }
    public String getQuoteKind() { return quoteKind; }
    public void setQuoteKind(String quoteKind) { this.quoteKind = quoteKind; }
    public List<QuoteModuleSaveDto> getModules() { return modules; }
    public void setModules(List<QuoteModuleSaveDto> modules) { this.modules = modules != null ? modules : new ArrayList<>(); }
}
