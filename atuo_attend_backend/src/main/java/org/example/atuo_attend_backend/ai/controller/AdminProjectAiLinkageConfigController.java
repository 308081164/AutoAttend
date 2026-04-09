package org.example.atuo_attend_backend.ai.controller;

import org.example.atuo_attend_backend.ai.domain.ProjectAiLinkageConfig;
import org.example.atuo_attend_backend.ai.mapper.ProjectAiLinkageConfigMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai-analysis/projects")
public class AdminProjectAiLinkageConfigController {
    private final ProjectAiLinkageConfigMapper mapper;

    public AdminProjectAiLinkageConfigController(ProjectAiLinkageConfigMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/{projectId}/linkage-config")
    public ApiResponse<?> getConfig(@PathVariable long projectId) {
        ProjectAiLinkageConfig cfg = mapper.findByProjectId(projectId);
        Map<String, Object> data = new HashMap<>();
        data.put("projectId", projectId);
        data.put("enabled", cfg != null && Boolean.TRUE.equals(cfg.getEnabled()));
        data.put("mode", cfg != null && cfg.getMode() != null ? cfg.getMode() : "auto");
        data.put("minConfidence", cfg != null && cfg.getMinConfidence() != null ? cfg.getMinConfidence() : "medium");
        data.put("riskTip", "AI的分析可能不准确，请仔细辨别");
        return ApiResponse.ok(data);
    }

    @PutMapping("/{projectId}/linkage-config")
    public ApiResponse<?> saveConfig(@PathVariable long projectId, @RequestBody Map<String, Object> body) {
        ProjectAiLinkageConfig cfg = new ProjectAiLinkageConfig();
        cfg.setProjectId(projectId);
        cfg.setEnabled(asBool(body != null ? body.get("enabled") : null));
        cfg.setMode(normalizeMode(asString(body != null ? body.get("mode") : null)));
        cfg.setMinConfidence(normalizeConfidence(asString(body != null ? body.get("minConfidence") : null)));
        mapper.upsert(cfg);
        return ApiResponse.ok(null);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean asBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "y".equals(s) || "yes".equals(s);
    }

    private static String normalizeMode(String s) {
        if (s == null) return "auto";
        String t = s.trim().toLowerCase(Locale.ROOT);
        if ("confirm".equals(t) || "suggest_only".equals(t) || "auto".equals(t)) return t;
        return "auto";
    }

    private static String normalizeConfidence(String s) {
        if (s == null) return "medium";
        String t = s.trim().toLowerCase(Locale.ROOT);
        if ("high".equals(t) || "medium".equals(t) || "low".equals(t)) return t;
        return "medium";
    }
}

