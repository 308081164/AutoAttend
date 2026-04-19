-- 项目信息发布：需求图片、联系方式附件、有效期模式（长期有效）

ALTER TABLE aa_marketplace_project
    ADD COLUMN requirement_images_json TEXT NULL COMMENT 'JSON 数组：MinIO storage key，需求配图'
        AFTER description,
    ADD COLUMN contact_attachment_storage_key VARCHAR(512) NULL COMMENT '联系方式配图/二维码 MinIO key'
        AFTER contact_value_enc,
    ADD COLUMN effective_never_expires TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=长期有效（上架后 expire_time 为空）'
        AFTER expire_time;
