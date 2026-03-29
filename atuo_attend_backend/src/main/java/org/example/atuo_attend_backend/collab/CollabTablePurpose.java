package org.example.atuo_attend_backend.collab;

/**
 * 项目下多维表用途：与 {@code biz_project_table.purpose} 对应。
 */
public final class CollabTablePurpose {

    /** 项目调整 / 问题与需求变更（原默认「任务表」，偏售后） */
    public static final String ISSUE_TRACKING = "issue_tracking";

    /** 待开发功能清单（偏开发阶段进度） */
    public static final String FEATURE_BACKLOG = "feature_backlog";

    private CollabTablePurpose() {
    }
}
