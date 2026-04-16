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
    private List<QuoteItemSaveDto> items = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getDeliverableKey() { return deliverableKey; }
    public void setDeliverableKey(String deliverableKey) { this.deliverableKey = deliverableKey; }
    public String getDeliverableLabel() { return deliverableLabel; }
    public void setDeliverableLabel(String deliverableLabel) { this.deliverableLabel = deliverableLabel; }
    public List<QuoteItemSaveDto> getItems() { return items; }
    public void setItems(List<QuoteItemSaveDto> items) { this.items = items != null ? items : new ArrayList<>(); }
}
