-- 项目信息发布（外包信息中介）：租户管理员发布权限 + 项目表

ALTER TABLE aa_tenant_admin_user
    ADD COLUMN can_publish_project_info TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可发布项目信息（租户内）'
        AFTER sms_login_onboarded;

CREATE TABLE IF NOT EXISTS aa_marketplace_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    publisher_user_id BIGINT NOT NULL COMMENT 'aa_tenant_admin_user.id',
    title VARCHAR(512) NOT NULL,
    description MEDIUMTEXT NULL,
    tech_stack_json TEXT NULL COMMENT 'JSON 数组：技术栈标签',
    budget_range VARCHAR(255) NULL,
    duration VARCHAR(128) NULL,
    location VARCHAR(255) NULL,
    contact_type VARCHAR(32) NOT NULL DEFAULT 'phone' COMMENT 'phone|email|internal',
    contact_value_enc TEXT NULL COMMENT 'AES-GCM 密文（Base64）',
    status VARCHAR(32) NOT NULL DEFAULT 'pending_review' COMMENT 'pending_review|open|closed|completed|rejected',
    reject_reason VARCHAR(512) NULL,
    view_count INT NOT NULL DEFAULT 0,
    publish_time DATETIME NULL,
    expire_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_status (tenant_id, status),
    KEY idx_tenant_publish (tenant_id, publish_time),
    KEY idx_publisher (publisher_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目信息发布';
