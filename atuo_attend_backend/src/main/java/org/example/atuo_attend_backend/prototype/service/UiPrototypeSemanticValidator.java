package org.example.atuo_attend_backend.prototype.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 对 LLM 输出 Spec 做“语义级”的最小校验：
 * - 至少有一个 Tabs
 * - Tabs.tabItems 数量满足期望模块数（仅当入参能解析出模块标记时）
 * - 每个 tab 对应的 contentId 子树中至少包含一个 Badge/Text（避免空壳 Tab）
 */
public class UiPrototypeSemanticValidator {

    private static final Pattern MODULE_LABEL_MARKER = Pattern.compile("【模块\\d+】");

    public void validate(JsonNode spec, int expectedModuleCount) {
        if (spec == null || !spec.isObject()) {
            throw new IllegalArgumentException("spec 必须是 JSON object");
        }
        JsonNode nodes = spec.path("nodes");
        if (!nodes.isObject()) {
            throw new IllegalArgumentException("spec.nodes 必须是 object");
        }

        final boolean hasExplicitModules = expectedModuleCount > 0;

        String selectedTabsId = null;
        JsonNode selectedTabsNode = null;
        JsonNode firstTabsNode = null;
        int selectedTabCount = -1;
        Iterator<String> it = nodes.fieldNames();
        while (it.hasNext()) {
            String nodeId = it.next();
            JsonNode node = nodes.path(nodeId);
            if (node != null && node.isObject() && "Tabs".equals(node.path("type").asText(null))) {
                if (firstTabsNode == null) {
                    firstTabsNode = node;
                }

                JsonNode tabItems = node.path("props").path("tabItems");
                if (!tabItems.isArray() || tabItems.isEmpty()) continue;
                int tabCount = tabItems.size();
                if (hasExplicitModules) {
                    boolean labelLooksLikeModule = false;
                    for (JsonNode ti : tabItems) {
                        if (ti == null || !ti.isObject()) continue;
                        String label = ti.path("label").asText(null);
                        if (label != null && MODULE_LABEL_MARKER.matcher(label).find()) {
                            labelLooksLikeModule = true;
                            break;
                        }
                    }
                    // 有模块标记时，优先选择“像模块 Tabs”的那个（且不要超过期望模块数）
                    if (labelLooksLikeModule && tabCount >= 1 && tabCount <= expectedModuleCount) {
                        if (tabCount > selectedTabCount) {
                            selectedTabsId = nodeId;
                            selectedTabsNode = node;
                            selectedTabCount = tabCount;
                        }
                    }
                } else {
                    // 没有模块标记时，选择 tabItems 最多的 Tabs 作为“主 Tabs”（更利于预览）
                    if (tabCount > selectedTabCount) {
                        selectedTabsId = nodeId;
                        selectedTabsNode = node;
                        selectedTabCount = tabCount;
                    }
                }
            }
        }

        if (firstTabsNode == null) {
            throw new IllegalArgumentException("语义校验失败：未找到 Tabs 节点");
        }

        JsonNode tabItems = (selectedTabsNode != null ? selectedTabsNode : firstTabsNode).path("props").path("tabItems");
        if (!tabItems.isArray() || tabItems.isEmpty()) {
            throw new IllegalArgumentException("语义校验失败：Tabs.props.tabItems 必须非空 array");
        }

        int tabCount = tabItems.size();
        if (hasExplicitModules && tabCount > expectedModuleCount) {
            throw new IllegalArgumentException("语义校验失败：Tabs.tabItems 数量不能超过期望模块数，expectedMax=" + expectedModuleCount + ", actual=" + tabCount);
        }

        for (JsonNode ti : tabItems) {
            if (ti == null || !ti.isObject()) continue;
            String label = ti.path("label").asText(null);
            String contentId = ti.path("contentId").asText(null);
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("语义校验失败：Tabs.tabItems.label 不能为空");
            }
            if (contentId == null || contentId.isBlank()) {
                throw new IllegalArgumentException("语义校验失败：Tabs.tabItems.contentId 不能为空");
            }
            if (hasExplicitModules && !MODULE_LABEL_MARKER.matcher(label).find()) {
                // 仅当需求明确提供模块标记时，才要求 label 带上【模块N】
                throw new IllegalArgumentException("语义校验失败：Tabs.tabItems.label 未包含模块标记【模块N】；label=" + label);
            }
            if (!subtreeHasBadgeOrText(nodes, contentId, new HashSet<>())) {
                throw new IllegalArgumentException("语义校验失败：模块 Tab 内容未找到 Badge/Text，contentId=" + contentId);
            }
        }
    }

    private boolean subtreeHasBadgeOrText(JsonNode nodes, String nodeId, Set<String> visited) {
        if (nodeId == null || nodeId.isBlank()) return false;
        if (!visited.add(nodeId)) return false; // 防止循环引用导致递归爆栈

        JsonNode node = nodes.path(nodeId);
        if (node == null || node.isMissingNode()) return false;

        String type = node.path("type").asText(null);
        if ("Badge".equals(type) || "Text".equals(type)) return true;

        JsonNode children = node.path("children");
        if (!children.isArray()) return false;
        for (JsonNode c : children) {
            String cid = c.asText(null);
            if (subtreeHasBadgeOrText(nodes, cid, visited)) return true;
        }
        return false;
    }
}

