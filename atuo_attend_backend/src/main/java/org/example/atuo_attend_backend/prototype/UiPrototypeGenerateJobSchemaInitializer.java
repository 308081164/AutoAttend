package org.example.atuo_attend_backend.prototype;

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
 * 若 aa_ui_prototype_generate_job 不存在则执行 schema_ui_prototype_generate_job.sql（与主 prototype 表分离，便于已上线库增量补齐）。
 */
@Component
@Order(Integer.MAX_VALUE - 2)
public class UiPrototypeGenerateJobSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UiPrototypeGenerateJobSchemaInitializer.class);
    private static final String TABLE_NAME = "aa_ui_prototype_generate_job";

    private final DataSource dataSource;

    public UiPrototypeGenerateJobSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (tableExists(conn)) {
                log.debug("UI prototype generate job table {} already exists, skip schema init.", TABLE_NAME);
                return;
            }
            log.info("UI prototype generate job table {} not found, executing schema_ui_prototype_generate_job.sql ...", TABLE_NAME);
            runSchema(conn);
            log.info("UI prototype generate job schema initialized successfully.");
        } catch (Exception e) {
            log.warn("UI prototype generate job schema init skipped or failed (non-fatal): {}", e.getMessage());
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
        String sql = new ClassPathResource("db/schema_ui_prototype_generate_job.sql").getContentAsString(StandardCharsets.UTF_8);
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
