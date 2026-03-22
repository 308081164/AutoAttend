-- 报价项目：报价单抬头（报价单位、联系方式、报价有效期），供 HTML/PDF 展示
SET @db := DATABASE();

SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_vendor_name'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN quote_vendor_name VARCHAR(255) NULL COMMENT ''报价单位（出具方名称）'' AFTER quote_contract_context_json',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists2 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_contact_info'
);
SET @sql2 := IF(@exists2 = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN quote_contact_info VARCHAR(512) NULL COMMENT ''报价联系方式'' AFTER quote_vendor_name',
    'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @exists3 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_validity_note'
);
SET @sql3 := IF(@exists3 = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN quote_validity_note VARCHAR(512) NULL COMMENT ''报价有效期说明'' AFTER quote_contact_info',
    'SELECT 1');
PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
