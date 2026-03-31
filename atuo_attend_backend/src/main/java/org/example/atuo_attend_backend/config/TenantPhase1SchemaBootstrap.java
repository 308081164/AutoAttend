package org.example.atuo_attend_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 在应用接受流量与定时任务执行前，幂等补齐租户相关 DDL（与 schema_tenant_id_phase1_columns.sql / schema_phase2_phase3.sql 对齐）。
 * 用于：生产环境未挂载 migrate 目录、entrypoint 未跑迁移、或历史库漏执行脚本等情况。
 */
@Component
@DependsOn("systemConfigTableReady")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantPhase1SchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantPhase1SchemaBootstrap.class);

    private final DataSource dataSource;

    public TenantPhase1SchemaBootstrap(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            String catalog = conn.getCatalog();
            ensureAaTenantPlanAndStatus(conn, catalog);
            for (TenantIdAdd op : tenantIdAdds()) {
                if (!tableExists(conn, catalog, op.table())) {
                    continue;
                }
                if (columnExists(conn, catalog, op.table(), "tenant_id")) {
                    continue;
                }
                log.info("Bootstrap: adding tenant_id to {} ...", op.table());
                try (Statement st = conn.createStatement()) {
                    st.execute(op.ddl());
                }
            }
        }
    }

    private void ensureAaTenantPlanAndStatus(Connection conn, String catalog) throws Exception {
        if (!tableExists(conn, catalog, "aa_tenant")) {
            return;
        }
        if (!columnExists(conn, catalog, "aa_tenant", "plan_code")) {
            log.info("Bootstrap: adding aa_tenant.plan_code ...");
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE aa_tenant ADD COLUMN plan_code VARCHAR(32) NOT NULL DEFAULT 'free' "
                        + "COMMENT '套餐档位 free|team|pro'");
            }
        }
        if (!columnExists(conn, catalog, "aa_tenant", "status")) {
            log.info("Bootstrap: adding aa_tenant.status ...");
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE aa_tenant ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'active' "
                        + "COMMENT 'active|suspended'");
            }
        }
    }

    private static List<TenantIdAdd> tenantIdAdds() {
        String col = "BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID'";
        List<TenantIdAdd> list = new ArrayList<>();
        list.add(new TenantIdAdd("aa_commit", "ALTER TABLE aa_commit ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_commit_diff", "ALTER TABLE aa_commit_diff ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_webhook_delivery", "ALTER TABLE aa_webhook_delivery ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_system_config", "ALTER TABLE aa_system_config ADD COLUMN tenant_id " + col + " FIRST"));
        list.add(new TenantIdAdd("biz_user", "ALTER TABLE biz_user ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_project", "ALTER TABLE biz_project ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_project_member", "ALTER TABLE biz_project_member ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_project_table", "ALTER TABLE biz_project_table ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_option_group", "ALTER TABLE biz_option_group ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_table_column", "ALTER TABLE biz_table_column ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_record", "ALTER TABLE biz_record ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_record_field", "ALTER TABLE biz_record_field ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_record_comment", "ALTER TABLE biz_record_comment ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_attachment", "ALTER TABLE biz_attachment ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_record_history", "ALTER TABLE biz_record_history ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_project", "ALTER TABLE biz_quote_project ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_module", "ALTER TABLE biz_quote_module ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_item", "ALTER TABLE biz_quote_item ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_baseline", "ALTER TABLE biz_quote_baseline ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_risk_config", "ALTER TABLE biz_quote_risk_config ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_price_config", "ALTER TABLE biz_quote_price_config ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_result", "ALTER TABLE biz_quote_result ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_document", "ALTER TABLE biz_quote_document ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_contract_draft", "ALTER TABLE biz_quote_contract_draft ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_actual", "ALTER TABLE biz_quote_actual ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("biz_quote_preset_item", "ALTER TABLE biz_quote_preset_item ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_ai_analysis_config", "ALTER TABLE aa_ai_analysis_config ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_ai_analysis_job", "ALTER TABLE aa_ai_analysis_job ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_ai_analysis_result", "ALTER TABLE aa_ai_analysis_result ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_ai_token_usage", "ALTER TABLE aa_ai_token_usage ADD COLUMN tenant_id " + col + " AFTER id"));
        list.add(new TenantIdAdd("aa_project_daily_summary", "ALTER TABLE aa_project_daily_summary ADD COLUMN tenant_id " + col + " AFTER id"));
        return list;
    }

    private static boolean tableExists(Connection conn, String catalog, String table) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ? LIMIT 1")) {
            ps.setString(1, catalog);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection conn, String catalog, String table, String column) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_schema = ? AND table_name = ? AND column_name = ? LIMIT 1")) {
            ps.setString(1, catalog);
            ps.setString(2, table);
            ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private record TenantIdAdd(String table, String ddl) {}
}
