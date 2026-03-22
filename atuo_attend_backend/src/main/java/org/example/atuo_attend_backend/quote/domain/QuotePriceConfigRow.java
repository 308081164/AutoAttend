package org.example.atuo_attend_backend.quote.domain;

import java.math.BigDecimal;

public class QuotePriceConfigRow {
    private Long id;
    private String regionLabel;
    private BigDecimal pricePerDay;
    private BigDecimal durationCoefficient;
    private String currency;
    private Boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegionLabel() { return regionLabel; }
    public void setRegionLabel(String regionLabel) { this.regionLabel = regionLabel; }
    public BigDecimal getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }
    public BigDecimal getDurationCoefficient() { return durationCoefficient; }
    public void setDurationCoefficient(BigDecimal durationCoefficient) { this.durationCoefficient = durationCoefficient; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
