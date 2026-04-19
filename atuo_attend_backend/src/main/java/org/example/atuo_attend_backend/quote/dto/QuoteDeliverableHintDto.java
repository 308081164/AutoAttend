package org.example.atuo_attend_backend.quote.dto;

/**
 * 多交付物 AI 填功能时，约束交付物键/名称及模块级技术栈。
 */
public class QuoteDeliverableHintDto {
    private String deliverableKey;
    private String deliverableLabel;
    /** 与项目 tech_stack 同枚举；可为空表示用项目级 */
    private String techStack;

    public String getDeliverableKey() {
        return deliverableKey;
    }

    public void setDeliverableKey(String deliverableKey) {
        this.deliverableKey = deliverableKey;
    }

    public String getDeliverableLabel() {
        return deliverableLabel;
    }

    public void setDeliverableLabel(String deliverableLabel) {
        this.deliverableLabel = deliverableLabel;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }
}
