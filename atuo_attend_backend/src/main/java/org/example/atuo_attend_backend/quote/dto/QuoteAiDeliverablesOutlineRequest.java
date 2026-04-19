package org.example.atuo_attend_backend.quote.dto;

/**
 * 解决方案向导：仅拆分交付物骨架（键、名称、建议技术栈、范围摘要），不含功能点。
 */
public class QuoteAiDeliverablesOutlineRequest {

    private String requirementText;
    private String projectType;
    /** 项目默认技术栈，供未指定的交付物参考 */
    private String techStack;
    private String prdSummary;

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

    public String getPrdSummary() {
        return prdSummary;
    }

    public void setPrdSummary(String prdSummary) {
        this.prdSummary = prdSummary;
    }
}
