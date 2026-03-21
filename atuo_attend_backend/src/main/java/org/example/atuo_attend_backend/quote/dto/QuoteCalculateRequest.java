package org.example.atuo_attend_backend.quote.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuoteCalculateRequest {
    private List<String> riskKeys = new ArrayList<>();
    private boolean urgencyRush;
    private Long priceConfigId;
    private Map<String, Boolean> auditChecklist = new HashMap<>();

    public List<String> getRiskKeys() { return riskKeys; }
    public void setRiskKeys(List<String> riskKeys) { this.riskKeys = riskKeys != null ? riskKeys : new ArrayList<>(); }
    public boolean isUrgencyRush() { return urgencyRush; }
    public void setUrgencyRush(boolean urgencyRush) { this.urgencyRush = urgencyRush; }
    public Long getPriceConfigId() { return priceConfigId; }
    public void setPriceConfigId(Long priceConfigId) { this.priceConfigId = priceConfigId; }
    public Map<String, Boolean> getAuditChecklist() { return auditChecklist; }
    public void setAuditChecklist(Map<String, Boolean> auditChecklist) { this.auditChecklist = auditChecklist != null ? auditChecklist : new HashMap<>(); }
}
