package org.example.atuo_attend_backend.quote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 应用启动时检查并补全报价相关表的缺失列（兼容已有数据库部署）。
 * 建表 SQL（schema_quote_mysql.sql）已包含这些列，此 Bootstrap 仅用于旧库升级。
 */
@Component
@Order(Integer.MAX_VALUE - 5)
public class QuoteKindSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuoteKindSchemaBootstrap.class);

    private final DataSource dataSource;

    public QuoteKindSchemaBootstrap(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();

            // ---- biz_quote_project 补全列 ----
            ensureColumn(conn, catalog, "biz_quote_project", "quote_kind",
                    "ALTER TABLE biz_quote_project ADD COLUMN quote_kind VARCHAR(32) NOT NULL DEFAULT 'single' COMMENT 'single=单体项目报价 solution=解决方案级报价' AFTER quote_subject_mode",
                    "UPDATE biz_quote_project SET quote_kind = 'single' WHERE quote_kind IS NULL OR TRIM(quote_kind) = ''");

            ensureColumn(conn, catalog, "biz_quote_project", "github_repo_full_name",
                    "ALTER TABLE biz_quote_project ADD COLUMN github_repo_full_name VARCHAR(512) NULL COMMENT 'GitHub 仓库全名'", null);
            ensureColumn(conn, catalog, "biz_quote_project", "github_repo_html_url",
                    "ALTER TABLE biz_quote_project ADD COLUMN github_repo_html_url VARCHAR(512) NULL COMMENT 'GitHub 仓库 HTML URL'", null);
            ensureColumn(conn, catalog, "biz_quote_project", "github_webhook_id",
                    "ALTER TABLE biz_quote_project ADD COLUMN github_webhook_id BIGINT NULL COMMENT 'GitHub Webhook ID'", null);
            ensureColumn(conn, catalog, "biz_quote_project", "github_webhook_secret",
                    "ALTER TABLE biz_quote_project ADD COLUMN github_webhook_secret VARCHAR(255) NULL COMMENT 'GitHub Webhook Secret'", null);
            ensureColumn(conn, catalog, "biz_quote_project", "provision_status",
                    "ALTER TABLE biz_quote_project ADD COLUMN provision_status VARCHAR(32) NULL COMMENT 'provisioning|done|failed'", null);
            ensureColumn(conn, catalog, "biz_quote_project", "provision_last_error",
                    "ALTER TABLE biz_quote_project ADD COLUMN provision_last_error TEXT NULL COMMENT 'provision 最后错误信息'", null);
            ensureColumn(conn, catalog, "biz_quote_project", "provision_synced_to_collab",
                    "ALTER TABLE biz_quote_project ADD COLUMN provision_synced_to_collab TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已同步到多维表'", null);
            ensureColumn(conn, catalog, "biz_quote_project", "provision_synced_at",
                    "ALTER TABLE biz_quote_project ADD COLUMN provision_synced_at DATETIME NULL COMMENT '同步时间'", null);

            // ---- biz_quote_module 补全列 ----
            ensureColumn(conn, catalog, "biz_quote_module", "deliverable_key",
                    "ALTER TABLE biz_quote_module ADD COLUMN deliverable_key VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '关联产出物标识'",
                    null);
            ensureColumn(conn, catalog, "biz_quote_module", "deliverable_label",
                    "ALTER TABLE biz_quote_module ADD COLUMN deliverable_label VARCHAR(255) NULL COMMENT '产出物显示名称'", null);
            ensureColumn(conn, catalog, "biz_quote_module", "tech_stack",
                    "ALTER TABLE biz_quote_module ADD COLUMN tech_stack VARCHAR(64) NULL COMMENT '可选；为空则使用项目级 tech_stack 做人天基准'", null);

            // ---- biz_quote_item 补全列 ----
            ensureColumn(conn, catalog, "biz_quote_item", "excluded_from_scale",
                    "ALTER TABLE biz_quote_item ADD COLUMN excluded_from_scale TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否排除在规模系数外'", null);
            ensureColumn(conn, catalog, "biz_quote_item", "line_price_snap",
                    "ALTER TABLE biz_quote_item ADD COLUMN line_price_snap DECIMAL(14,2) NULL COMMENT '计算报价时的行金额快照'", null);
            ensureColumn(conn, catalog, "biz_quote_item", "line_price_adjusted",
                    "ALTER TABLE biz_quote_item ADD COLUMN line_price_adjusted DECIMAL(14,2) NULL COMMENT '商务调价后的行金额'", null);

        } catch (Exception e) {
            log.error("QuoteKind schema bootstrap failed: {}", e.getMessage(), e);
        }
    }

    private void ensureColumn(Connection conn, String catalog, String table, String column,
                               String addSql, String fixSql) {
        try {
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ? LIMIT 1")) {
                ps.setString(1, catalog);
                ps.setString(2, table);
                ps.setString(3, column);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (exists) {
                log.debug("{}.{} already exists, skip.", table, column);
                return;
            }
            log.info("{}.{} not found, adding...", table, column);
            try (Statement st = conn.createStatement()) {
                st.execute(addSql);
            }
            if (fixSql != null) {
                try (Statement st = conn.createStatement()) {
                    st.execute(fixSql);
                }
            }
            log.info("{}.{} added successfully.", table, column);
        } catch (Exception e) {
            log.error("Failed to add column {}.{}: {}", table, column, e.getMessage());
        }
    }
}
