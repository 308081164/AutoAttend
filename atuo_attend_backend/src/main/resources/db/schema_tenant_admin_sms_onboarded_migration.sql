-- 租户管理员：是否已完成至少一次短信验证码登录（旧租户首次短信登录后与新模式对齐）
ALTER TABLE aa_tenant_admin_user ADD COLUMN sms_login_onboarded TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已完成短信登录绑定';
