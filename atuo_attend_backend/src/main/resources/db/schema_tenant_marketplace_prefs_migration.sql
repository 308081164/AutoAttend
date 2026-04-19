-- 租户级「项目信息发布」偏好：浏览开关（默认开启）、发布开关（默认关闭，仍须管理员 can_publish_project_info）

ALTER TABLE aa_tenant
    ADD COLUMN project_marketplace_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否对本租户开放项目信息（浏览列表/详情等）'
        AFTER workspace_prefs_json,
    ADD COLUMN project_marketplace_allow_publish TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本租户是否允许发布（仍须管理员账号具备发布权限位）'
        AFTER project_marketplace_enabled;
