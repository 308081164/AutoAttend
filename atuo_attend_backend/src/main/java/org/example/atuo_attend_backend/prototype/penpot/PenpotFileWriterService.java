package org.example.atuo_attend_backend.prototype.penpot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.prototype.penpot.dto.PenpotLayoutPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 将 {@link PenpotLayoutPlan} 写入已有 Penpot 文件（update-file + add-obj）。
 */
@Service
public class PenpotFileWriterService {

    private static final Logger log = LoggerFactory.getLogger(PenpotFileWriterService.class);
    private static final ObjectMapper om = new ObjectMapper();

    private final PenpotRpcClient rpc;

    public PenpotFileWriterService(PenpotRpcClient rpc) {
        this.rpc = rpc;
    }

    public void applyLayout(String fileIdStr, PenpotLayoutPlan plan, String tenantAccessToken) {
        UUID fileId = UUID.fromString(fileIdStr);
        UUID sessionId = UUID.randomUUID();

        JsonNode file = rpc.command("get-file", Map.of("id", fileIdStr), tenantAccessToken);
        int revn = file.path("revn").asInt(0);
        int vern = file.path("vern").asInt(0);

        String pageIdStr = firstPageId(file);
        JsonNode page = rpc.command("get-page", Map.of("fileId", fileIdStr, "pageId", pageIdStr), tenantAccessToken);
        UUID rootId = PenpotConstants.PAGE_ROOT_ID;

        List<Map<String, Object>> changes = new ArrayList<>();

        double bw = plan.getBoardWidth() > 100 ? plan.getBoardWidth() : 1440;
        double bh = plan.getBoardHeight() > 100 ? plan.getBoardHeight() : 900;

        UUID boardId = UUID.randomUUID();
        Map<String, Object> boardShape = minimalFrame("Board", 40, 80, bw - 80, bh - 160);
        changes.add(addObjChange(pageIdStr, rootId, boardId, boardShape));

        UUID titleBarId = UUID.randomUUID();
        Map<String, Object> titleBar = minimalRect("Title bar", 40, 80, bw - 80, 56, "#1a73e8");
        changes.add(addObjChange(pageIdStr, boardId, titleBarId, titleBar));

        UUID titleTextId = UUID.randomUUID();
        Map<String, Object> titleText = titleTextShape(plan.getTitle() != null ? plan.getTitle() : "快原型", 56, 92, bw - 120);
        changes.add(addObjChange(pageIdStr, boardId, titleTextId, titleText));

        double y = 160;
        List<PenpotLayoutPlan.Block> blocks = plan.getBlocks() != null ? plan.getBlocks() : List.of();
        int maxBlocks = Math.min(blocks.size(), 8);
        for (int i = 0; i < maxBlocks; i++) {
            PenpotLayoutPlan.Block b = blocks.get(i);
            String label = b.getLabel() != null && !b.getLabel().isBlank() ? b.getLabel() : ("区块 " + (i + 1));
            String body = b.getBody() != null ? b.getBody() : "";
            UUID blockBg = UUID.randomUUID();
            changes.add(addObjChange(pageIdStr, boardId, blockBg, minimalRect(label, 56, y, bw - 160, 120, "#f8fafc")));
            UUID blockTitle = UUID.randomUUID();
            changes.add(addObjChange(pageIdStr, boardId, blockTitle, titleTextShape(label, 72, y + 16, bw - 200)));
            UUID blockBody = UUID.randomUUID();
            changes.add(addObjChange(pageIdStr, boardId, blockBody, bodyTextShape(body, 72, y + 52, bw - 200, 96)));
            y += 140;
            if (y > bh - 120) break;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", fileIdStr);
        body.put("sessionId", sessionId.toString());
        body.put("revn", revn);
        body.put("vern", vern);
        body.put("changes", changes);
        body.put("skipValidate", true);

        log.info("Penpot update-file payload (first change): {}", safeWriteJson(changes.get(0)));
        log.info("Penpot update-file total changes: {}", changes.size());
        rpc.command("update-file", body, tenantAccessToken);
    }

    private static String firstPageId(JsonNode file) {
        JsonNode pages = file.path("data").path("pages");
        if (pages.isArray() && pages.size() > 0) {
            return pages.get(0).asText();
        }
        throw new IllegalStateException("无法解析 Penpot 文件 pages");
    }

    private static Map<String, Object> addObjChange(String pageId, UUID frameId, UUID shapeId, Map<String, Object> obj) {
        Map<String, Object> ch = new LinkedHashMap<>();
        ch.put("type", "add-obj");
        ch.put("id", shapeId.toString());
        ch.put("pageId", pageId);
        ch.put("frameId", frameId.toString());
        ch.put("obj", obj);
        return ch;
    }

    private static String safeWriteJson(Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private static Map<String, Object> minimalFrame(String name, double x, double y, double w, double h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "frame");
        m.put("name", name);
        m.put("x", x);
        m.put("y", y);
        m.put("width", w);
        m.put("height", h);
        return m;
    }

    private static Map<String, Object> minimalRect(String name, double x, double y, double w, double h, String fillHex) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "rect");
        m.put("name", name);
        m.put("x", x);
        m.put("y", y);
        m.put("width", w);
        m.put("height", h);
        // 设置填充色，使矩形在画布中可见
        if (fillHex != null && !fillHex.isBlank()) {
            Map<String, Object> fill = new LinkedHashMap<>();
            fill.put("fillColor", fillHex);
            fill.put("fillOpacity", 1);
            m.put("fills", List.of(fill));
        }
        return m;
    }

    private static Map<String, Object> titleTextShape(String text, double x, double y, double maxW) {
        return textShape(text, x, y, maxW, 18, "#FFFFFF", 28);
    }

    private static Map<String, Object> bodyTextShape(String text, double x, double y, double maxW, double minH) {
        return textShape(text, x, y, maxW, 14, "#1f2937", minH);
    }

    private static Map<String, Object> textShape(String text, double x, double y, double maxW, double fontSize, String color, double minHeight) {
        String t = text != null ? text : "";
        if (t.length() > 500) {
            t = t.substring(0, 500) + "…";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "text");
        m.put("name", "Text");
        m.put("x", x);
        m.put("y", y);
        m.put("width", maxW);
        m.put("height", Math.max(minHeight, 24));
        m.put("content", Map.of(
                "type", "root",
                "children", List.of(Map.of(
                        "type", "paragraph-set",
                        "children", List.of(Map.of(
                                "type", "paragraph",
                                "children", List.of(Map.of(
                                        "text", t
                                ))
                        ))
                ))
        ));
        return m;
    }
}
