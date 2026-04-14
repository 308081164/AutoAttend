-- 平台官方邀请码：与租户无关（referrer_tenant_id 可为 NULL，类似全局激活码）
ALTER TABLE aa_invite_code MODIFY COLUMN referrer_tenant_id BIGINT NULL COMMENT '推荐方租户 ID；平台全局码为 NULL';
