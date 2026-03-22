-- 人天单价档位增加「工期系数」；报价结果增加「预计工期」与采用的系数（可重复执行）

-- biz_quote_price_config.duration_coefficient
SET @db := DATABASE();
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_price_config' AND COLUMN_NAME = 'duration_coefficient'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE biz_quote_price_config ADD COLUMN duration_coefficient DECIMAL(10,4) NOT NULL DEFAULT 1.2000 COMMENT ''工期系数：预计工期(天)=总人天×系数'' AFTER price_per_day',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- biz_quote_result.estimated_duration_days
SET @exists2 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'estimated_duration_days'
);
SET @sql2 := IF(@exists2 = 0,
    'ALTER TABLE biz_quote_result ADD COLUMN estimated_duration_days DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''预计工期(天)=总人天×工期系数'' AFTER total_days',
    'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- biz_quote_result.duration_coefficient_used
SET @exists3 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_result' AND COLUMN_NAME = 'duration_coefficient_used'
);
SET @sql3 := IF(@exists3 = 0,
    'ALTER TABLE biz_quote_result ADD COLUMN duration_coefficient_used DECIMAL(10,4) NOT NULL DEFAULT 1.2000 COMMENT ''计算时采用的工期系数'' AFTER price_per_day_used',
    'SELECT 1');
PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- 历史行：按总人天与已落库的系数回填预计工期（系数列新增时 MySQL 会为旧行填默认 1.2）
UPDATE biz_quote_result
SET estimated_duration_days = ROUND(total_days * duration_coefficient_used, 2)
WHERE estimated_duration_days = 0
  AND total_days IS NOT NULL
  AND total_days > 0;
