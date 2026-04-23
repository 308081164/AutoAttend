-- Agent 附件独立于协作项目：将 project_id 改为 NULLABLE 并移除外键约束
-- Agent 会话上传的附件不需要关联协作项目，仅作为 AI 背景材料存储在 MinIO

-- 1. 移除外键约束 fk_attachment_project
ALTER TABLE biz_attachment DROP FOREIGN KEY fk_attachment_project;

-- 2. 将 project_id 改为允许 NULL（Agent 附件不需要 project_id）
ALTER TABLE biz_attachment MODIFY COLUMN project_id BIGINT NULL COMMENT '所属项目（Agent附件可为NULL）';

-- 3. 添加 agent_session_id 列，用于关联 Agent 会话附件
ALTER TABLE biz_attachment ADD COLUMN agent_session_id BIGINT NULL COMMENT '关联的Agent会话ID' AFTER record_id;

-- 4. 添加索引
ALTER TABLE biz_attachment ADD KEY idx_agent_session (agent_session_id);
