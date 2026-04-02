-- 快原型：HTML+CSS mockup（单项目仅保存“当前产物”，不做版本控制）
CREATE TABLE IF NOT EXISTS aa_ui_prototype_mockup (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id BIGINT NOT NULL COMMENT '原型项目ID',
    html MEDIUMTEXT NOT NULL COMMENT 'mockup html（body 内内容）',
    css MEDIUMTEXT NOT NULL COMMENT 'mockup css',
    raw_ai_content MEDIUMTEXT NULL COMMENT 'AI 原始输出（便于排错/对照）',
    messages_json MEDIUMTEXT NULL COMMENT '对话消息（JSON，供前端恢复 chat log）',
    model_used VARCHAR(64) NULL COMMENT '模型（用于展示/排错）',
    repaired TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否走过修复流程',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_project (tenant_id, project_id),
    KEY idx_tenant_updated (tenant_id, updated_at),
    KEY idx_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快原型 HTML+CSS mockup（当前产物）';

