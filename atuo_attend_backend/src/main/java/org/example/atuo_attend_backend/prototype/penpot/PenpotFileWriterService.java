package org.example.atuo_attend_backend.prototype.penpot;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.atuo_attend_backend.prototype.penpot.dto.PenpotLayoutPlan;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 将 {@link PenpotLayoutPlan} 写入已有 Penpot 文件（update-file + add-obj）。
 */
@Service
public class PenpotFileWriterService {

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

    private static Map<String, Object> minimalFrame(String name, double x, double y, double w, double h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "frame");
        m.put("frameId", PenpotConstants.PAGE_ROOT_ID.toString());
        m.put("name", name);
        m.put("x", x);
        m.put("y", y);
        m.put("width", w);
        m.put("height", h);
        m.put("opacity", 1);
        m.put("hidden", false);
        m.put("blocked", false);
        m.put("fills", List.of(Map.of(
                "fillColor", "#FFFFFFFF",
                "fillOpacity", 1
        )));
        m.put("strokes", List.of());
        m.put("r1", 0);
        m.put("r2", 0);
        m.put("r3", 0);
        m.put("r4", 0);
        m.put("showContent", true);
        m.put("hideFillOnExport", false);
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
        m.put("opacity", 1);
        m.put("fills", List.of(Map.of("fillColor", fillHex, "fillOpacity", 1)));
        m.put("strokes", List.of());
        m.put("r1", 8);
        m.put("r2", 8);
        m.put("r3", 8);
        m.put("r4", 8);
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
        if (t.length() > 1200) {
            t = t.substring(0, 1200) + "…";
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "root");
        root.put("verticalAlign", "top");
        List<Map<String, Object>> paragraphs = new ArrayList<>();
        String[] lines = t.split("\n", 12);
        for (String line : lines) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("type", "paragraph");
            Map<String, Object> leaf = new LinkedHashMap<>();
            leaf.put("text", line.isEmpty() ? " " : line);
            leaf.put("fontId", "sourcesanspro");
            leaf.put("fontFamily", "sourcesanspro");
            leaf.put("fontVariantId", "regular");
            leaf.put("fontSize", fontSize);
            leaf.put("fontWeight", "400");
            leaf.put("fontStyle", "normal");
            leaf.put("fillColor", color);
            leaf.put("fillOpacity", 1);
            p.put("children", List.of(leaf));
            paragraphs.add(p);
        }
        Map<String, Object> pset = new LinkedHashMap<>();
        pset.put("type", "paragraph-set");
        pset.put("children", paragraphs);
        root.put("children", List.of(pset));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "text");
        m.put("name", "Text");
        m.put("x", x);
        m.put("y", y);
        m.put("width", maxW);
        m.put("height", Math.max(minHeight, 24));
        m.put("content", root);
        m.put("opacity", 1);
        return m;
    }
}
