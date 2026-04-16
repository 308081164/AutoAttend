package org.example.atuo_attend_backend.quote.dto;

/**
 * 自然语言需求 → DeepSeek 解析为功能模块/功能点（与报价表单结构一致）。
 */
public class QuoteAiModulesParseRequest {

    /** 客户或内部的文字需求（必填） */
    private String requirementText;
    /** 以下字段来自当前报价表单，供模型结合上下文拆解（均可空） */
    private String projectType;
    private String techStack;
    private String designType;
    private String dataMigration;
    private String concurrency;
    private String securityLevel;
    private String deployType;
    private String prdSummary;
    /** 为 true 时要求 AI 按「多交付物」输出 deliverables[]（整套系统报价） */
    private Boolean multiDeliverableMode;

    public String getRequirementText() {
        return requirementText;
    }

    public void setRequirementText(String requirementText) {
        this.requirementText = requirementText;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getDesignType() {
        return designType;
    }

    public void setDesignType(String designType) {
        this.designType = designType;
    }

    public String getDataMigration() {
        return dataMigration;
    }

    public void setDataMigration(String dataMigration) {
        this.dataMigration = dataMigration;
    }

    public String getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(String concurrency) {
        this.concurrency = concurrency;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getDeployType() {
        return deployType;
    }

    public void setDeployType(String deployType) {
        this.deployType = deployType;
    }

    public String getPrdSummary() {
        return prdSummary;
    }

    public void setPrdSummary(String prdSummary) {
        this.prdSummary = prdSummary;
    }

    public Boolean getMultiDeliverableMode() {
        return multiDeliverableMode;
    }

    public void setMultiDeliverableMode(Boolean multiDeliverableMode) {
        this.multiDeliverableMode = multiDeliverableMode;
    }
}
