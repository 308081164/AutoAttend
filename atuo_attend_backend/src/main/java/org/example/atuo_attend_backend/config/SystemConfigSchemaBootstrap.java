package org.example.atuo_attend_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 在创建任何依赖 aa_system_config 的 Bean 之前先确保表存在，避免 RestTemplateConfig 等启动时报错。
 */
@Configuration
public class SystemConfigSchemaBootstrap {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigSchemaBootstrap.class);
    private static final String TABLE_NAME = "aa_system_config";

    @Bean(name = "systemConfigTableReady")
    public Object ensureSystemConfigTable(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (!tableExists(conn)) {
                log.info("Creating system config table {} ...", TABLE_NAME);
                runSchema(conn);
                log.info("System config table created.");
            } else {
                log.debug("System config table {} already exists.", TABLE_NAME);
            }
            // 已有库可能仅有 config_key 主键、无 tenant_id；Mapper 依赖 tenant_id，必须在任意依赖配置的 Bean 之前补齐
            ensureTenantIdColumnAndPk(conn);
        } catch (Exception e) {
            log.error("System config schema bootstrap failed", e);
            throw new IllegalStateException("aa_system_config bootstrap failed", e);
        }
        return new Object();
    }

    private void ensureTenantIdColumnAndPk(Connection conn) throws Exception {
        if (!tableExists(conn)) {
            return;
        }
        String catalog = conn.getCatalog();
        boolean hasTenantId = columnExists(conn, catalog, TABLE_NAME, "tenant_id");
        if (!hasTenantId) {
            log.info("Migrating {}: adding tenant_id ...", TABLE_NAME);
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + TABLE_NAME
                        + " ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' FIRST");
            }
        }
        if (!primaryKeyIncludesColumn(conn, catalog, TABLE_NAME, "tenant_id")) {
            log.info("Migrating {}: PRIMARY KEY (tenant_id, config_key) ...", TABLE_NAME);
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + TABLE_NAME + " DROP PRIMARY KEY, ADD PRIMARY KEY (tenant_id, config_key)");
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

    private static boolean primaryKeyIncludesColumn(Connection conn, String catalog, String table, String column)
            throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema = ? AND table_name = ? "
                        + "AND constraint_name = 'PRIMARY' AND column_name = ?")) {
            ps.setString(1, catalog);
            ps.setString(2, table);
            ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean tableExists(Connection conn) throws Exception {
        String catalog = conn.getCatalog();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ? LIMIT 1")) {
            ps.setString(1, catalog);
            ps.setString(2, TABLE_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void runSchema(Connection conn) throws Exception {
        String sql = new ClassPathResource("db/schema_system_config_migration.sql").getContentAsString(StandardCharsets.UTF_8);
        List<String> statements = parseStatements(sql);
        try (Statement st = conn.createStatement()) {
            for (String s : statements) {
                String trimmed = s.trim();
                if (trimmed.isEmpty()) continue;
                st.execute(trimmed);
            }
        }
    }

    private static List<String> parseStatements(String sql) {
        StringBuilder withoutComments = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            withoutComments.append(line).append("\n");
        }
        List<String> list = new ArrayList<>();
        for (String block : withoutComments.toString().split(";")) {
            String s = block.trim();
            if (!s.isEmpty()) list.add(s + ";");
        }
        return list;
    }
}
