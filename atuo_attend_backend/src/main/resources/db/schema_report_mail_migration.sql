-- 项目级工作日报邮件：项目日报 + 开发者个人日报
-- 幂等迁移：表不存在则创建。

CREATE TABLE IF NOT EXISTS aa_report_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '总开关：1=启用，0=关闭',
    schedule_type VARCHAR(32) NULL COMMENT '策略类型：WORKDAY / WEEKLY / DAILY / MONTHLY（可选）',
    schedule_param VARCHAR(255) NULL COMMENT '策略参数 JSON（可选）',
    cron_expression VARCHAR(64) NULL COMMENT 'cron（可选，与 schedule_type 二选一）',
    company_name VARCHAR(128) NULL COMMENT '企业名称（邮件标题/抬头）',
    daily_extra_message TEXT NULL COMMENT '当日附加信息（可选）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日报邮件：全局配置';

CREATE TABLE IF NOT EXISTS aa_report_recipient (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NULL,
    unsubscribed_at DATETIME NULL COMMENT '退订时间，NULL 表示未退订',
    unsubscribe_token VARCHAR(64) NULL COMMENT '退订 token',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日报邮件：收件人与退订';

CREATE TABLE IF NOT EXISTS aa_report_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日报邮件：黑名单';

CREATE TABLE IF NOT EXISTS aa_project_report_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '协作项目 ID',
    repo_full_name VARCHAR(255) NULL COMMENT '绑定的 GitHub owner/repo（可选）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用该项目日报',
    send_to_managers TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否向项目负责人发送项目日报',
    send_to_developers TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否向开发者发送个人日报',
    manager_emails TEXT NULL COMMENT 'JSON 数组：项目负责人/额外收件人邮箱列表',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_id (project_id),
    KEY idx_repo_full_name (repo_full_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日报邮件：项目级配置';

CREATE TABLE IF NOT EXISTS aa_report_send_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    report_date DATE NOT NULL,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(16) NOT NULL DEFAULT 'success' COMMENT 'success / failed',
    error_message TEXT NULL,
    report_type VARCHAR(16) NOT NULL COMMENT 'project / developer',
    project_id BIGINT NULL,
    repo_full_name VARCHAR(255) NULL,
    KEY idx_report_date (report_date),
    KEY idx_email (email),
    KEY idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日报邮件：发送记录';

