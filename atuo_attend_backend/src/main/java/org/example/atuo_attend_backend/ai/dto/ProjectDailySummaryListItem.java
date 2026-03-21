package org.example.atuo_attend_backend.ai.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 列表接口不含正文，避免 MEDIUMTEXT 过大 */
public class ProjectDailySummaryListItem {
    private Long id;
    private String repoFullName;
    private LocalDate summaryDate;
    private String title;
    private int commitCount;
    private String model;
    private String status;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getCommitCount() { return commitCount; }
    public void setCommitCount(int commitCount) { this.commitCount = commitCount; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
