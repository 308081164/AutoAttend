package org.example.atuo_attend_backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用启动时若 aa_ai_token_usage 表不存在则自动建表（兼容已有库未执行新 SQL 的部署）。
 */
@Component
@Order(1)
public class AiTokenUsageSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiTokenUsageSchemaBootstrap.class);
    private static final String TABLE_NAME = "aa_ai_token_usage";

    private final DataSource dataSource;

    public AiTokenUsageSchemaBootstrap(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (tableExists(conn)) {
                log.debug("AI token usage table {} already exists.", TABLE_NAME);
                return;
            }
            log.info("Creating AI token usage table {} ...", TABLE_NAME);
            runSchema(conn);
            log.info("AI token usage table created.");
        } catch (Exception e) {
            log.warn("AI token usage schema bootstrap failed: {}", e.getMessage());
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
        String sql = new ClassPathResource("db/schema_ai_token_usage.sql").getContentAsString(StandardCharsets.UTF_8);
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
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("--")) continue;
            sb.append(line).append("\n");
        }
        List<String> list = new ArrayList<>();
        for (String block : sb.toString().split(";")) {
            String s = block.trim();
            if (!s.isEmpty()) list.add(s + ";");
        }
        return list;
    }
}
