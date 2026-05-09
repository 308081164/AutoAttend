-- ============================================================
-- 咸鱼值守模块 - 数据库迁移
-- Phase B: 基础值守（MVP）
-- 表: aa_xianyu_account, aa_xianyu_conversation, aa_xianyu_message, aa_xianyu_quick_reply
-- ============================================================

-- 咸鱼账号表
CREATE TABLE IF NOT EXISTS aa_xianyu_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    nickname VARCHAR(100) DEFAULT NULL COMMENT '咸鱼昵称',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    cookie_encrypted TEXT DEFAULT NULL COMMENT '加密后的Cookie',
    status VARCHAR(20) DEFAULT 'offline' COMMENT 'online/offline/expired',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_active_at DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咸鱼账号表';

-- 咸鱼会话表
CREATE TABLE IF NOT EXISTS aa_xianyu_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '关联账号ID',
    peer_id VARCHAR(100) DEFAULT NULL COMMENT '对方用户ID',
    peer_nickname VARCHAR(100) DEFAULT NULL COMMENT '对方昵称',
    peer_avatar VARCHAR(500) DEFAULT NULL COMMENT '对方头像',
    last_message TEXT DEFAULT NULL COMMENT '最后一条消息内容',
    last_message_at DATETIME DEFAULT NULL COMMENT '最后消息时间',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/archived',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_account_id (account_id),
    INDEX idx_status (status),
    INDEX idx_last_message_at (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咸鱼会话表';

-- 咸鱼消息表
CREATE TABLE IF NOT EXISTS aa_xianyu_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '关联会话ID',
    direction VARCHAR(10) DEFAULT NULL COMMENT 'in/out',
    content TEXT DEFAULT NULL COMMENT '消息内容',
    msg_type VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/file',
    file_url VARCHAR(500) DEFAULT NULL COMMENT '文件URL',
    sent_at DATETIME DEFAULT NULL COMMENT '发送时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咸鱼消息表';

-- 咸鱼快捷回复模板表
CREATE TABLE IF NOT EXISTS aa_xianyu_quick_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    title VARCHAR(100) DEFAULT NULL COMMENT '回复标题/标签',
    content TEXT DEFAULT NULL COMMENT '回复内容',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咸鱼快捷回复模板表';
