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

    /**
     * 构建 Penpot shape 的公共几何属性：selrect、points、transform。
     * Penpot 2.x 的 update-file RPC 会对每个 add-obj 做 schema 校验，
     * 缺少这些字段会导致 "invalid shape found after applying changes" 错误。
     */
    private static Map<String, Object> shapeGeometry(double x, double y, double w, double h) {
        Map<String, Object> g = new LinkedHashMap<>();
        // selrect: 矩形选区 {x, y, width, height}
        Map<String, Object> selrect = new LinkedHashMap<>();
        selrect.put("x", x);
        selrect.put("y", y);
        selrect.put("width", w);
        selrect.put("height", h);
        g.put("selrect", selrect);

        // points: 四个角点坐标 [[x,y],[x+w,y],[x+w,y+h],[x,y+h]]
        List<List<Double>> points = List.of(
                List.of(x, y),
                List.of(x + w, y),
                List.of(x + w, y + h),
                List.of(x, y + h)
        );
        g.put("points", points);

        // transform: 单位矩阵 [1,0,0,1,0,0]
        g.put("transform", List.of(1.0, 0.0, 0.0, 1.0, 0.0, 0.0));

        // transform-inverse: 单位矩阵
        g.put("transform-inverse", List.of(1.0, 0.0, 0.0, 1.0, 0.0, 0.0));

        return g;
    }

    /**
     * 构建 Penpot shape 的公共样式属性：fills、strokes、opacity。
     */
    private static Map<String, Object> shapeStyle(String fillHex, double opacity) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("opacity", opacity);
        s.put("strokes", List.of());
        s.put("stroke-style", "none");
        s.put("stroke-width", 0.0);
        s.put("stroke-alignment", "center");
        s.put("stroke-color", "#000000");
        s.put("stroke-opacity", 0.0);
        s.put("hide-fill-in-output", false);

        // fills
        List<Map<String, Object>> fills = new ArrayList<>();
        if (fillHex != null && !fillHex.isBlank()) {
            Map<String, Object> fill = new LinkedHashMap<>();
            fill.put("fill-color", fillHex);
            fill.put("fill-opacity", 1.0);
            fill.put("fill-color-gradient", null);
            fill.put("fill-image", null);
            fills.add(fill);
        }
        s.put("fills", fills);

        return s;
    }

    private static Map<String, Object> minimalFrame(String name, double x, double y, double w, double h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "frame");
        m.put("name", name);
        m.put("x", x);
        m.put("y", y);
        m.put("width", w);
        m.put("height", h);
        m.putAll(shapeGeometry(x, y, w, h));
        m.putAll(shapeStyle(null, 1.0));
        m.put("hide-fill-in-output", true);
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
        m.put("rx", 0.0);
        m.put("ry", 0.0);
        m.putAll(shapeGeometry(x, y, w, h));
        m.putAll(shapeStyle(fillHex, 1.0));
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
        double h = Math.max(minHeight, 24);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "text");
        m.put("name", "Text");
        m.put("x", x);
        m.put("y", y);
        m.put("width", maxW);
        m.put("height", h);
        m.putAll(shapeGeometry(x, y, maxW, h));
        m.putAll(shapeStyle(null, 1.0));
        // 文本排版属性
        m.put("grow-type", "auto-height");
        m.put("font-family", "sans-serif");
        m.put("font-size", fontSize);
        m.put("font-weight", "400");
        m.put("font-style", "normal");
        m.put("letter-spacing", 0.0);
        m.put("line-height", 1.5);
        m.put("text-align", "left");
        m.put("text-direction", "ltr");
        m.put("text-transform", null);
        m.put("text-decoration", null);
        m.put("overflow-text", "visible");
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
