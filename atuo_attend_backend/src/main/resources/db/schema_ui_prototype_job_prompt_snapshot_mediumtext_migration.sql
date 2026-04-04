-- 快原型异步任务：prompt_snapshot 与 Java 端 promptSnapshot() 上限对齐（《页面设计文档》等可远超 2000 字）
-- 修复：Data truncation: Data too long for column 'prompt_snapshot'

ALTER TABLE aa_ui_prototype_mockup_generate_job
    MODIFY COLUMN prompt_snapshot MEDIUMTEXT NULL COMMENT '请求摘要（审计/排错；长 prompt 截断入库）';

ALTER TABLE aa_ui_prototype_generate_job
    MODIFY COLUMN prompt_snapshot MEDIUMTEXT NULL COMMENT '请求摘要（审计/排错；长 prompt 截断入库）';
