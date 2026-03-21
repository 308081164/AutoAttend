package org.example.atuo_attend_backend.quote.dto;

import java.math.BigDecimal;

public class QuoteBaselineSaveDto {
    private String techStack;
    private String complexity;
    private BigDecimal days;

    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }
}
