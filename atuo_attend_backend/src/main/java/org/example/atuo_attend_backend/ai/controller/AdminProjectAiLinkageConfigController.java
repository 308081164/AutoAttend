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
        data.put("automationMode", cfg != null && cfg.getAutomationMode() != null
                ? cfg.getAutomationMode() : ProjectAiLinkageConfig.AUTOMATION_ANALYSIS_ONLY);
        data.put("minConfidence", cfg != null && cfg.getMinConfidence() != null ? cfg.getMinConfidence() : "medium");
        data.put("riskTip", "AI 的分析可能不准确；验收结果仅可由管理员在多维表中手动维护。");
        return ApiResponse.ok(data);
    }

    @PutMapping("/{projectId}/linkage-config")
    public ApiResponse<?> saveConfig(@PathVariable long projectId, @RequestBody Map<String, Object> body) {
        ProjectAiLinkageConfig cfg = new ProjectAiLinkageConfig();
        cfg.setProjectId(projectId);
        cfg.setAutomationMode(normalizeAutomationMode(asString(body != null ? body.get("automationMode") : null)));
        cfg.setMinConfidence(normalizeConfidence(asString(body != null ? body.get("minConfidence") : null)));
        mapper.upsert(cfg);
        return ApiResponse.ok(null);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String normalizeAutomationMode(String s) {
        if (s == null) return ProjectAiLinkageConfig.AUTOMATION_ANALYSIS_ONLY;
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (ProjectAiLinkageConfig.AUTOMATION_AUTO_STATUS.equals(t)) return ProjectAiLinkageConfig.AUTOMATION_AUTO_STATUS;
        if (ProjectAiLinkageConfig.AUTOMATION_DISABLED.equals(t)) return ProjectAiLinkageConfig.AUTOMATION_DISABLED;
        return ProjectAiLinkageConfig.AUTOMATION_ANALYSIS_ONLY;
    }

    private static String normalizeConfidence(String s) {
        if (s == null) return "medium";
        String t = s.trim().toLowerCase(Locale.ROOT);
        if ("high".equals(t) || "medium".equals(t) || "low".equals(t)) return t;
        return "medium";
    }
}
