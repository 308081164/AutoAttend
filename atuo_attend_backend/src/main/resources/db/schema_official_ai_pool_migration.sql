-- 官方 API 池：租户余额、用量来源、兑换码
SET @db := DATABASE();

-- aa_tenant：官方赠送/兑换的 API 额度（人民币）
SET @x := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'aa_tenant' AND column_name = 'official_api_cny_balance');
SET @s := IF(@x = 0, 'ALTER TABLE aa_tenant ADD COLUMN official_api_cny_balance DECIMAL(14,4) NOT NULL DEFAULT 0 COMMENT ''官方 API 额度余额（元）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- aa_ai_token_usage：区分官方/个人，记录折算成本（元）
SET @y := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'aa_ai_token_usage' AND column_name = 'usage_source');
SET @s2 := IF(@y = 0, 'ALTER TABLE aa_ai_token_usage ADD COLUMN usage_source VARCHAR(16) NOT NULL DEFAULT ''personal'' COMMENT ''official=官方池 personal=用户自备Key''', 'SELECT 1');
PREPARE stmt FROM @s2; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @z := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'aa_ai_token_usage' AND column_name = 'cost_yuan');
SET @s3 := IF(@z = 0, 'ALTER TABLE aa_ai_token_usage ADD COLUMN cost_yuan DECIMAL(14,8) NULL COMMENT ''本次调用折算成本（元），官方池计费''', 'SELECT 1');
PREPARE stmt FROM @s3; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS aa_token_redeem_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 小写hex',
    grant_cny DECIMAL(14,4) NOT NULL COMMENT '每张兑换增加的官方额度（元）',
    max_uses INT NOT NULL DEFAULT 1,
    used_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code_hash (code_hash),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官方API额度兑换码';

CREATE TABLE IF NOT EXISTS aa_token_redeem_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    grant_cny DECIMAL(14,4) NOT NULL,
    redeemed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tenant (tenant_id),
    KEY idx_code (code_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换码使用记录';
