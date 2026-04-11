-- 客户项目阅览看板：按协作项目配置公开 token 与展示模块

CREATE TABLE IF NOT EXISTS biz_project_client_board (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL COMMENT 'biz_project.id',
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    public_token VARCHAR(64) NOT NULL COMMENT 'URL 中的不透明令牌',
    show_progress_dashboard TINYINT(1) NOT NULL DEFAULT 1,
    show_feature_backlog TINYINT(1) NOT NULL DEFAULT 0,
    show_ai_table_entry TINYINT(1) NOT NULL DEFAULT 0,
    ai_purpose VARCHAR(32) NOT NULL DEFAULT 'issue_tracking' COMMENT 'AI 录入目标表 purpose',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_client_board_project (project_id),
    UNIQUE KEY uk_client_board_token (public_token),
    KEY idx_client_board_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作项目客户阅览看板配置';
