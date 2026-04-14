package org.example.atuo_attend_backend.report.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.report.domain.ReportConfig;
import org.example.atuo_attend_backend.report.mapper.ReportConfigMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局日报邮件配置（总开关、企业名称、附加信息）。
 *
 * 发信 SMTP 由平台监测台 /api/platform/settings/mail 维护（全局 tenant_id=0）。
 */
@RestController
@RequestMapping("/api/admin/report")
public class AdminReportConfigController {

    private final ReportConfigMapper reportConfigMapper;

    public AdminReportConfigController(ReportConfigMapper reportConfigMapper) {
        this.reportConfigMapper = reportConfigMapper;
    }

    @GetMapping("/config")
    public ApiResponse<?> getConfig() {
        ReportConfig cfg = reportConfigMapper.getLatest();
        if (cfg == null) {
            cfg = new ReportConfig();
            cfg.setEnabled(false);
            cfg.setCompanyName("");
            cfg.setDailyExtraMessage("");
            reportConfigMapper.insert(cfg);
            cfg = reportConfigMapper.getLatest();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", cfg != null && Boolean.TRUE.equals(cfg.getEnabled()));
        data.put("companyName", cfg != null ? cfg.getCompanyName() : "");
        data.put("dailyExtraMessage", cfg != null ? cfg.getDailyExtraMessage() : "");
        return ApiResponse.ok(data);
    }

    @PutMapping("/config")
    public ApiResponse<?> updateConfig(@RequestBody Map<String, Object> body) {
        ReportConfig cfg = reportConfigMapper.getLatest();
        if (cfg == null) {
            cfg = new ReportConfig();
            cfg.setEnabled(false);
            cfg.setCompanyName("");
            cfg.setDailyExtraMessage("");
            reportConfigMapper.insert(cfg);
            cfg = reportConfigMapper.getLatest();
        }
        if (cfg == null) return ApiResponse.error(50000, "配置初始化失败");
        Object en = body != null ? body.get("enabled") : null;
        Object cn = body != null ? body.get("companyName") : null;
        Object extra = body != null ? body.get("dailyExtraMessage") : null;
        if (en != null) cfg.setEnabled(asBool(en));
        if (cn != null) cfg.setCompanyName(String.valueOf(cn));
        if (extra != null) cfg.setDailyExtraMessage(String.valueOf(extra));
        reportConfigMapper.update(cfg);
        return ApiResponse.ok(null);
    }

    private static Boolean asBool(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        String s = String.valueOf(v).trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s) || "y".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "n".equals(s)) return false;
        return null;
    }
}

