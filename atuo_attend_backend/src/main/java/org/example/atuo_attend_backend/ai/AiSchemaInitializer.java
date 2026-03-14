package org.example.atuo_attend_backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
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
 * 应用启动时若 AI 分析表不存在则自动执行 schema_ai_mysql.sql。
 * 用于已有 MySQL 数据卷的部署（initdb.d 仅在首次初始化时执行），
 * 新部署仍由 Docker 的 docker-entrypoint-initdb.d/03_schema_ai.sql 完成建表。
 */
@Component
@Order(Integer.MAX_VALUE)
public class AiSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiSchemaInitializer.class);
    private static final String TABLE_NAME = "aa_ai_analysis_config";

    private final DataSource dataSource;

    public AiSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (tableExists(conn)) {
                log.debug("AI analysis table {} already exists, skip schema init.", TABLE_NAME);
                return;
            }
            log.info("AI analysis table {} not found, executing schema_ai_mysql.sql ...", TABLE_NAME);
            runSchema(conn);
            log.info("AI analysis schema initialized successfully.");
        } catch (Exception e) {
            log.warn("AI schema init skipped or failed (non-fatal): {}", e.getMessage());
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
        String sql = new ClassPathResource("db/schema_ai_mysql.sql").getContentAsString(StandardCharsets.UTF_8);
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
