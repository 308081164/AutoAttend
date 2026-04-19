package org.example.atuo_attend_backend.quote.dto;

import java.util.ArrayList;
import java.util.List;

public class QuoteModuleSaveDto {
    private String name;
    private int sortOrder;
    /** 交付物分组键（同键模块归为同一交付物，如 web、app、api） */
    private String deliverableKey;
    /** 交付物显示名称 */
    private String deliverableLabel;
    /** 可选；解决方案下按交付物人天基准；空则继承项目 tech_stack */
    private String techStack;
    private List<QuoteItemSaveDto> items = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getDeliverableKey() { return deliverableKey; }
    public void setDeliverableKey(String deliverableKey) { this.deliverableKey = deliverableKey; }
    public String getDeliverableLabel() { return deliverableLabel; }
    public void setDeliverableLabel(String deliverableLabel) { this.deliverableLabel = deliverableLabel; }
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
    public List<QuoteItemSaveDto> getItems() { return items; }
    public void setItems(List<QuoteItemSaveDto> items) { this.items = items != null ? items : new ArrayList<>(); }
}
