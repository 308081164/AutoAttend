-- 租户级 Penpot 凭证（邮箱/密码与 Access Token 均 AES-GCM 加密落库；一租户一账号）
CREATE TABLE IF NOT EXISTS aa_tenant_penpot_credential (
  tenant_id BIGINT NOT NULL PRIMARY KEY,
  penpot_email VARCHAR(320) NOT NULL,
  password_enc TEXT NOT NULL COMMENT 'AES-GCM 密文',
  access_token_enc TEXT NOT NULL COMMENT 'AES-GCM 密文',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_aa_tenant_penpot_email (penpot_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
