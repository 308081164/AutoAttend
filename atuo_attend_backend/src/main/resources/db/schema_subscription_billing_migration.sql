-- 订阅与模拟支付：租户权益截止时间、基线套餐；订单与平台审计
-- 幂等：列/表已存在时由 docker mysql --force 跳过错误

ALTER TABLE aa_tenant
  ADD COLUMN billing_baseline_plan_code VARCHAR(32) NOT NULL DEFAULT 'free' COMMENT '未付费时的套餐档位，权益到期后回退到此值' AFTER plan_code,
  ADD COLUMN subscription_ends_at DATETIME NULL COMMENT '当前付费/试用权益截止时间，NULL 表示无时限或已回退' AFTER billing_baseline_plan_code;

-- 存量租户：无订阅截止时间且套餐为 team/pro 时，将基线与套餐对齐（模拟付费租户会带 subscription_ends_at，不会被覆盖）
UPDATE aa_tenant SET billing_baseline_plan_code = plan_code
WHERE subscription_ends_at IS NULL AND plan_code IN ('team', 'pro');

CREATE TABLE IF NOT EXISTS aa_subscription_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  plan_code VARCHAR(32) NOT NULL COMMENT 'team|pro',
  amount_cents INT NOT NULL DEFAULT 0 COMMENT '标价（分），模拟支付用',
  currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
  provider VARCHAR(32) NOT NULL DEFAULT 'mock' COMMENT 'mock 表示无真实支付',
  status VARCHAR(32) NOT NULL DEFAULT 'completed' COMMENT 'completed|failed',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sub_order_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅订单（模拟支付审计）';

CREATE TABLE IF NOT EXISTS aa_platform_ops_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform_session_id BIGINT NULL,
  actor_label VARCHAR(128) NULL COMMENT '脱敏后的操作者标识',
  action VARCHAR(64) NOT NULL,
  tenant_id BIGINT NULL,
  payload_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_platform_audit_created (created_at),
  KEY idx_platform_audit_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台监测台运维操作审计';
