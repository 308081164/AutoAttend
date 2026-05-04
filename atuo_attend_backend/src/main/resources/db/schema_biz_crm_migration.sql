-- 商务系统（CRM）表结构
-- 客户管理、跟进记录、商机看板
-- 执行前请 USE 你的库；可与现有 schema 并存。

CREATE TABLE IF NOT EXISTS aa_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    name VARCHAR(255) NOT NULL COMMENT '客户名称/联系人姓名',
    company VARCHAR(255) NULL COMMENT '公司名称',
    phone VARCHAR(64) NULL COMMENT '联系电话',
    email VARCHAR(255) NULL COMMENT '邮箱',
    source VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源：manual|referral|marketplace|other',
    stage VARCHAR(32) NOT NULL DEFAULT 'lead' COMMENT '阶段：lead|contacted|qualified|negotiation|closed_won|closed_lost',
    tags VARCHAR(512) NULL COMMENT '标签（逗号分隔）',
    assigned_to BIGINT NULL COMMENT '负责人（admin_user_id）',
    last_contacted_at DATETIME NULL COMMENT '最后联系时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_updated (tenant_id, updated_at),
    KEY idx_tenant_stage (tenant_id, stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_customer_followup (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    customer_id BIGINT NOT NULL COMMENT '关联客户 ID',
    method VARCHAR(32) NOT NULL DEFAULT 'note' COMMENT '方式：call|email|meeting|note|other',
    content TEXT NOT NULL COMMENT '跟进内容',
    operator_id BIGINT NULL COMMENT '操作人（admin_user_id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_customer (tenant_id, customer_id, created_at),
    CONSTRAINT fk_followup_customer FOREIGN KEY (customer_id) REFERENCES aa_customer (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aa_opportunity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    customer_id BIGINT NULL COMMENT '关联客户 ID',
    quote_project_id BIGINT NULL COMMENT '关联报价项目 ID',
    name VARCHAR(255) NOT NULL COMMENT '商机名称',
    expected_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '预计金额',
    stage VARCHAR(32) NOT NULL DEFAULT 'discovery' COMMENT '阶段：discovery|qualification|proposal|negotiation|closed_won|closed_lost',
    win_probability INT NOT NULL DEFAULT 10 COMMENT '赢单概率（0-100）',
    expected_close_date DATE NULL COMMENT '预计成交日期',
    assigned_to BIGINT NULL COMMENT '负责人（admin_user_id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_updated (tenant_id, updated_at),
    KEY idx_tenant_stage (tenant_id, stage),
    KEY idx_customer (tenant_id, customer_id),
    CONSTRAINT fk_opportunity_customer FOREIGN KEY (customer_id) REFERENCES aa_customer (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报价项目表增加 customer_id 字段（若不存在）
SET @dbname = DATABASE();
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'customer_id');
SET @sql = IF(@exists = 0,
    'ALTER TABLE biz_quote_project ADD COLUMN customer_id BIGINT NULL COMMENT "关联客户 ID" AFTER link_table_id, ADD KEY idx_customer (customer_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
