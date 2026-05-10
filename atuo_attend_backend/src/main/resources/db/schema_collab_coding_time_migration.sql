-- VSCode / 协作侧「编码时长」心跳片段（WakaTime 类 MVP）
-- 新库可单独执行本文件；已有库执行一次即可

CREATE TABLE IF NOT EXISTS biz_coding_time_heartbeat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL COMMENT '有效成员身份 user_id（含 acting）',
    client_event_id VARCHAR(80) NOT NULL COMMENT '客户端幂等键',
    heartbeat_at DATETIME(3) NOT NULL COMMENT '片段结束时间（建议 UTC）',
    duration_seconds INT NOT NULL COMMENT '本片段活跃秒数',
    language VARCHAR(64) NULL,
    file_fingerprint VARCHAR(128) NULL COMMENT '可选：路径哈希等',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_client_event_id (client_event_id),
    KEY idx_project_user_time (project_id, user_id, heartbeat_at),
    KEY idx_tenant_time (tenant_id, heartbeat_at),
    CONSTRAINT fk_cth_project FOREIGN KEY (project_id) REFERENCES biz_project (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
