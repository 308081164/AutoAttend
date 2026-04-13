-- ============================================================
-- Agent 智能引导功能 - 数据库迁移脚本
-- ============================================================

-- 会话表
CREATE TABLE IF NOT EXISTS `aa_agent_session` (
    `id`                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    `tenant_id`           BIGINT       NOT NULL COMMENT '租户ID',
    `project_id`          BIGINT       NOT NULL COMMENT '关联报价项目ID',
    `public_token`        VARCHAR(64)  NOT NULL UNIQUE COMMENT '公开访问令牌',
    `status`              VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '会话状态: active/ended/terminated',
    `project_context`     TEXT,
    `background_context`  MEDIUMTEXT   COMMENT '背景资料摘要',
    `background_sources`  JSON         COMMENT '背景来源JSON',
    `summary_text`        MEDIUMTEXT   COMMENT 'AI 生成的需求摘要',
    `ended_at`            DATETIME,
    `ended_by`            VARCHAR(20),
    `total_messages`      INT          DEFAULT 0,
    `total_input_tokens`  BIGINT       DEFAULT 0,
    `total_output_tokens` BIGINT       DEFAULT 0,
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_agent_tenant_project` (`tenant_id`, `project_id`),
    UNIQUE KEY `uk_agent_public_token` (`public_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 智能引导会话';

-- 消息表
CREATE TABLE IF NOT EXISTS `aa_agent_message` (
    `id`               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    `session_id`       BIGINT       NOT NULL COMMENT '关联会话ID',
    `role`             VARCHAR(20)  NOT NULL COMMENT '消息角色: user/assistant',
    `content`          MEDIUMTEXT   NOT NULL,
    `content_type`     VARCHAR(20)  NOT NULL DEFAULT 'text' COMMENT '内容类型: text/image',
    `attachment_id`    BIGINT,
    `attachment_name`  VARCHAR(255),
    `input_tokens`     INT          DEFAULT 0,
    `output_tokens`    INT          DEFAULT 0,
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_agent_msg_session_id` (`session_id`),
    INDEX `idx_agent_msg_session_created` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 智能引导消息';
