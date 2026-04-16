-- 快原型 Penpot Beta：项目与 Penpot 文件映射 + 异步任务
ALTER TABLE aa_ui_prototype_project
    ADD COLUMN penpot_team_id VARCHAR(64) NULL COMMENT 'Penpot 团队 UUID' AFTER current_spec_version,
    ADD COLUMN penpot_project_id VARCHAR(64) NULL COMMENT 'Penpot 项目 UUID' AFTER penpot_team_id,
    ADD COLUMN penpot_file_id VARCHAR(64) NULL COMMENT '当前关联的 Penpot 文件 UUID' AFTER penpot_project_id,
    ADD COLUMN penpot_preview_url VARCHAR(1024) NULL COMMENT '最近一次生成的预览链接（可选缓存）' AFTER penpot_file_id;

CREATE TABLE IF NOT EXISTS aa_ui_prototype_penpot_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id BIGINT NOT NULL COMMENT '快原型项目ID',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending|running|success|failed',
    error_message TEXT NULL,
    prompt_snapshot MEDIUMTEXT NULL,
    penpot_file_id VARCHAR(64) NULL COMMENT '本次任务创建的文件 UUID',
    penpot_preview_url VARCHAR(1024) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_project (tenant_id, project_id),
    KEY idx_tenant_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快原型 Penpot 异步任务';
