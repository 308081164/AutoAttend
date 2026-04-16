-- 报价：多交付物（解决方案）、行级价格快照与等比例商务调价、租户工作台偏好
-- 幂等：列已存在则跳过（由迁移执行器逐条处理）

-- biz_quote_module：交付物分组（同一报价项目下多 Web/APP/后端等）
SET @db := DATABASE();
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_module' AND COLUMN_NAME = 'deliverable_key') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_module ADD COLUMN deliverable_key VARCHAR(64) NOT NULL DEFAULT ''default'' COMMENT ''交付物分组键，同键模块归属同一交付物'' AFTER sort_order'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_module' AND COLUMN_NAME = 'deliverable_label') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_module ADD COLUMN deliverable_label VARCHAR(255) NULL COMMENT ''交付物显示名称'' AFTER deliverable_key'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE biz_quote_module SET deliverable_key = 'default' WHERE deliverable_key IS NULL OR deliverable_key = '';

-- biz_quote_item：不参与商务缩放的行、模型基线行金额快照、调价后金额
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_item' AND COLUMN_NAME = 'excluded_from_scale') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_item ADD COLUMN excluded_from_scale TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=不参与等比例缩放（如第三方固定成本）'' AFTER sort_order'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_item' AND COLUMN_NAME = 'line_price_snap') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_item ADD COLUMN line_price_snap DECIMAL(14,2) NULL COMMENT ''模型总价分摊到本行的基线金额（含税风险后）'' AFTER excluded_from_scale'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_item' AND COLUMN_NAME = 'line_price_adjusted') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_item ADD COLUMN line_price_adjusted DECIMAL(14,2) NULL COMMENT ''商务调价后金额；空则等于 line_price_snap'' AFTER line_price_snap'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- biz_quote_result：基线总价与调价系数
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'baseline_final_amount') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_result ADD COLUMN baseline_final_amount DECIMAL(14,2) NULL COMMENT ''模型输出的最终报价（调价前基线）'' AFTER final_amount'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'price_scale_factor') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_result ADD COLUMN price_scale_factor DECIMAL(14,6) NOT NULL DEFAULT 1.000000 COMMENT ''作用于可缩放行的系数'' AFTER baseline_final_amount'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'adjusted_final_amount') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_result ADD COLUMN adjusted_final_amount DECIMAL(14,2) NULL COMMENT ''商务调价后总价'' AFTER price_scale_factor'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'price_adjust_note') > 0,
    'SELECT 1',
    'ALTER TABLE biz_quote_result ADD COLUMN price_adjust_note VARCHAR(512) NULL COMMENT ''调价说明（如客户预算区间）'' AFTER adjusted_final_amount'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE biz_quote_result SET baseline_final_amount = final_amount, price_scale_factor = 1, adjusted_final_amount = final_amount
WHERE baseline_final_amount IS NULL AND final_amount IS NOT NULL;

-- aa_tenant：工作台 UI 偏好（JSON）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'aa_tenant' AND COLUMN_NAME = 'workspace_prefs_json') > 0,
    'SELECT 1',
    'ALTER TABLE aa_tenant ADD COLUMN workspace_prefs_json TEXT NULL COMMENT ''工作台偏好 JSON：quoteNavVisible 等'' AFTER status'
  )
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
