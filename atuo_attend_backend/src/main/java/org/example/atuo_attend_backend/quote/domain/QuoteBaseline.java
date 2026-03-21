package org.example.atuo_attend_backend.quote.domain;

import java.math.BigDecimal;

public class QuoteBaseline {
    private Long id;
    private String techStack;
    private String complexity;
    private BigDecimal days;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }
}
