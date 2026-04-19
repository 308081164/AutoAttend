-- 增效实验室：用户问题反馈（文本 + 可选图片 MinIO key）
CREATE TABLE IF NOT EXISTS aa_lab_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  content TEXT NOT NULL COMMENT '反馈正文',
  image_key VARCHAR(512) NULL COMMENT 'MinIO 对象 key（feedback/ 前缀）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_lab_feedback_tenant (tenant_id),
  KEY idx_lab_feedback_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='增效实验室问题反馈';
