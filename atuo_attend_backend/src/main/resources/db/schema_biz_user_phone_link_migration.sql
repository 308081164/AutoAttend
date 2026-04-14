-- 协作成员：手机号与管理员账号绑定（跨团队身份）
SET @db := DATABASE();

-- phone_e164: E.164 手机；未绑定为 NULL
SET @x := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'biz_user' AND column_name = 'phone_e164');
SET @s := IF(@x = 0, 'ALTER TABLE biz_user ADD COLUMN phone_e164 VARCHAR(32) NULL COMMENT ''E.164 手机号；未填写表示未绑定''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_user' AND index_name = 'idx_biz_user_phone_e164');
SET @s := IF(@x = 0, 'CREATE INDEX idx_biz_user_phone_e164 ON biz_user(phone_e164)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- linked_tenant_admin_user_id: 与 aa_tenant_admin_user 绑定（同手机号自动关联）
SET @y := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'biz_user' AND column_name = 'linked_tenant_admin_user_id');
SET @s2 := IF(@y = 0, 'ALTER TABLE biz_user ADD COLUMN linked_tenant_admin_user_id BIGINT NULL COMMENT ''绑定的租户管理员 aa_tenant_admin_user.id''', 'SELECT 1');
PREPARE stmt FROM @s2; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @z := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_user' AND index_name = 'idx_biz_user_linked_admin');
SET @s3 := IF(@z = 0, 'CREATE INDEX idx_biz_user_linked_admin ON biz_user(linked_tenant_admin_user_id)', 'SELECT 1');
PREPARE stmt FROM @s3; EXECUTE stmt; DEALLOCATE PREPARE stmt;
