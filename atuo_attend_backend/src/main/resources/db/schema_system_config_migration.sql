-- 系统配置表（GitHub Token 等）：已有库若仅有旧结构，由 SystemConfigSchemaBootstrap 自动补 tenant_id 与复合主键
CREATE TABLE IF NOT EXISTS aa_system_config (
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置 key-value';
