package org.example.atuo_attend_backend.quote.dto;

import java.util.List;
import java.util.Map;

/**
 * 根据当前报价项目的功能清单与上下文，由 AI 生成「验收测试用例/测试清单」JSON。
 * <p>
 * {@code modules} 结构与报价保存一致：{@code [{ "name", "items": [{ "name", "complexity", "quantity" }] }]}。
 */
public class QuoteAiAcceptanceTestCasesRequest {

    private String projectType;
    private String techStack;
    private String designType;
    private String dataMigration;
    private String concurrency;
    private String securityLevel;
    private String deployType;
    private String prdSummary;
    /** 合同补充中的「验收标准补充说明」，勿与用例矛盾 */
    private String acceptanceCriteriaNote;
    /** 功能模块+功能点（与前端 modules 一致） */
    private List<Map<String, Object>> modules;

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

    public String getAcceptanceCriteriaNote() {
        return acceptanceCriteriaNote;
    }

    public void setAcceptanceCriteriaNote(String acceptanceCriteriaNote) {
        this.acceptanceCriteriaNote = acceptanceCriteriaNote;
    }

    public List<Map<String, Object>> getModules() {
        return modules;
    }

    public void setModules(List<Map<String, Object>> modules) {
        this.modules = modules;
    }
}
