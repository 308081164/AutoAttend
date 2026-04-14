-- 平台级系统配置：tenant_id=0 存放全局 SMTP / 日报邮件调度（与租户控制台解耦）
-- 将原 default 租户（1）中的邮件与公网基址复制到 0（若目标键不存在则插入）

INSERT INTO aa_system_config (tenant_id, config_key, config_value)
SELECT 0, s.config_key, s.config_value
FROM aa_system_config s
WHERE s.tenant_id = 1
  AND s.config_key IN (
    'mail.smtp.host', 'mail.smtp.port', 'mail.smtp.username', 'mail.smtp.password',
    'mail.from.address', 'mail.from.name', 'app.public.base_url'
  )
  AND NOT EXISTS (
    SELECT 1 FROM aa_system_config z
    WHERE z.tenant_id = 0 AND z.config_key = s.config_key
  );

-- 日报邮件调度（可由监测台修改；未配置时应用内仍有默认值）
INSERT INTO aa_system_config (tenant_id, config_key, config_value)
SELECT 0, 'app.report_mail.enabled', 'true' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM aa_system_config WHERE tenant_id = 0 AND config_key = 'app.report_mail.enabled');
INSERT INTO aa_system_config (tenant_id, config_key, config_value)
SELECT 0, 'app.report_mail.cron', '0 30 4 * * *' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM aa_system_config WHERE tenant_id = 0 AND config_key = 'app.report_mail.cron');
INSERT INTO aa_system_config (tenant_id, config_key, config_value)
SELECT 0, 'app.report_mail.timezone', 'Asia/Shanghai' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM aa_system_config WHERE tenant_id = 0 AND config_key = 'app.report_mail.timezone');
