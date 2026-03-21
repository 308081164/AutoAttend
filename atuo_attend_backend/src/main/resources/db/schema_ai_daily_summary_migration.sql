-- 项目每日进展总结：配置开关 + 按仓库按日存储的 AI 总结正文
-- 若列/表已存在，执行会报错，可在迁移日志中忽略。

ALTER TABLE aa_ai_analysis_config
    ADD COLUMN daily_summary_enabled TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '是否每日定时生成各仓库进展总结（DeepSeek）' AFTER enabled;

CREATE TABLE IF NOT EXISTS aa_project_daily_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_full_name VARCHAR(255) NOT NULL COMMENT 'GitHub owner/repo',
    summary_date DATE NOT NULL COMMENT '所总结的业务日（默认 Asia/Shanghai 自然日）',
    title VARCHAR(512) NULL COMMENT 'AI 生成或衍生的短标题',
    content MEDIUMTEXT NOT NULL COMMENT 'Markdown 报告正文',
    commit_count INT NOT NULL DEFAULT 0 COMMENT '当日纳入统计的提交数',
    model VARCHAR(64) NULL COMMENT '使用的模型',
    status VARCHAR(16) NOT NULL DEFAULT 'success' COMMENT 'success / failed',
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_repo_summary_date (repo_full_name, summary_date),
    KEY idx_summary_date (summary_date),
    KEY idx_repo (repo_full_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按项目（仓库）按日的 AI 进展总结';
