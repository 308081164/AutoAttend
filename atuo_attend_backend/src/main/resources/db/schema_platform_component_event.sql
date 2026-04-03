-- 平台监测：组件点击/功能使用事件落库表
-- 用于跨租户聚合统计「组件点击数」「核心接口调用次数（用 usage 事件表示）」。
CREATE TABLE IF NOT EXISTS aa_platform_component_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    admin_user_id BIGINT NULL COMMENT '管理员用户ID',
    admin_phone VARCHAR(32) NULL COMMENT '管理员手机号',
    component_key VARCHAR(64) NOT NULL COMMENT '组件key（维度）',
    core_api_key VARCHAR(64) NULL COMMENT '核心接口key（监测指标）',
    event_type VARCHAR(16) NOT NULL COMMENT 'click/usage',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_created_at (created_at),
    KEY idx_tenant_created (tenant_id, created_at),
    KEY idx_component_type (component_key, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台组件事件（点击/使用）';

