-- 推荐邀请：租户推荐关系、会员积分、邀请码、分成流水
-- 幂等：单列重复时 mysql --force 会跳过 ERROR 1060，其余列仍可添加

ALTER TABLE aa_tenant ADD COLUMN referrer_tenant_id BIGINT NULL COMMENT '推荐方租户 ID（被邀请注册时写入）' AFTER slug;
ALTER TABLE aa_tenant ADD COLUMN invite_code_redeemed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已使用过邀请码兑换' AFTER referrer_tenant_id;
ALTER TABLE aa_tenant ADD COLUMN member_points INT NOT NULL DEFAULT 0 COMMENT '会员积分，1 积分=1 元购买力' AFTER invite_code_redeemed;
ALTER TABLE aa_tenant ADD COLUMN team_first_month_used TINYINT(1) NOT NULL DEFAULT 0 COMMENT '尝鲜版首月优惠价是否已用过' AFTER member_points;

CREATE TABLE IF NOT EXISTS aa_invite_code (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL COMMENT '大写字母数字',
  created_by VARCHAR(16) NOT NULL COMMENT 'platform | user',
  referrer_tenant_id BIGINT NOT NULL COMMENT '推荐方租户（分成与积分归属）',
  creator_user_id BIGINT NULL COMMENT '用户自建时管理员用户 ID',
  expires_at DATETIME NULL COMMENT 'NULL 表示永久（仅用户自建）',
  disabled TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_invite_code (code),
  KEY idx_referrer (referrer_tenant_id),
  KEY idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码';

CREATE TABLE IF NOT EXISTS aa_referral_commission (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  referrer_tenant_id BIGINT NOT NULL,
  source_tenant_id BIGINT NOT NULL COMMENT '产生消费的被推荐租户',
  order_amount_cents INT NOT NULL DEFAULT 0,
  commission_cents INT NOT NULL DEFAULT 0 COMMENT '10% 分成（分）',
  subscription_order_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_referrer_created (referrer_tenant_id, created_at),
  KEY idx_source (source_tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐首年消费分成（模拟订单）';
