package org.example.atuo_attend_backend.quote.dto;

public class QuotePresetItemSaveDto {
    private String name;
    private String complexity;
    private String category;
    private Integer sortOrder;
    private Boolean enabled;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
