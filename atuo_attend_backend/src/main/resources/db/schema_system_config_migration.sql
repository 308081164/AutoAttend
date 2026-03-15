-- 系统配置表（GitHub Token 等）：已有库升级时执行一次；新部署无需执行（schema_mysql.sql 已含）
CREATE TABLE IF NOT EXISTS aa_system_config (
    config_key VARCHAR(128) NOT NULL PRIMARY KEY,
    config_value TEXT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置 key-value';
