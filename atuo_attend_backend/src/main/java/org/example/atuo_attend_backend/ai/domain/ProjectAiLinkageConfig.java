package org.example.atuo_attend_backend.ai.domain;

import java.time.LocalDateTime;

/**
 * 项目级 Commit AI 与多维表联动配置。
 */
public class ProjectAiLinkageConfig {

    /** 仅执行提交分析（不注入多维候选、不写表） */
    public static final String AUTOMATION_ANALYSIS_ONLY = "analysis_only";

    /** 分析 + 自动更新任务状态列（issue_tracking:「当前状态」；feature_backlog:「开发进度」） */
    public static final String AUTOMATION_AUTO_STATUS = "auto_status";

    /** 关闭本项目 Commit AI 分析（不调用大模型） */
    public static final String AUTOMATION_DISABLED = "disabled";

    private Long id;
    private Long projectId;
    private String automationMode;
    private String minConfidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getAutomationMode() { return automationMode; }
    public void setAutomationMode(String automationMode) { this.automationMode = automationMode; }
    public String getMinConfidence() { return minConfidence; }
    public void setMinConfidence(String minConfidence) { this.minConfidence = minConfidence; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
