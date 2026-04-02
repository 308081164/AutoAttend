-- AI 智能录入原文持久化：用于智能报价回显编辑、以及快原型信息库补充
-- 幂等：列不存在则添加，已存在则跳过

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_quote_project'
      AND column_name = 'ai_requirement_text'
);

SET @sql := IF(@col_exists = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN ai_requirement_text MEDIUMTEXT NULL COMMENT ''AI 智能录入原文（用于智能报价回显编辑/快原型信息库）''',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

