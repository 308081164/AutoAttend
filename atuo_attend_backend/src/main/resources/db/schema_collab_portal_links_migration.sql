-- 协作项目：开发与数据看板「传送门」链接配置（按项目持久化）

CREATE TABLE IF NOT EXISTS biz_project_portal_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    label VARCHAR(128) NOT NULL COMMENT '展示文本',
    url VARCHAR(1024) NOT NULL COMMENT '链接 URL',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project_id (project_id),
    KEY idx_project_sort (project_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作项目传送门链接';

