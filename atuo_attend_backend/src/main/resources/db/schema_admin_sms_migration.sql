-- 租户管理员短信验证码（登录/注册）
CREATE TABLE IF NOT EXISTS aa_admin_sms_code (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(32) NOT NULL COMMENT 'E.164',
  purpose VARCHAR(16) NOT NULL COMMENT 'login | register',
  code_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hex',
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  verify_attempts INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_phone_purpose_created (phone, purpose, created_at),
  KEY idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
