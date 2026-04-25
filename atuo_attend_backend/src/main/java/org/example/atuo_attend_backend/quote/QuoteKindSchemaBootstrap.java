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
 * 应用启动时检查 biz_quote_project 是否有 quote_kind 列，
 * 若不存在则自动添加（兼容已有数据库部署）。
 */
@Component
@Order(Integer.MAX_VALUE)
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

            // 检查 quote_kind 列是否存在
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'biz_quote_project' AND COLUMN_NAME = 'quote_kind' LIMIT 1")) {
                ps.setString(1, catalog);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                log.debug("biz_quote_project.quote_kind column already exists, skip.");
                return;
            }

            log.info("biz_quote_project.quote_kind column not found, adding...");
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE biz_quote_project ADD COLUMN quote_kind VARCHAR(32) NOT NULL DEFAULT 'single' COMMENT 'single=单体项目报价 solution=解决方案级报价' AFTER quote_subject_mode");
            }
            try (Statement st = conn.createStatement()) {
                st.execute("UPDATE biz_quote_project SET quote_kind = 'single' WHERE quote_kind IS NULL OR TRIM(quote_kind) = ''");
            }
            log.info("biz_quote_project.quote_kind column added successfully.");

            // 同时检查 biz_quote_module.tech_stack 列
            boolean moduleColExists = false;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'biz_quote_module' AND COLUMN_NAME = 'tech_stack' LIMIT 1")) {
                ps.setString(1, catalog);
                try (ResultSet rs = ps.executeQuery()) {
                    moduleColExists = rs.next();
                }
            }

            if (!moduleColExists) {
                log.info("biz_quote_module.tech_stack column not found, adding...");
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE biz_quote_module ADD COLUMN tech_stack VARCHAR(64) NULL COMMENT '可选；为空则使用项目级 tech_stack 做人天基准'");
                }
                log.info("biz_quote_module.tech_stack column added successfully.");
            }
        } catch (Exception e) {
            log.warn("QuoteKind schema bootstrap skipped or failed (non-fatal): {}", e.getMessage());
        }
    }
}
