-- Nexus 内存利用率指标落库表（来自阿里云 CloudMonitor：acs_ecs_dashboard / memory_usedutilization）
CREATE TABLE IF NOT EXISTS aa_nexus_instance_memory_metric (
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id BIGINT NOT NULL COMMENT '云账号接入ID',
    instance_id VARCHAR(128) NOT NULL COMMENT 'ECS 实例ID',
    ts DATETIME NOT NULL COMMENT '指标时间点（服务端解析后的时间）',
    value DOUBLE NOT NULL COMMENT '内存利用率（%）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, account_id, instance_id, ts),
    KEY idx_instance_time (tenant_id, account_id, instance_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nexus 实例内存利用率指标点（来自 CMS DescribeMetricList）';

