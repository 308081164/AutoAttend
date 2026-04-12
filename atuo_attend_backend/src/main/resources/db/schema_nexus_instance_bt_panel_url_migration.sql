-- 旧库若早于 bt_panel_url 合入主 schema，实例表缺少该列会导致 SELECT 报 Unknown column。
-- 幂等：已存在列时会报错，entrypoint 使用 mysql --force 会跳过并继续。
ALTER TABLE aa_nexus_cloud_instance
  ADD COLUMN bt_panel_url VARCHAR(512) NULL COMMENT '宝塔面板 URL，同步时不覆盖' AFTER memory_mb;
