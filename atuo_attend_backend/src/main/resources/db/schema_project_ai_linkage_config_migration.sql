CREATE TABLE IF NOT EXISTS aa_project_ai_linkage_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    mode VARCHAR(32) NOT NULL DEFAULT 'auto',
    min_confidence VARCHAR(16) NOT NULL DEFAULT 'medium',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_id (project_id),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目级 commit AI 联动配置';

