-- AI Token 消耗记录（用于用量监测与提前充值）
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
