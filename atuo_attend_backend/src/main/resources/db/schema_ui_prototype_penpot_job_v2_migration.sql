-- Penpot 任务：进度、阶段、重试计数、导出链接
ALTER TABLE aa_ui_prototype_penpot_job
    ADD COLUMN stage VARCHAR(64) NULL COMMENT 'planning|creating|writing|exporting|done' AFTER status,
    ADD COLUMN progress TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0-100' AFTER stage,
    ADD COLUMN retry_count TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER progress,
    ADD COLUMN export_binfile_url VARCHAR(2048) NULL COMMENT 'export-binfile 返回的临时下载 URL' AFTER penpot_preview_url;

ALTER TABLE aa_ui_prototype_project
    ADD COLUMN penpot_export_url VARCHAR(2048) NULL COMMENT '最近一次 .penpot 导出链接' AFTER penpot_preview_url;
