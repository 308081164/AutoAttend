package org.example.atuo_attend_backend.quote.dto;

public class QuoteItemSaveDto {
    private String name;
    private String complexity;
    private int quantity = 1;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
