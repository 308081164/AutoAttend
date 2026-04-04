-- 快原型：异步生成任务（避免 nginx 同步等待 LLM 导致 504）
CREATE TABLE IF NOT EXISTS aa_ui_prototype_generate_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id BIGINT NOT NULL COMMENT '原型项目ID',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
    error_message TEXT NULL COMMENT '失败原因',
    spec_version INT NULL COMMENT '成功时写入的版本号',
    prompt_snapshot MEDIUMTEXT NULL COMMENT '请求摘要（审计/排错；长 prompt 截断入库）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_project (tenant_id, project_id),
    KEY idx_tenant_project_status (tenant_id, project_id, status),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快原型 spec 异步生成任务';
