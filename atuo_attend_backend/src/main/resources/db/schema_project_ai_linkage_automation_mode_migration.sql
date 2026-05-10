-- 项目级 Commit AI：三档自动化模式（替换原 enabled + mode）
-- analysis_only：仅提交分析，不注入多维候选、不写表
-- auto_status：分析 + 注入候选，且仅自动更新「当前状态」或「开发进度」列
-- disabled：本项目关闭 Commit AI 分析（不调用大模型、不落分析结果）

ALTER TABLE aa_project_ai_linkage_config
    ADD COLUMN automation_mode VARCHAR(32) NOT NULL DEFAULT 'analysis_only'
    COMMENT 'analysis_only|auto_status|disabled'
    AFTER project_id;

UPDATE aa_project_ai_linkage_config
SET automation_mode = CASE
    WHEN enabled = 0 THEN 'disabled'
    WHEN enabled = 1 AND LOWER(TRIM(mode)) = 'auto' THEN 'auto_status'
    ELSE 'analysis_only'
END;

ALTER TABLE aa_project_ai_linkage_config
    DROP INDEX idx_enabled,
    DROP COLUMN enabled,
    DROP COLUMN mode;

ALTER TABLE aa_project_ai_linkage_config
    ADD KEY idx_automation_mode (automation_mode);
