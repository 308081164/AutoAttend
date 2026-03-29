-- 阶段 1：唯一约束与主键调整（幂等：可重复执行）。依赖 schema_tenant_id_phase1_columns.sql 已执行。

-- ========== aa_commit ==========
SET @db := DATABASE();
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_commit' AND index_name = 'uk_repo_sha');
SET @s := IF(@x > 0, 'ALTER TABLE aa_commit DROP INDEX uk_repo_sha', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_commit' AND index_name = 'uk_tenant_repo_commit');
SET @s2 := IF(@y = 0, 'ALTER TABLE aa_commit ADD UNIQUE KEY uk_tenant_repo_commit (tenant_id, repo_full_name, commit_sha)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== aa_commit_diff ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_commit_diff' AND index_name = 'uk_repo_sha');
SET @s := IF(@x > 0, 'ALTER TABLE aa_commit_diff DROP INDEX uk_repo_sha', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_commit_diff' AND index_name = 'uk_tenant_repo_commit');
SET @s2 := IF(@y = 0, 'ALTER TABLE aa_commit_diff ADD UNIQUE KEY uk_tenant_repo_commit (tenant_id, repo_full_name, commit_sha)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== aa_system_config 主键 (tenant_id, config_key) ==========
SET @has_tenant_pk := (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema = @db AND table_name = 'aa_system_config' AND constraint_name = 'PRIMARY' AND column_name = 'tenant_id');
SET @s := IF(@has_tenant_pk = 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'aa_system_config' AND column_name = 'tenant_id') > 0,
  'ALTER TABLE aa_system_config DROP PRIMARY KEY, ADD PRIMARY KEY (tenant_id, config_key)',
  'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- ========== biz_user ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_user' AND index_name = 'uk_email');
SET @s := IF(@x > 0, 'ALTER TABLE biz_user DROP INDEX uk_email', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_user' AND index_name = 'uk_tenant_email');
SET @s2 := IF(@y = 0, 'ALTER TABLE biz_user ADD UNIQUE KEY uk_tenant_email (tenant_id, email)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== biz_project ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_project' AND index_name = 'uk_repo_id');
SET @s := IF(@x > 0, 'ALTER TABLE biz_project DROP INDEX uk_repo_id', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_project' AND index_name = 'uk_tenant_repo');
SET @s2 := IF(@y = 0, 'ALTER TABLE biz_project ADD UNIQUE KEY uk_tenant_repo (tenant_id, repo_id)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== biz_quote_baseline ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_quote_baseline' AND index_name = 'uk_stack_complexity');
SET @s := IF(@x > 0, 'ALTER TABLE biz_quote_baseline DROP INDEX uk_stack_complexity', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_quote_baseline' AND index_name = 'uk_tenant_stack_complexity');
SET @s2 := IF(@y = 0, 'ALTER TABLE biz_quote_baseline ADD UNIQUE KEY uk_tenant_stack_complexity (tenant_id, tech_stack, complexity)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== biz_quote_risk_config ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_quote_risk_config' AND index_name = 'uk_risk_key');
SET @s := IF(@x > 0, 'ALTER TABLE biz_quote_risk_config DROP INDEX uk_risk_key', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_quote_risk_config' AND index_name = 'uk_tenant_risk_key');
SET @s2 := IF(@y = 0, 'ALTER TABLE biz_quote_risk_config ADD UNIQUE KEY uk_tenant_risk_key (tenant_id, risk_key)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== biz_quote_price_config ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_quote_price_config' AND index_name = 'uk_region');
SET @s := IF(@x > 0, 'ALTER TABLE biz_quote_price_config DROP INDEX uk_region', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'biz_quote_price_config' AND index_name = 'uk_tenant_region');
SET @s2 := IF(@y = 0, 'ALTER TABLE biz_quote_price_config ADD UNIQUE KEY uk_tenant_region (tenant_id, region_label)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== aa_ai_analysis_config ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_ai_analysis_config' AND index_name = 'uk_provider');
SET @s := IF(@x > 0, 'ALTER TABLE aa_ai_analysis_config DROP INDEX uk_provider', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_ai_analysis_config' AND index_name = 'uk_tenant_provider');
SET @s2 := IF(@y = 0, 'ALTER TABLE aa_ai_analysis_config ADD UNIQUE KEY uk_tenant_provider (tenant_id, provider)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== aa_ai_analysis_job ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_ai_analysis_job' AND index_name = 'uk_repo_sha');
SET @s := IF(@x > 0, 'ALTER TABLE aa_ai_analysis_job DROP INDEX uk_repo_sha', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_ai_analysis_job' AND index_name = 'uk_tenant_repo_sha');
SET @s2 := IF(@y = 0, 'ALTER TABLE aa_ai_analysis_job ADD UNIQUE KEY uk_tenant_repo_sha (tenant_id, repo_full_name, commit_sha)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== aa_ai_analysis_result ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_ai_analysis_result' AND index_name = 'uk_repo_sha');
SET @s := IF(@x > 0, 'ALTER TABLE aa_ai_analysis_result DROP INDEX uk_repo_sha', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_ai_analysis_result' AND index_name = 'uk_tenant_repo_sha');
SET @s2 := IF(@y = 0, 'ALTER TABLE aa_ai_analysis_result ADD UNIQUE KEY uk_tenant_repo_sha (tenant_id, repo_full_name, commit_sha)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

-- ========== aa_project_daily_summary ==========
SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_project_daily_summary' AND index_name = 'uk_repo_summary_date');
SET @s := IF(@x > 0, 'ALTER TABLE aa_project_daily_summary DROP INDEX uk_repo_summary_date', 'SELECT 1');
PREPARE _stmt FROM @s; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
SET @y := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'aa_project_daily_summary' AND index_name = 'uk_tenant_repo_summary_date');
SET @s2 := IF(@y = 0, 'ALTER TABLE aa_project_daily_summary ADD UNIQUE KEY uk_tenant_repo_summary_date (tenant_id, repo_full_name, summary_date)', 'SELECT 1');
PREPARE _stmt2 FROM @s2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;
