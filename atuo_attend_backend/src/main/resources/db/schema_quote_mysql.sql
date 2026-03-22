-- 需求→报价→合同 半自动化（MVP）表结构
-- 执行前请 USE 你的库；可与现有 schema 并存。

CREATE TABLE IF NOT EXISTS biz_quote_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    project_type VARCHAR(64) NOT NULL DEFAULT 'other',
    tech_stack VARCHAR(64) NOT NULL DEFAULT 'vue_node',
    design_type VARCHAR(64) NOT NULL DEFAULT 'need_design',
    data_migration VARCHAR(64) NOT NULL DEFAULT 'none',
    concurrency VARCHAR(32) NOT NULL DEFAULT 'lt100',
    security_level VARCHAR(32) NOT NULL DEFAULT 'normal',
    deploy_type VARCHAR(32) NOT NULL DEFAULT 'cloud',
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    link_table_id BIGINT NULL COMMENT '可选关联多维表项目',
    prd_summary TEXT NULL COMMENT 'PRD/需求摘要，用于置信度',
    quote_calc_prefs_json TEXT NULL COMMENT '报价计算与审核清单偏好(JSON)',
    quote_contract_context_json TEXT NULL COMMENT '合同补充：付款计划、质保、验收、交付物、里程碑等(JSON)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    KEY idx_project (quote_project_id),
    CONSTRAINT fk_quote_module_project FOREIGN KEY (quote_project_id) REFERENCES biz_quote_project (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_id BIGINT NOT NULL,
    name VARCHAR(512) NOT NULL,
    complexity VARCHAR(32) NOT NULL DEFAULT 'standard',
    quantity INT NOT NULL DEFAULT 1,
    estimated_days DECIMAL(10,2) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    KEY idx_module (module_id),
    CONSTRAINT fk_quote_item_module FOREIGN KEY (module_id) REFERENCES biz_quote_module (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_baseline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tech_stack VARCHAR(64) NOT NULL,
    complexity VARCHAR(32) NOT NULL,
    days DECIMAL(10,2) NOT NULL,
    UNIQUE KEY uk_stack_complexity (tech_stack, complexity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_risk_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    risk_key VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    default_pct DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '百分比，如 10 表示 +10%',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uk_risk_key (risk_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_price_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    region_label VARCHAR(128) NOT NULL,
    price_per_day DECIMAL(12,2) NOT NULL,
    duration_coefficient DECIMAL(10,4) NOT NULL DEFAULT 1.2000 COMMENT '工期系数：预计工期(天)=总人天×系数',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uk_region (region_label),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_project_id BIGINT NOT NULL,
    total_days DECIMAL(12,2) NOT NULL,
    estimated_duration_days DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '预计工期(天)=总人天×工期系数',
    base_amount DECIMAL(14,2) NOT NULL,
    risk_pct_total DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'Σ 风险百分比',
    risk_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(14,2) NOT NULL,
    confidence_score INT NOT NULL DEFAULT 70,
    audit_checklist_json TEXT NULL,
    selected_risks_json TEXT NULL,
    price_per_day_used DECIMAL(12,2) NOT NULL,
    duration_coefficient_used DECIMAL(10,4) NOT NULL DEFAULT 1.2000 COMMENT '计算时采用的工期系数',
    region_label_used VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (quote_project_id),
    CONSTRAINT fk_quote_result_project FOREIGN KEY (quote_project_id) REFERENCES biz_quote_project (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_result_id BIGINT NOT NULL,
    doc_type VARCHAR(32) NOT NULL COMMENT 'quote / contract',
    content MEDIUMTEXT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_result (quote_result_id),
    CONSTRAINT fk_quote_doc_result FOREIGN KEY (quote_result_id) REFERENCES biz_quote_result (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_contract_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_result_id BIGINT NOT NULL,
    client_name VARCHAR(255) NULL,
    company_name VARCHAR(255) NULL,
    template_type VARCHAR(64) NOT NULL DEFAULT 'software_dev',
    ai_prompt_snapshot MEDIUMTEXT NULL,
    ai_raw_response MEDIUMTEXT NULL,
    edited_content MEDIUMTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_result (quote_result_id),
    CONSTRAINT fk_contract_result FOREIGN KEY (quote_result_id) REFERENCES biz_quote_result (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_actual (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_project_id BIGINT NOT NULL,
    estimated_days DECIMAL(12,2) NULL,
    actual_days DECIMAL(12,2) NULL,
    deviation_reason TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (quote_project_id),
    CONSTRAINT fk_quote_actual_project FOREIGN KEY (quote_project_id) REFERENCES biz_quote_project (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biz_quote_preset_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(512) NOT NULL,
    complexity VARCHAR(32) NOT NULL DEFAULT 'standard',
    category VARCHAR(128) NULL COMMENT '分组/模块建议',
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 人天基准（与设计文档附录一致，可按团队调整）
INSERT INTO biz_quote_baseline (tech_stack, complexity, days) VALUES
('vue_node', 'simple', 0.5), ('vue_node', 'standard', 1.5), ('vue_node', 'medium', 3), ('vue_node', 'complex', 6), ('vue_node', 'extreme', 10),
('react_java', 'simple', 0.6), ('react_java', 'standard', 1.8), ('react_java', 'medium', 3.5), ('react_java', 'complex', 7), ('react_java', 'extreme', 12),
('miniprogram', 'simple', 0.4), ('miniprogram', 'standard', 1.2), ('miniprogram', 'medium', 2.5), ('miniprogram', 'complex', 5), ('miniprogram', 'extreme', 8),
('flutter', 'simple', 0.7), ('flutter', 'standard', 2), ('flutter', 'medium', 4), ('flutter', 'complex', 8), ('flutter', 'extreme', 15),
('other', 'simple', 0.5), ('other', 'standard', 1.5), ('other', 'medium', 3), ('other', 'complex', 6), ('other', 'extreme', 10)
ON DUPLICATE KEY UPDATE days = VALUES(days);

INSERT INTO biz_quote_risk_config (risk_key, label, default_pct, enabled) VALUES
('requirement_change', '需求变更风险', 15, 1),
('tech_risk', '技术风险（新技术/预研）', 12, 1),
('communication', '沟通成本（异地/非技术客户）', 8, 1),
('maintenance', '维护预留', 12, 1),
('standard_cycle', '标准交付周期（非加急）', -10, 1),
('urgency_rush', '加急', 25, 1)
ON DUPLICATE KEY UPDATE label = VALUES(label), default_pct = VALUES(default_pct);

INSERT INTO biz_quote_price_config (region_label, price_per_day, currency, enabled) VALUES
('一线/新一线（参考）', 1800.00, 'CNY', 1),
('二线及其他（参考）', 1200.00, 'CNY', 1)
ON DUPLICATE KEY UPDATE price_per_day = VALUES(price_per_day);

-- 预设功能点种子数据勿写在此：本文件每次容器启动都会执行，无条件 INSERT 会产生重复行。
-- 幂等插入见 migrate_manifest 中的 schema_quote_preset_item_dedupe_migration.sql（去重）与
-- schema_quote_preset_risk_custom_migration.sql（按名称 NOT EXISTS 插入）。
