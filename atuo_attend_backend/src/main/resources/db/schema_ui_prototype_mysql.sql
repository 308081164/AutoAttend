-- 快原型：多项目 + UI Spec 版本存储
-- 说明：tenant_id 从一开始就随表落库，避免依赖后续 TenantPhase1SchemaBootstrap 对“列新增”的处理。

CREATE TABLE IF NOT EXISTS aa_ui_prototype_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name VARCHAR(255) NOT NULL COMMENT '项目名称',
    current_spec_version INT NULL COMMENT '当前 spec 版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_updated (tenant_id, updated_at),
    KEY idx_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI UI 原型项目';

CREATE TABLE IF NOT EXISTS aa_ui_prototype_spec (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    version INT NOT NULL COMMENT 'spec 版本号（单项目从 1 开始递增）',
    spec_json MEDIUMTEXT NOT NULL COMMENT 'UI spec（JSON 字符串）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_project_version (tenant_id, project_id, version),
    KEY idx_tenant_project (tenant_id, project_id),
    KEY idx_tenant_project_created (tenant_id, project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI UI 原型 spec 版本';

