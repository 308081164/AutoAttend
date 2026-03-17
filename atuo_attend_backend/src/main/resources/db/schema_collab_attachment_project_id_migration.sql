-- biz_attachment 增加 project_id 列（用于项目级附件上传 / AI 录入模式等，未绑定记录时也需归属项目）
-- 仅对已存在且缺少 project_id 的库执行；新部署使用 schema_collab_mysql.sql（已含该列），无需执行本文件。
-- 若表中已有 project_id 列，第一步 ALTER 会报 Duplicate column，可忽略并跳过本文件。

-- 1. 添加列（允许 NULL，便于先回填再改 NOT NULL）
ALTER TABLE biz_attachment ADD COLUMN project_id BIGINT NULL COMMENT '所属项目' AFTER id;

-- 2. 从 record -> table 回填 project_id
UPDATE biz_attachment a
INNER JOIN biz_record r ON a.record_id = r.id
INNER JOIN biz_project_table t ON r.table_id = t.id
SET a.project_id = t.project_id
WHERE a.project_id IS NULL;

-- 3. 无法通过 record 回填的行（如 record_id=0 的 AI 上传占位）：取第一个项目作为默认，避免 NOT NULL 约束报错
UPDATE biz_attachment SET project_id = (SELECT id FROM biz_project ORDER BY id LIMIT 1) WHERE project_id IS NULL;

-- 4. 改为 NOT NULL 并加索引、外键
ALTER TABLE biz_attachment MODIFY COLUMN project_id BIGINT NOT NULL COMMENT '所属项目';
ALTER TABLE biz_attachment ADD KEY idx_project (project_id);
ALTER TABLE biz_attachment ADD CONSTRAINT fk_attachment_project FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE CASCADE;
