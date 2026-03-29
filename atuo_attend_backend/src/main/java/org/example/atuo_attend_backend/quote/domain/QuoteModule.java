package org.example.atuo_attend_backend.quote.domain;

public class QuoteModule {
    private Long id;
    private Long tenantId;
    private Long quoteProjectId;
    private String name;
    private int sortOrder;

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
}
