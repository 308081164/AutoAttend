-- AutoAttend MVP (MySQL) schema
-- 创建数据库示例：
--   CREATE DATABASE autoattend DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   USE autoattend;

CREATE TABLE IF NOT EXISTS aa_commit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_full_name VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    parent_sha VARCHAR(64) NULL,
    author_name VARCHAR(128) NULL,
    author_email VARCHAR(255) NULL,
    committed_at DATETIME NULL,
    message TEXT NULL,
    files_changed INT NOT NULL DEFAULT 0,
    insertions INT NOT NULL DEFAULT 0,
    deletions INT NOT NULL DEFAULT 0,
    valid_commit TINYINT(1) NOT NULL DEFAULT 1,
    valid_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_repo_sha (repo_full_name, commit_sha),
    KEY idx_author (author_email),
    KEY idx_committed_at (committed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_commit_diff (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_full_name VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    diff_text LONGTEXT NOT NULL,
    diff_size_bytes BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_repo_sha (repo_full_name, commit_sha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_webhook_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    delivery_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_delivery (delivery_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统配置（如 GitHub Token，由管理后台「AI 配置」页统一填写与维护）
CREATE TABLE IF NOT EXISTS aa_system_config (
    config_key VARCHAR(128) NOT NULL PRIMARY KEY,
    config_value TEXT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置 key-value';

