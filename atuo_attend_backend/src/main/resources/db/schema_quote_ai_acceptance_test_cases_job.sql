-- 异步生成「验收测试用例/测试清单」job（AI 生成）
-- 用于避免 nginx/http 网关超时（504）
CREATE TABLE IF NOT EXISTS aa_quote_ai_acceptance_test_cases_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    quote_project_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
    request_snapshot MEDIUMTEXT NULL COMMENT '请求快照（用于排查；可能截断）',
    result_json MEDIUMTEXT NULL COMMENT '生成结果 JSON（与前端原有 acceptanceTestCases 结构一致）',
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_project_status (tenant_id, quote_project_id, status),
    KEY idx_tenant_job_status (tenant_id, id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

