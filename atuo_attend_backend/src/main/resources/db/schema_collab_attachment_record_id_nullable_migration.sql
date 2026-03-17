-- biz_attachment.record_id 改为允许 NULL（项目级/AI 录入上传时未绑定记录，插入 record_id=NULL）
-- 若列已为 NULL 则本语句等效无副作用；新部署使用 schema_collab_mysql.sql（已为 NULL），无需执行本文件。

ALTER TABLE biz_attachment MODIFY COLUMN record_id BIGINT NULL;
