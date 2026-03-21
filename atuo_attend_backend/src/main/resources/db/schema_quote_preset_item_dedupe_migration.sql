-- 修复历史问题：schema_quote_mysql.sql 曾对 biz_quote_preset_item 做无条件 INSERT，每次启动多 9 行。
-- 本脚本可重复执行：删除 (name + category + complexity) 完全相同的重复行，仅保留 id 最小的一条。
DELETE t1 FROM biz_quote_preset_item t1
INNER JOIN biz_quote_preset_item t2
  ON t1.name = t2.name
  AND IFNULL(t1.category, '') = IFNULL(t2.category, '')
  AND t1.complexity = t2.complexity
  AND t1.id > t2.id;
