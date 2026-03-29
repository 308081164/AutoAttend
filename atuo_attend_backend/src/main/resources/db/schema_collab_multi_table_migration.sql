-- 协作多维表：由「一项目一表」扩展为「一项目多表」，用 purpose 区分
-- docker-entrypoint 使用 mysql --force：无旧索引 / 列已存在 等错误可忽略并继续。

-- 1) 去掉 project_id 唯一约束（仅旧库有 uk_project）；新库请忽略本句报错
ALTER TABLE biz_project_table DROP INDEX uk_project;

-- 2) purpose：issue_tracking=项目调整；feature_backlog=待开发功能清单（列已存在时忽略报错）
ALTER TABLE biz_project_table
  ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'issue_tracking' COMMENT '表用途' AFTER name;

UPDATE biz_project_table SET purpose = 'issue_tracking' WHERE purpose IS NULL OR purpose = '';

-- 3) 唯一约束（若已存在 uk_project_purpose 则跳过）
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_project_table'
    AND index_name = 'uk_project_purpose'
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE biz_project_table ADD UNIQUE KEY uk_project_purpose (project_id, purpose)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
