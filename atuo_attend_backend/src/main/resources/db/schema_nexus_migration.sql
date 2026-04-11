-- Nexus 快捷运维（Nexus）数据库结构
-- 约定：
-- 1) 所有表均带 tenant_id，用于多租户隔离
-- 2) 凭证采用加密落库（明文不可写入）

CREATE TABLE IF NOT EXISTS aa_nexus_cloud_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    provider VARCHAR(32) NOT NULL COMMENT '云厂商：aliyun 等',
    display_name VARCHAR(128) NOT NULL COMMENT '展示名称',
    region_id VARCHAR(32) NOT NULL COMMENT '默认地域（MVP：单地域）',
    access_key_id_enc TEXT NOT NULL COMMENT 'AccessKeyId（加密）',
    access_key_secret_enc TEXT NOT NULL COMMENT 'AccessKeySecret（加密）',
    auto_sync_interval_seconds INT NULL COMMENT '自动巡检间隔（秒，覆盖全局）；NULL 则使用全局',
    last_auto_sync_at DATETIME NULL COMMENT '最近一次自动同步时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_provider (tenant_id, provider),
    KEY idx_tenant_region (tenant_id, region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 云账号接入（多云多账号）';

CREATE TABLE IF NOT EXISTS aa_nexus_cloud_instance (
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id BIGINT NOT NULL COMMENT '云账号接入ID',
    instance_id VARCHAR(128) NOT NULL COMMENT 'ECS 实例ID',
    instance_name VARCHAR(256) NULL COMMENT '实例名称',
    status VARCHAR(32) NULL COMMENT '运行状态',
    instance_type VARCHAR(64) NULL COMMENT '实例规格',
    zone_id VARCHAR(64) NULL COMMENT '可用区',
    public_ip VARCHAR(64) NULL COMMENT '公网IP（可选）',
    private_ip VARCHAR(64) NULL COMMENT '内网IP（可选）',
    os_name VARCHAR(128) NULL COMMENT '镜像/系统名称（可选）',
    memory_mb BIGINT NULL COMMENT '内存容量（MB，来自 DescribeInstances：Memory）',
    bt_panel_url VARCHAR(512) NULL COMMENT '宝塔面板 URL，同步时不覆盖',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, account_id, instance_id),
    KEY idx_account (tenant_id, account_id),
    KEY idx_instance_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 云实例元数据';

CREATE TABLE IF NOT EXISTS aa_nexus_instance_cpu_metric (
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id BIGINT NOT NULL COMMENT '云账号接入ID',
    instance_id VARCHAR(128) NOT NULL COMMENT '实例ID',
    ts DATETIME NOT NULL COMMENT '指标时间点（服务端解析后的时间）',
    value DOUBLE NOT NULL COMMENT 'CPU 指标值（先按 DescribeInstanceMonitorData 的 CPU 字段落库）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, account_id, instance_id, ts),
    KEY idx_instance_time (tenant_id, account_id, instance_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 实例 CPU 指标点（来自 DescribeInstanceMonitorData）';

CREATE TABLE IF NOT EXISTS aa_nexus_auto_sync_config (
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启自动巡检',
    global_interval_seconds INT NOT NULL DEFAULT 60 COMMENT '全局默认间隔（秒）',
    cpu_period_seconds INT NOT NULL DEFAULT 60 COMMENT 'DescribeInstanceMonitorData 的 Period（秒）',
    cpu_window_minutes INT NOT NULL DEFAULT 60 COMMENT 'DescribeInstanceMonitorData 查询窗口（分钟）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 自动巡检/同步配置';

-- 为默认 tenant_id=1 预置一行
INSERT IGNORE INTO aa_nexus_auto_sync_config (tenant_id, enabled, global_interval_seconds, cpu_period_seconds, cpu_window_minutes)
VALUES (1, 1, 60, 60, 60);

CREATE TABLE IF NOT EXISTS aa_nexus_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    actor_user_id BIGINT NULL COMMENT '操作者用户ID（可选）',
    actor_phone VARCHAR(32) NULL COMMENT '操作者手机号（可选）',
    action VARCHAR(64) NOT NULL COMMENT '动作标识，如 nexus.sync.manual',
    target_type VARCHAR(64) NULL COMMENT '目标类型，如 cloud_account / instance',
    target_id VARCHAR(128) NULL COMMENT '目标ID（字符串化）',
    result VARCHAR(32) NOT NULL COMMENT '结果：success/fail',
    meta_json TEXT NULL COMMENT '补充信息（脱敏）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_time (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 平台侧操作审计日志';

