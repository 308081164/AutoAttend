-- 多维表格协作模块：项目随仓库自动创建，用户由 commit 作者同步
-- 执行顺序：在 schema_mysql.sql 之后

-- 协作用户（来自 commit author_email，默认密码 123456）
CREATE TABLE IF NOT EXISTS biz_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(128) NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'member',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email (email),
    KEY idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 项目（与仓库一一对应，repo_id = repo_full_name；随仓库首次 commit 自动创建）
CREATE TABLE IF NOT EXISTS biz_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(512) NULL,
    repo_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_repo_id (repo_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 项目成员（source: sync=仓库作者同步, manual=管理员添加）
CREATE TABLE IF NOT EXISTS biz_project_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'member',
    source VARCHAR(16) NOT NULL DEFAULT 'sync',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_user (project_id, user_id),
    KEY idx_user (user_id),
    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES biz_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 项目绑定表（一项目一表）
CREATE TABLE IF NOT EXISTS biz_project_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL DEFAULT '任务表',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project (project_id),
    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 选项组（scope: project/global；归属模块等按项目配置）
CREATE TABLE IF NOT EXISTS biz_option_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    `options` JSON NOT NULL,
    scope VARCHAR(16) NOT NULL DEFAULT 'global',
    project_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_scope_project (scope, project_id),
    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 表列定义
CREATE TABLE IF NOT EXISTS biz_table_column (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    column_type VARCHAR(32) NOT NULL,
    option_group_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_table (table_id),
    FOREIGN KEY (table_id) REFERENCES biz_project_table(id) ON DELETE CASCADE,
    FOREIGN KEY (option_group_id) REFERENCES biz_option_group(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 记录（行）
CREATE TABLE IF NOT EXISTS biz_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_table (table_id),
    KEY idx_created_by (created_by),
    FOREIGN KEY (table_id) REFERENCES biz_project_table(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES biz_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 记录字段值（多列存为多行或 JSON）
CREATE TABLE IF NOT EXISTS biz_record_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    column_id BIGINT NOT NULL,
    value_text TEXT NULL,
    value_number DOUBLE NULL,
    value_date DATETIME NULL,
    value_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_column (record_id, column_id),
    FOREIGN KEY (record_id) REFERENCES biz_record(id) ON DELETE CASCADE,
    FOREIGN KEY (column_id) REFERENCES biz_table_column(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 记录讨论/留言
CREATE TABLE IF NOT EXISTS biz_record_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record (record_id),
    FOREIGN KEY (record_id) REFERENCES biz_record(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES biz_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 附件（MinIO storage_key）
CREATE TABLE IF NOT EXISTS biz_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    storage_key VARCHAR(512) NOT NULL,
    uploaded_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record (record_id),
    FOREIGN KEY (record_id) REFERENCES biz_record(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES biz_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
