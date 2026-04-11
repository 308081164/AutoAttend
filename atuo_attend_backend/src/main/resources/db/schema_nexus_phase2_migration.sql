-- Nexus 阶段二（仅阿里云）：告警规则表
-- 说明：bt_panel_url 已合入 schema_nexus_migration.sql（新库自带）。
-- 若你的库是更早版本创建的且无 bt_panel_url，请手动执行：
-- ALTER TABLE aa_nexus_cloud_instance ADD COLUMN bt_panel_url VARCHAR(512) NULL COMMENT '宝塔面板 URL' AFTER memory_mb;

CREATE TABLE IF NOT EXISTS aa_nexus_alert_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    account_id BIGINT NULL COMMENT 'NULL=全部云账号接入',
    instance_id VARCHAR(128) NULL COMMENT 'NULL=账号下全部实例（account_id 建议非空）',
    name VARCHAR(128) NOT NULL DEFAULT '',
    metric_type VARCHAR(16) NOT NULL COMMENT 'cpu | memory',
    op VARCHAR(8) NOT NULL COMMENT 'gt | gte | lt | lte',
    threshold DOUBLE NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 5 COMMENT '窗口内采样点需全部满足条件',
    webhook_url VARCHAR(768) NULL COMMENT '告警触发 POST JSON',
    notify_email VARCHAR(256) NULL COMMENT '可选，走系统 SMTP',
    silence_seconds INT NOT NULL DEFAULT 900 COMMENT '同一规则静默期（秒）',
    enabled TINYINT NOT NULL DEFAULT 1,
    last_triggered_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_enabled (tenant_id, enabled),
    KEY idx_tenant_account (tenant_id, account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 告警规则';
