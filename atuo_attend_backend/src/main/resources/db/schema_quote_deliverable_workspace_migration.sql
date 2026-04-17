-- 报价：多交付物（解决方案）、行级价格快照与等比例商务调价、租户工作台偏好
-- 与 duration_coefficient 等迁移一致：用 IF(@exists=0) 动态 ALTER，可重复执行；由 docker-entrypoint.sh 按 migrate_manifest.txt 顺序执行。

SET @db := DATABASE();

-- biz_quote_module：交付物分组
SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_module' AND COLUMN_NAME = 'deliverable_key'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE biz_quote_module ADD COLUMN deliverable_key VARCHAR(64) NOT NULL DEFAULT ''default'' COMMENT ''交付物分组键，同键模块归属同一交付物'' AFTER sort_order',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists2 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_module' AND COLUMN_NAME = 'deliverable_label'
);
SET @sql2 := IF(@exists2 = 0,
  'ALTER TABLE biz_quote_module ADD COLUMN deliverable_label VARCHAR(255) NULL COMMENT ''交付物显示名称'' AFTER deliverable_key',
  'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

UPDATE biz_quote_module SET deliverable_key = 'default' WHERE deliverable_key IS NULL OR TRIM(deliverable_key) = '';

-- biz_quote_item
SET @exists3 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_item' AND COLUMN_NAME = 'excluded_from_scale'
);
SET @sql3 := IF(@exists3 = 0,
  'ALTER TABLE biz_quote_item ADD COLUMN excluded_from_scale TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=不参与等比例缩放（如第三方固定成本）'' AFTER sort_order',
  'SELECT 1');
PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SET @exists4 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_item' AND COLUMN_NAME = 'line_price_snap'
);
SET @sql4 := IF(@exists4 = 0,
  'ALTER TABLE biz_quote_item ADD COLUMN line_price_snap DECIMAL(14,2) NULL COMMENT ''模型总价分摊到本行的基线金额（含税风险后）'' AFTER excluded_from_scale',
  'SELECT 1');
PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

SET @exists5 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_item' AND COLUMN_NAME = 'line_price_adjusted'
);
SET @sql5 := IF(@exists5 = 0,
  'ALTER TABLE biz_quote_item ADD COLUMN line_price_adjusted DECIMAL(14,2) NULL COMMENT ''商务调价后金额；空则等于 line_price_snap'' AFTER line_price_snap',
  'SELECT 1');
PREPARE stmt5 FROM @sql5;
EXECUTE stmt5;
DEALLOCATE PREPARE stmt5;

-- biz_quote_result
SET @exists6 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'baseline_final_amount'
);
SET @sql6 := IF(@exists6 = 0,
  'ALTER TABLE biz_quote_result ADD COLUMN baseline_final_amount DECIMAL(14,2) NULL COMMENT ''模型输出的最终报价（调价前基线）'' AFTER final_amount',
  'SELECT 1');
PREPARE stmt6 FROM @sql6;
EXECUTE stmt6;
DEALLOCATE PREPARE stmt6;

SET @exists7 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'price_scale_factor'
);
SET @sql7 := IF(@exists7 = 0,
  'ALTER TABLE biz_quote_result ADD COLUMN price_scale_factor DECIMAL(14,6) NOT NULL DEFAULT 1.000000 COMMENT ''作用于可缩放行的系数'' AFTER baseline_final_amount',
  'SELECT 1');
PREPARE stmt7 FROM @sql7;
EXECUTE stmt7;
DEALLOCATE PREPARE stmt7;

SET @exists8 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'adjusted_final_amount'
);
SET @sql8 := IF(@exists8 = 0,
  'ALTER TABLE biz_quote_result ADD COLUMN adjusted_final_amount DECIMAL(14,2) NULL COMMENT ''商务调价后总价'' AFTER price_scale_factor',
  'SELECT 1');
PREPARE stmt8 FROM @sql8;
EXECUTE stmt8;
DEALLOCATE PREPARE stmt8;

SET @exists9 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'price_adjust_note'
);
SET @sql9 := IF(@exists9 = 0,
  'ALTER TABLE biz_quote_result ADD COLUMN price_adjust_note VARCHAR(512) NULL COMMENT ''调价说明（如客户预算区间）'' AFTER adjusted_final_amount',
  'SELECT 1');
PREPARE stmt9 FROM @sql9;
EXECUTE stmt9;
DEALLOCATE PREPARE stmt9;

UPDATE biz_quote_result
SET baseline_final_amount = final_amount,
    price_scale_factor = 1,
    adjusted_final_amount = final_amount
WHERE baseline_final_amount IS NULL AND final_amount IS NOT NULL;

-- aa_tenant：工作台偏好
SET @exists10 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'aa_tenant' AND COLUMN_NAME = 'workspace_prefs_json'
);
SET @sql10 := IF(@exists10 = 0,
  'ALTER TABLE aa_tenant ADD COLUMN workspace_prefs_json TEXT NULL COMMENT ''工作台偏好 JSON：quoteNavVisible 等'' AFTER status',
  'SELECT 1');
PREPARE stmt10 FROM @sql10;
EXECUTE stmt10;
DEALLOCATE PREPARE stmt10;
