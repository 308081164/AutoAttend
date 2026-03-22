package org.example.atuo_attend_backend.quote.domain;

import java.time.LocalDateTime;

public class QuoteProject {
    private Long id;
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
    /** JSON：riskKeys、urgencyRush、priceConfigId、auditChecklist */
    private String quoteCalcPrefsJson;
    /** JSON：paymentPlan、质保、验收、交付物、里程碑、争议解决等，供合同 AI 与附件 */
    private String quoteContractContextJson;
    /** 报价单抬头：出具方、联系方式、有效期说明（写入报价 HTML/PDF） */
    private String quoteVendorName;
    private String quoteContactInfo;
    private String quoteValidityNote;
    /** 报价单出具方来源：legal_entity=配置法人模板，natural_person=配置自然人模板，manual=本项目手写 */
    private String quoteSubjectMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getQuoteCalcPrefsJson() { return quoteCalcPrefsJson; }
    public void setQuoteCalcPrefsJson(String quoteCalcPrefsJson) { this.quoteCalcPrefsJson = quoteCalcPrefsJson; }
    public String getQuoteContractContextJson() { return quoteContractContextJson; }
    public void setQuoteContractContextJson(String quoteContractContextJson) { this.quoteContractContextJson = quoteContractContextJson; }
    public String getQuoteVendorName() { return quoteVendorName; }
    public void setQuoteVendorName(String quoteVendorName) { this.quoteVendorName = quoteVendorName; }
    public String getQuoteContactInfo() { return quoteContactInfo; }
    public void setQuoteContactInfo(String quoteContactInfo) { this.quoteContactInfo = quoteContactInfo; }
    public String getQuoteValidityNote() { return quoteValidityNote; }
    public void setQuoteValidityNote(String quoteValidityNote) { this.quoteValidityNote = quoteValidityNote; }
    public String getQuoteSubjectMode() { return quoteSubjectMode; }
    public void setQuoteSubjectMode(String quoteSubjectMode) { this.quoteSubjectMode = quoteSubjectMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
