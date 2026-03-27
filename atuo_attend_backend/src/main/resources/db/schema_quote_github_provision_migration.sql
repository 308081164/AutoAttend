-- 报价项目 -> GitHub 仓库创建/同步：绑定仓库与 provision 状态
-- 幂等：列已存在时忽略（由容器迁移执行器记录 warning 并继续）

ALTER TABLE biz_quote_project
  ADD COLUMN github_repo_full_name VARCHAR(255) NULL COMMENT 'GitHub 仓库 full_name: owner/repo';

ALTER TABLE biz_quote_project
  ADD COLUMN github_repo_html_url VARCHAR(512) NULL COMMENT 'GitHub 仓库页面 URL';

ALTER TABLE biz_quote_project
  ADD COLUMN github_webhook_id BIGINT NULL COMMENT 'GitHub webhook id';

ALTER TABLE biz_quote_project
  ADD COLUMN github_webhook_secret VARCHAR(255) NULL COMMENT 'GitHub webhook secret (future signature verification)';

ALTER TABLE biz_quote_project
  ADD COLUMN provision_status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft|provisioning|done|failed';

ALTER TABLE biz_quote_project
  ADD COLUMN provision_last_error TEXT NULL COMMENT '最后一次 provision 错误信息';

ALTER TABLE biz_quote_project
  ADD COLUMN provision_synced_to_collab TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已同步需求到多维表';

ALTER TABLE biz_quote_project
  ADD COLUMN provision_synced_at DATETIME NULL COMMENT '同步到多维表时间';

