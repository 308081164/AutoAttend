package org.example.atuo_attend_backend.quote.dto;

import java.util.ArrayList;
import java.util.List;

public class QuoteModuleSaveDto {
    private String name;
    private int sortOrder;
    private List<QuoteItemSaveDto> items = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<QuoteItemSaveDto> getItems() { return items; }
    public void setItems(List<QuoteItemSaveDto> items) { this.items = items != null ? items : new ArrayList<>(); }
}
