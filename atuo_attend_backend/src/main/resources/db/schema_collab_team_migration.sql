-- 团队管理：biz_user 增加头像、备注名、职务（仅对已存在的库执行一次；若列已存在会报错，可忽略）
-- 新部署使用 schema_collab_mysql.sql（已含这些列），无需执行本文件

ALTER TABLE biz_user ADD COLUMN avatar VARCHAR(512) NULL COMMENT '头像 URL' AFTER role;
ALTER TABLE biz_user ADD COLUMN remark_name VARCHAR(128) NULL COMMENT '备注名' AFTER avatar;
ALTER TABLE biz_user ADD COLUMN job_title VARCHAR(64) NULL DEFAULT '开发工程师' COMMENT '职务' AFTER remark_name;
