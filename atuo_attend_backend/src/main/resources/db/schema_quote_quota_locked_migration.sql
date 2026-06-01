-- 获客链接超量创建的报价项目：对租户侧隐藏直至升级后用量回到档位内
SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quota_locked'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE biz_quote_project ADD COLUMN quota_locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=超量获客创建，租户未升级前不可查看'' AFTER quote_kind',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
