-- 报价项目：持久化「报价计算」勾选（风险、加急、单价档）与审核清单（可重复执行）
SET @db := DATABASE();
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_calc_prefs_json'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN quote_calc_prefs_json TEXT NULL COMMENT ''报价计算与审核清单偏好(JSON)'' AFTER prd_summary',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
