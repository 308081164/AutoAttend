package org.example.atuo_attend_backend.report.domain;

import java.time.LocalDateTime;

public class ReportConfig {
    private Long id;
    private Boolean enabled;
    private String scheduleType;
    private String scheduleParam;
    private String cronExpression;
    private String companyName;
    private String dailyExtraMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleParam() {
        return scheduleParam;
    }

    public void setScheduleParam(String scheduleParam) {
        this.scheduleParam = scheduleParam;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDailyExtraMessage() {
        return dailyExtraMessage;
    }

    public void setDailyExtraMessage(String dailyExtraMessage) {
        this.dailyExtraMessage = dailyExtraMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

