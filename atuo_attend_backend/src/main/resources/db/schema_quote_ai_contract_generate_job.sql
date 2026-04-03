-- 异步生成「合同正文」job（AI 生成）
-- 用于避免 nginx/http 网关超时（504）
CREATE TABLE IF NOT EXISTS aa_quote_ai_contract_generate_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    quote_result_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
    request_snapshot MEDIUMTEXT NULL COMMENT '请求快照（用于排查；可能截断）',
    edited_content LONGTEXT NULL COMMENT 'AI 生成/编辑后的合同正文（与前端合同正文字段一致）',
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_result_status (tenant_id, quote_result_id, status),
    KEY idx_tenant_job_status (tenant_id, id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

