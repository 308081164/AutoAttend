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

    @Bean(name = "systemConfigSchemaBootstrap")
    public Object ensureSystemConfigTable(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (tableExists(conn)) {
                log.debug("System config table {} already exists.", TABLE_NAME);
                return new Object();
            }
            log.info("Creating system config table {} ...", TABLE_NAME);
            runSchema(conn);
            log.info("System config table created.");
        } catch (Exception e) {
            log.warn("System config schema bootstrap failed: {}", e.getMessage());
        }
        return new Object();
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
