package org.example.atuo_attend_backend.prototype.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * MVP：对 LLM 输出的 UI Spec 做“结构化校验”，保证前端渲染器有确定输入。
 */
public class UiPrototypeSpecValidator {

    private static final Set<String> ALLOWED_NODE_TYPES = Set.of(
            "Page", "Container", "Card", "Text", "Button", "Badge", "Tabs", "Panel", "Grid"
    );

    private static final Set<String> ALLOWED_SPACING_TOKENS = Set.of(
            "space-4", "space-8", "space-12", "space-16", "space-24"
    );
    private static final Set<String> ALLOWED_RADIUS_TOKENS = Set.of("r-8", "r-10", "r-12");
    private static final Set<String> ALLOWED_COLOR_TOKENS = Set.of(
            "primary", "primary-strong", "text", "text-muted", "bg", "border-muted", "success", "info", "warn"
    );
    private static final Set<String> ALLOWED_SHADOW_TOKENS = Set.of("shadow-soft", "shadow-none");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validate(JsonNode spec) {
        if (spec == null || !spec.isObject()) {
            throw new IllegalArgumentException("spec 必须是 JSON object");
        }
        ensureHas(spec, "meta");
        ensureHas(spec, "layout");
        ensureHas(spec, "nodes");
        ensureHas(spec, "interactions");

        JsonNode layout = spec.path("layout");
        String rootId = text(layout, "root", true);
        JsonNode nodes = spec.path("nodes");
        if (!nodes.isObject()) throw new IllegalArgumentException("nodes 必须是 object");
        if (!nodes.has(rootId)) throw new IllegalArgumentException("layout.root 指向的节点不存在: " + rootId);

        Set<String> nodeIds = new HashSet<>();
        nodes.fieldNames().forEachRemaining(nodeIds::add);

        // 1) 校验 nodes：type/props/style/children
        Iterator<Map.Entry<String, JsonNode>> fields = nodes.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> e = fields.next();
            String id = e.getKey();
            JsonNode node = e.getValue();
            validateNode(id, node, nodeIds);
        }

        // 2) 校验 interactions（target source 类型、params 结构）
        JsonNode interactions = spec.path("interactions");
        if (!interactions.isArray()) throw new IllegalArgumentException("interactions 必须是 array");
        Set<String> tabKeysUniverse = new HashSet<>();

        for (JsonNode it : interactions) {
            if (!it.isObject()) throw new IllegalArgumentException("interaction 必须是 object");
            String type = text(it, "type", true);
            if (!("togglePanel".equals(type) || "setTab".equals(type))) {
                throw new IllegalArgumentException("不支持的 interaction.type: " + type);
            }
            normalizeInteraction((ObjectNode) it);
            String sourceId = textFromKeys(it, List.of(
                    "sourceId", "source", "sourceNodeId", "fromId", "from", "triggerId", "trigger"
            ), true);
            String targetId = textFromKeys(it, List.of(
                    "targetId", "target", "targetNodeId", "toId", "to", "panelId", "tabsId"
            ), true);
            if (!nodeIds.contains(sourceId)) throw new IllegalArgumentException("interaction.sourceId 不存在: " + sourceId);
            if (!nodeIds.contains(targetId)) throw new IllegalArgumentException("interaction.targetId 不存在: " + targetId);

            JsonNode sourceNode = nodes.path(sourceId);
            JsonNode targetNode = nodes.path(targetId);
            String sourceType = text(sourceNode, "type", true);
            String targetType = text(targetNode, "type", true);

            JsonNode params = it.path("params");
            if (params == null || !params.isObject()) throw new IllegalArgumentException("interaction.params 必须是 object");

            if ("togglePanel".equals(type)) {
                if (!"Panel".equals(targetType)) throw new IllegalArgumentException("togglePanel 的 target 必须是 Panel");
                JsonNode openNode = params.get("open");
                if (openNode == null || !openNode.isBoolean()) throw new IllegalArgumentException("togglePanel.params.open 必须是 boolean");
            } else if ("setTab".equals(type)) {
                if (!"Tabs".equals(targetType)) throw new IllegalArgumentException("setTab 的 target 必须是 Tabs");
                String tabKey = text(params, "tabKey", true);
                JsonNode tabItems = targetNode.path("props").path("tabItems");
                if (!tabItems.isArray()) throw new IllegalArgumentException("Tabs.props.tabItems 必须是 array");
                boolean ok = false;
                for (JsonNode ti : tabItems) {
                    String k = text(ti, "key", false);
                    if (k != null && k.equals(tabKey)) ok = true;
                }
                if (!ok) throw new IllegalArgumentException("setTab.params.tabKey 不存在于 Tabs.props.tabItems: " + tabKey);
            }
        }
    }

    private void validateNode(String id, JsonNode node, Set<String> nodeIds) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException("node[" + id + "] 必须是 object");
        String type = text(node, "type", true);
        if (!ALLOWED_NODE_TYPES.contains(type)) {
            throw new IllegalArgumentException("不支持的 node.type: " + type + "（nodeId=" + id + "）");
        }

        JsonNode props = node.path("props");
        if (props != null && !props.isMissingNode() && !props.isObject()) {
            throw new IllegalArgumentException("node[" + id + "].props 必须是 object（或缺省）");
        }
        JsonNode style = node.path("style");
        if (style != null && !style.isMissingNode() && !style.isObject()) {
            throw new IllegalArgumentException("node[" + id + "].style 必须是 object（或缺省）");
        }
        JsonNode children = node.path("children");
        if (!children.isMissingNode()) {
            if (!children.isArray()) throw new IllegalArgumentException("node[" + id + "].children 必须是 array（或缺省）");
            for (JsonNode c : children) {
                String cid = c.asText(null);
                if (cid == null || cid.isBlank()) throw new IllegalArgumentException("node[" + id + "].children 里有空 id");
                if (!nodeIds.contains(cid)) throw new IllegalArgumentException("node[" + id + "].children 引用不存在节点: " + cid);
            }
        }

        // style token 校验（可选）
        if (style != null && !style.isMissingNode() && style.isObject()) {
            validateStyleTokens(style);
        }

        // type-specific 校验（MVP）
        if ("Text".equals(type)) {
            text(props, "text", true);
        } else if ("Button".equals(type)) {
            text(props, "label", true);
        } else if ("Badge".equals(type)) {
            text(props, "text", true);
        } else if ("Tabs".equals(type)) {
            JsonNode tabItems = props.path("tabItems");
            if (!tabItems.isArray() || tabItems.isEmpty()) throw new IllegalArgumentException("Tabs.props.tabItems 必须是非空 array");
            for (JsonNode ti : tabItems) {
                if (ti == null || !ti.isObject()) throw new IllegalArgumentException("Tabs.props.tabItems 元素必须是 object");
                text(ti, "key", true);
                text(ti, "label", true);
                String contentId = text(ti, "contentId", true);
                if (!nodeIds.contains(contentId)) throw new IllegalArgumentException("Tabs.tabItems.contentId 引用不存在: " + contentId);
            }
        } else if ("Panel".equals(type)) {
            JsonNode openByDefault = props.path("defaultOpen");
            if (!openByDefault.isMissingNode() && !openByDefault.isBoolean()) {
                throw new IllegalArgumentException("Panel.props.defaultOpen 必须是 boolean（或缺省）");
            }
            // Panel 内允许用 children 表示内容节点列表
        } else if ("Grid".equals(type)) {
            JsonNode cols = props.path("columns");
            if (!cols.isMissingNode() && !cols.isInt()) {
                throw new IllegalArgumentException("Grid.props.columns 必须是 int（或缺省）");
            }
        }
    }

    private void validateStyleTokens(JsonNode style) {
        // 允许字段：padding/radius/bg/border/shadow
        if (style.has("padding")) {
            String v = text(style, "padding", true);
            if (!ALLOWED_SPACING_TOKENS.contains(v)) throw new IllegalArgumentException("style.padding 不支持的 token: " + v);
        }
        if (style.has("radius")) {
            String v = text(style, "radius", true);
            if (!ALLOWED_RADIUS_TOKENS.contains(v)) throw new IllegalArgumentException("style.radius 不支持的 token: " + v);
        }
        if (style.has("bg")) {
            String v = text(style, "bg", true);
            if (!ALLOWED_COLOR_TOKENS.contains(v)) throw new IllegalArgumentException("style.bg 不支持的 token: " + v);
        }
        if (style.has("border")) {
            String v = text(style, "border", true);
            if (!ALLOWED_COLOR_TOKENS.contains(v)) throw new IllegalArgumentException("style.border 不支持的 token: " + v);
        }
        if (style.has("shadow")) {
            String v = text(style, "shadow", true);
            if (!ALLOWED_SHADOW_TOKENS.contains(v)) throw new IllegalArgumentException("style.shadow 不支持的 token: " + v);
        }
    }

    private static void ensureHas(JsonNode node, String key) {
        if (node == null || node.path(key).isMissingNode()) {
            throw new IllegalArgumentException("spec 缺少字段: " + key);
        }
    }

    private static String text(JsonNode node, String key, boolean required) {
        if (node == null || node.isMissingNode()) {
            if (required) throw new IllegalArgumentException("缺少字段: " + key);
            return null;
        }
        JsonNode v = node.path(key);
        if (v.isMissingNode() || v.isNull()) {
            if (required) throw new IllegalArgumentException("缺少字段: " + key);
            return null;
        }
        if (!v.isTextual()) {
            throw new IllegalArgumentException("字段不是 string: " + key);
        }
        String s = v.asText("");
        if (required && (s == null || s.isBlank())) throw new IllegalArgumentException("字段为空: " + key);
        return s;
    }

    private static String textFromKeys(JsonNode node, List<String> keys, boolean required) {
        for (String key : keys) {
            String v = text(node, key, false);
            if (v != null && !v.isBlank()) return v;
        }
        if (required) throw new IllegalArgumentException("缺少字段: " + keys.get(0));
        return null;
    }

    /**
     * 兼容 LLM 常见输出变体，统一收敛到标准字段：
     * - source/sourceNodeId/fromId/from/triggerId/trigger -> sourceId
     * - target/targetNodeId/toId/to/panelId/tabsId -> targetId
     * - open/tabKey 若出现在 interaction 顶层，则搬运到 params
     */
    private static void normalizeInteraction(ObjectNode it) {
        if (!it.has("sourceId")) {
            copyAliasField(it, "sourceId", List.of("source", "sourceNodeId", "fromId", "from", "triggerId", "trigger"));
        }
        if (!it.has("targetId")) {
            copyAliasField(it, "targetId", List.of("target", "targetNodeId", "toId", "to", "panelId", "tabsId"));
        }
        JsonNode paramsNode = it.get("params");
        ObjectNode params;
        if (paramsNode != null && paramsNode.isObject()) {
            params = (ObjectNode) paramsNode;
        } else {
            params = it.objectNode();
            it.set("params", params);
        }
        if (!params.has("open") && it.has("open")) {
            params.set("open", it.get("open"));
        }
        if (!params.has("tabKey") && it.has("tabKey")) {
            params.set("tabKey", it.get("tabKey"));
        }
    }

    private static void copyAliasField(ObjectNode it, String canonical, List<String> aliases) {
        for (String alias : aliases) {
            if (it.has(alias)) {
                it.set(canonical, it.get(alias));
                return;
            }
        }
    }
}

