package org.example.atuo_attend_backend.quote.domain;

public class QuoteModule {
    private Long id;
    private Long tenantId;
    private Long quoteProjectId;
    private String name;
    private int sortOrder;
    /** 同一报价项目下多交付物分组（如 web、app、api） */
    private String deliverableKey;
    /** 交付物展示名 */
    private String deliverableLabel;
    /** 可选；为空则使用项目级 tech_stack 做人天基准（解决方案多交付物） */
    private String techStack;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getQuoteProjectId() { return quoteProjectId; }
    public void setQuoteProjectId(Long quoteProjectId) { this.quoteProjectId = quoteProjectId; }
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
}
