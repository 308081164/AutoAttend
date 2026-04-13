-- ============================================================
-- Agent 智能引导功能 - 数据库迁移脚本
-- ============================================================

-- 会话表
CREATE TABLE IF NOT EXISTS `agent_session` (
    `id`                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    `tenant_id`           BIGINT       NOT NULL,
    `project_id`          BIGINT       NOT NULL,
    `public_token`        VARCHAR(64)  NOT NULL UNIQUE,
    `status`              VARCHAR(20)  NOT NULL DEFAULT 'active',
    `project_context`     TEXT,
    `background_context`  MEDIUMTEXT,
    `background_sources`  JSON,
    `summary_text`        MEDIUMTEXT,
    `ended_at`            DATETIME,
    `ended_by`            VARCHAR(20),
    `total_messages`      INT          DEFAULT 0,
    `total_input_tokens`  BIGINT       DEFAULT 0,
    `total_output_tokens` BIGINT       DEFAULT 0,
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_tenant_project` (`tenant_id`, `project_id`),
    UNIQUE KEY `uk_public_token` (`public_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 消息表
CREATE TABLE IF NOT EXISTS `agent_message` (
    `id`               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    `session_id`       BIGINT       NOT NULL,
    `role`             VARCHAR(20)  NOT NULL,
    `content`          MEDIUMTEXT   NOT NULL,
    `content_type`     VARCHAR(20)  NOT NULL DEFAULT 'text',
    `attachment_id`    BIGINT,
    `attachment_name`  VARCHAR(255),
    `input_tokens`     INT          DEFAULT 0,
    `output_tokens`    INT          DEFAULT 0,
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_session_created` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
