-- 修复旧库 aa_ai_analysis_result.main_area 字段过短导致写入失败
-- 新部署使用 schema_ai_mysql.sql（main_area 为 VARCHAR(255)）无需执行本文件。
-- 若已是足够长度，重复执行等效无副作用。

ALTER TABLE aa_ai_analysis_result MODIFY COLUMN main_area VARCHAR(255) NULL COMMENT '主要涉及模块/路径';

