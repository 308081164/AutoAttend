-- 阶段 2/3：套餐字段、邀请表（幂等，需 MySQL 8.0.29+ 支持 IF NOT EXISTS）

ALTER TABLE aa_tenant ADD COLUMN IF NOT EXISTS plan_code VARCHAR(32) NOT NULL DEFAULT 'free' COMMENT '套餐档位 free|team|pro';
ALTER TABLE aa_tenant ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active|suspended';

CREATE TABLE IF NOT EXISTS aa_tenant_invite (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  token VARCHAR(64) NOT NULL COMMENT '邀请令牌（一次性展示给管理员）',
  expires_at DATETIME NOT NULL,
  max_uses INT NOT NULL DEFAULT 1,
  used_count INT NOT NULL DEFAULT 0,
  note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_aa_tenant_invite_token (token),
  KEY idx_aa_tenant_invite_tenant (tenant_id),
  CONSTRAINT fk_aa_tenant_invite_tenant FOREIGN KEY (tenant_id) REFERENCES aa_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
