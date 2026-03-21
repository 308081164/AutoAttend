package org.example.atuo_attend_backend.quote.dto;

import java.util.ArrayList;
import java.util.List;

public class QuoteRiskConfigBatchUpdate {
    private List<QuoteRiskConfigUpdateItem> items = new ArrayList<>();

    public List<QuoteRiskConfigUpdateItem> getItems() { return items; }
    public void setItems(List<QuoteRiskConfigUpdateItem> items) { this.items = items != null ? items : new ArrayList<>(); }
}
