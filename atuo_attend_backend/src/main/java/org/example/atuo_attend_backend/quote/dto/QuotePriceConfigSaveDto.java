package org.example.atuo_attend_backend.quote.dto;

import java.math.BigDecimal;

public class QuotePriceConfigSaveDto {
    private String regionLabel;
    private BigDecimal pricePerDay;
    private String currency;
    private Boolean enabled;

    public String getRegionLabel() { return regionLabel; }
    public void setRegionLabel(String regionLabel) { this.regionLabel = regionLabel; }
    public BigDecimal getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
