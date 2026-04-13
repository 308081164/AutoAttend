-- 邀请码：可使用次数（官方码必填语义由业务校验；NULL=不限制）
ALTER TABLE aa_invite_code ADD COLUMN max_uses INT NULL COMMENT '最大可用次数，NULL 表示不限制（用户永久码）';
ALTER TABLE aa_invite_code ADD COLUMN used_count INT NOT NULL DEFAULT 0 COMMENT '已使用次数';
