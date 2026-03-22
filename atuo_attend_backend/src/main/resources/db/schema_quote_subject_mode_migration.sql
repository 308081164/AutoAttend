-- 报价项目：报价单主体来源 legal_entity | natural_person | manual（与报价配置乙方模板联动）
SET @db := DATABASE();
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_subject_mode'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN quote_subject_mode VARCHAR(32) NOT NULL DEFAULT ''legal_entity'' COMMENT ''报价单主体：legal_entity|natural_person|manual'' AFTER quote_validity_note',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
