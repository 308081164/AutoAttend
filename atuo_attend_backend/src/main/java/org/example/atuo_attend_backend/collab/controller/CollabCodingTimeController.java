package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.auth.CollabAccessContext;
import org.example.atuo_attend_backend.collab.dto.CollabCodingHeartbeatBatchRequest;
import org.example.atuo_attend_backend.collab.service.CollabCodingTimeService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * WakaTime 类编码统计：心跳入库与按日汇总（MVP）。
 */
@RestController
@RequestMapping("/api/collab/coding-time")
public class CollabCodingTimeController {

    private final CollabCodingTimeService codingTimeService;

    public CollabCodingTimeController(CollabCodingTimeService codingTimeService) {
        this.codingTimeService = codingTimeService;
    }

    @PostMapping("/heartbeats")
    public ApiResponse<Map<String, Object>> heartbeats(@RequestBody CollabCodingHeartbeatBatchRequest body,
                                                         HttpServletRequest req) {
        CollabAccessContext ctx = CollabAccessContext.from(req);
        try {
            int accepted = codingTimeService.ingestHeartbeats(ctx, body);
            Map<String, Object> data = new HashMap<>();
            data.put("accepted", accepted);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /**
     * 按 UTC 日历日汇总当前有效用户在指定项目上的编码秒数。
     *
     * @param day ISO 日期 yyyy-MM-dd，缺省为当天（UTC）
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam long projectId,
                                                    @RequestParam(required = false) String day,
                                                    HttpServletRequest req) {
        CollabAccessContext ctx = CollabAccessContext.from(req);
        LocalDate d;
        try {
            d = day == null || day.isBlank() ? LocalDate.now(java.time.ZoneOffset.UTC) : LocalDate.parse(day);
        } catch (DateTimeParseException e) {
            return ApiResponse.error(40000, "day 格式应为 yyyy-MM-dd");
        }
        try {
            long seconds = codingTimeService.sumSecondsForUserProjectDay(ctx, projectId, d);
            Map<String, Object> data = new HashMap<>();
            data.put("projectId", projectId);
            data.put("day", d.toString());
            data.put("totalSeconds", seconds);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40300, e.getMessage());
        }
    }
}
