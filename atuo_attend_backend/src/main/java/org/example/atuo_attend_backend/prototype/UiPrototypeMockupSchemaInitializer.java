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
 * 若 aa_ui_prototype_mockup 不存在则执行 schema_ui_prototype_mockup.sql（与 spec 表分离，便于增量补齐）。
 */
@Component
@Order(Integer.MAX_VALUE - 3)
public class UiPrototypeMockupSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UiPrototypeMockupSchemaInitializer.class);
    private static final String TABLE_NAME = "aa_ui_prototype_mockup";

    private final DataSource dataSource;

    public UiPrototypeMockupSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (tableExists(conn)) {
                log.debug("UI prototype mockup table {} already exists, skip schema init.", TABLE_NAME);
                return;
            }
            log.info("UI prototype mockup table {} not found, executing schema_ui_prototype_mockup.sql ...", TABLE_NAME);
            runSchema(conn);
            log.info("UI prototype mockup schema initialized successfully.");
        } catch (Exception e) {
            log.warn("UI prototype mockup schema init skipped or failed (non-fatal): {}", e.getMessage());
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
        String sql = new ClassPathResource("db/schema_ui_prototype_mockup.sql").getContentAsString(StandardCharsets.UTF_8);
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

