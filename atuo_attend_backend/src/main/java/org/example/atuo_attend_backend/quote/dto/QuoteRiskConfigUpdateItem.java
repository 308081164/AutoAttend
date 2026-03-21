package org.example.atuo_attend_backend.quote.dto;

import java.math.BigDecimal;

public class QuoteRiskConfigUpdateItem {
    private Long id;
    private String label;
    private BigDecimal defaultPct;
    private Boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getDefaultPct() { return defaultPct; }
    public void setDefaultPct(BigDecimal defaultPct) { this.defaultPct = defaultPct; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
