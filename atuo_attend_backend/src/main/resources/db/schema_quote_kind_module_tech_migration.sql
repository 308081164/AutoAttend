-- 报价模式（单体 / 解决方案）与模块级技术栈（解决方案下按交付物计价）
SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_kind'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE biz_quote_project ADD COLUMN quote_kind VARCHAR(32) NOT NULL DEFAULT ''single'' COMMENT ''single=单体项目报价 solution=解决方案级报价'' AFTER quote_subject_mode',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE biz_quote_project SET quote_kind = 'single' WHERE quote_kind IS NULL OR TRIM(quote_kind) = '';

SET @exists2 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_module' AND COLUMN_NAME = 'tech_stack'
);
SET @sql2 := IF(@exists2 = 0,
  'ALTER TABLE biz_quote_module ADD COLUMN tech_stack VARCHAR(64) NULL COMMENT ''可选；为空则使用项目级 tech_stack 做人天基准'' AFTER deliverable_label',
  'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
