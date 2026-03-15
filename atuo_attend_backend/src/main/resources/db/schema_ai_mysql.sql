-- 单次提交 AI 分析（设计见 docs/单次提交AI分析-功能设计文档.md）
-- 依赖 schema_mysql.sql（aa_commit, aa_commit_diff）

-- AI 分析配置（当前版本仅 DeepSeek，API Key 由管理员在配置页填写）
CREATE TABLE IF NOT EXISTS aa_ai_analysis_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(32) NOT NULL DEFAULT 'deepseek' COMMENT 'ai 提供商',
    api_key VARCHAR(512) NULL COMMENT 'API Key，由用户在配置页填写',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用单次提交 AI 分析',
    model VARCHAR(64) NOT NULL DEFAULT 'deepseek-chat' COMMENT '模型标识',
    prompt_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '提示词版本',
    max_diff_chars INT NOT NULL DEFAULT 100000 COMMENT '单次 diff 最大字符数，超出截断',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 分析配置';

-- 分析任务表
CREATE TABLE IF NOT EXISTS aa_ai_analysis_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_full_name VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
    model VARCHAR(64) NULL,
    prompt_version VARCHAR(32) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_repo_sha (repo_full_name, commit_sha),
    KEY idx_status (status),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 分析任务';

-- 分析结果表
CREATE TABLE IF NOT EXISTS aa_ai_analysis_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_full_name VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    work_summary VARCHAR(512) NULL COMMENT '工作内容摘要',
    work_type VARCHAR(32) NULL COMMENT 'feature/bugfix/refactor/format_only/doc_only/config_only/other',
    main_area VARCHAR(255) NULL COMMENT '主要涉及模块/路径',
    is_effective VARCHAR(16) NULL COMMENT 'effective/weak_effective/ineffective',
    effective_reason TEXT NULL COMMENT '有效性理由',
    invalid_reason_tag VARCHAR(64) NULL,
    quality_level VARCHAR(16) NULL COMMENT 'high/medium/low 或分数档位',
    quality_comment TEXT NULL,
    risk_flags VARCHAR(512) NULL COMMENT 'JSON 数组',
    suggestions TEXT NULL COMMENT 'JSON 数组',
    raw_response TEXT NULL COMMENT '原始 AI 返回',
    prompt_version VARCHAR(32) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_repo_sha (repo_full_name, commit_sha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 分析结果';

-- Token 消耗记录（用于用量监测与提前充值）
CREATE TABLE IF NOT EXISTS aa_ai_token_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    provider VARCHAR(32) NOT NULL DEFAULT 'deepseek',
    model VARCHAR(64) NULL,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    repo_full_name VARCHAR(255) NULL,
    commit_sha VARCHAR(64) NULL,
    KEY idx_used_at (used_at),
    KEY idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用 Token 消耗记录';

-- 初始化默认配置行（DeepSeek）
INSERT INTO aa_ai_analysis_config (provider, api_key, enabled, model, prompt_version, max_diff_chars)
VALUES ('deepseek', NULL, 0, 'deepseek-chat', 'v1', 100000)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
