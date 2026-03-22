-- 报价项目：合同生成补充信息（付款/质保/验收/交付物/里程碑等 JSON）与 AI Prompt 增强
SET @db := DATABASE();
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_contract_context_json'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN quote_contract_context_json TEXT NULL COMMENT ''合同补充：付款计划、质保、验收、交付物、里程碑等(JSON)'' AFTER quote_calc_prefs_json',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
