package org.example.atuo_attend_backend.report.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.report.domain.ProjectReportConfig;
import org.example.atuo_attend_backend.report.mapper.ProjectReportConfigMapper;
import org.example.atuo_attend_backend.report.service.ReportMailService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 项目级日报配置（协作项目管理员使用）。
 *
 * 注意：权限校验（是否可管理该协作项目）后续可接入 CollabProjectService；
 * MVP 先沿用已有 admin/collab 鉴权链路（由网关/拦截器注入身份）。
 */
@RestController
@RequestMapping("/api/admin/report/projects")
public class AdminProjectReportConfigController {

    private final ProjectReportConfigMapper mapper;
    private final ReportMailService reportMailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminProjectReportConfigController(ProjectReportConfigMapper mapper, ReportMailService reportMailService) {
        this.mapper = mapper;
        this.reportMailService = reportMailService;
    }

    @GetMapping("/{projectId}/config")
    public ApiResponse<?> getConfig(@PathVariable long projectId) {
        ProjectReportConfig cfg = mapper.findByProjectId(projectId);
        if (cfg == null) {
            cfg = new ProjectReportConfig();
            cfg.setProjectId(projectId);
            cfg.setEnabled(false);
            cfg.setSendToManagers(true);
            cfg.setSendToDevelopers(true);
            cfg.setManagerEmails("[]");
        }
        Map<String, Object> data = toDto(cfg);
        data.put("mailConfigured", reportMailService.isMailConfigured());
        return ApiResponse.ok(data);
    }

    @PutMapping("/{projectId}/config")
    public ApiResponse<?> upsert(@PathVariable long projectId, @RequestBody Map<String, Object> body) {
        ProjectReportConfig cfg = new ProjectReportConfig();
        cfg.setProjectId(projectId);
        cfg.setRepoFullName(trimToNull(asString(body.get("repoFullName"))));
        cfg.setEnabled(asBool(body.get("enabled")));
        cfg.setSendToManagers(asBool(body.get("sendToManagers")));
        cfg.setSendToDevelopers(asBool(body.get("sendToDevelopers")));
        cfg.setManagerEmails(normalizeEmailsJson(body.get("managerEmails")));
        mapper.upsert(cfg);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{projectId}/preview")
    public ApiResponse<?> preview(@PathVariable long projectId,
                                  @RequestParam(required = false) String date,
                                  @RequestParam(defaultValue = "Asia/Shanghai") String timezone) {
        LocalDate d = parseDate(date);
        ZoneId zone = parseZone(timezone);
        String html = reportMailService.previewProjectHtml(projectId, d, zone);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", d.toString());
        data.put("html", html);
        return ApiResponse.ok(data);
    }

    @PostMapping("/{projectId}/send-now")
    public ApiResponse<?> sendNow(@PathVariable long projectId,
                                  @RequestParam(required = false) String date,
                                  @RequestParam(defaultValue = "Asia/Shanghai") String timezone,
                                  @RequestParam(defaultValue = "false") boolean includeDevelopers) {
        LocalDate d = parseDate(date);
        ZoneId zone = parseZone(timezone);
        int sent = reportMailService.sendForProject(projectId, d, zone, includeDevelopers);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sent", sent);
        data.put("date", d.toString());
        return ApiResponse.ok(data);
    }

    private Map<String, Object> toDto(ProjectReportConfig cfg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("projectId", cfg.getProjectId());
        m.put("repoFullName", cfg.getRepoFullName());
        m.put("enabled", Boolean.TRUE.equals(cfg.getEnabled()));
        m.put("sendToManagers", Boolean.TRUE.equals(cfg.getSendToManagers()));
        m.put("sendToDevelopers", Boolean.TRUE.equals(cfg.getSendToDevelopers()));
        m.put("managerEmails", safeParseEmails(cfg.getManagerEmails()));
        return m;
    }

    private List<String> safeParseEmails(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            List<String> out = new ArrayList<>();
            for (String s : list) {
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
            return out;
        } catch (Exception ignore) {
            String[] parts = raw.split("[,\\n\\r\\t ]+");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                String e = p.trim();
                if (!e.isBlank()) out.add(e);
            }
            return out;
        }
    }

    private String normalizeEmailsJson(Object v) {
        if (v == null) return "[]";
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) continue;
                String s = o.toString().trim();
                if (!s.isBlank()) out.add(s);
            }
            try {
                return objectMapper.writeValueAsString(out);
            } catch (Exception e) {
                return "[]";
            }
        }
        // 允许直接传字符串（逗号/换行分隔）
        String raw = v.toString();
        String[] parts = raw.split("[,\\n\\r\\t ]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isBlank()) out.add(s);
        }
        try {
            return objectMapper.writeValueAsString(out);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean asBool(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s) || "y".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "n".equals(s)) return false;
        return null;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now().minusDays(1);
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return LocalDate.now().minusDays(1); }
    }

    private static ZoneId parseZone(String tz) {
        try { return ZoneId.of(tz); } catch (Exception e) { return ZoneId.of("Asia/Shanghai"); }
    }
}

