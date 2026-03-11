package org.example.atuo_attend_backend.dashboard;

import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.commit.mapper.CommitMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    private final CommitService commitService;

    public DashboardController(CommitService commitService) {
        this.commitService = commitService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam(value = "range", required = false) String range,
                                                      @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        List<CommitRecord> all;
        if (repoFullName != null && !repoFullName.isBlank()) {
            all = commitService.listPagedByRepo(repoFullName, 1, 100);
        } else {
            all = commitService.listPaged(1, 100);
        }
        Map<String, Object> data = new HashMap<>();
        Map<String, Integer> summary = new HashMap<>();
        summary.put("activeCoding", all.size());
        summary.put("inReview", 0);
        summary.put("reviewingOthers", 0);
        summary.put("ciFixing", 0);
        summary.put("blocked", 0);
        summary.put("idle", 0);
        data.put("summary", summary);
        data.put("commits", all);
        data.put("repoFullName", repoFullName);
        if (repoFullName != null && !repoFullName.isBlank()) {
            List<CommitMapper.AuthorAggregate> authors = commitService.aggregateByAuthor(repoFullName);
            data.put("authors", authors);
        } else {
            data.put("authors", List.of());
        }
        data.put("alerts", List.of());
        data.put("range", range != null ? range : "24h");
        return ApiResponse.ok(data);
    }

    @GetMapping("/users/{userId}/status")
    public ApiResponse<Map<String, Object>> userStatus(@PathVariable("userId") Long userId,
                                                       @RequestParam(value = "range", required = false) String range) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("range", range != null ? range : "7d");
        data.put("snapshots", List.of());
        return ApiResponse.ok(data);
    }

    @GetMapping("/users/{userId}/activities")
    public ApiResponse<Map<String, Object>> userActivities(@PathVariable("userId") Long userId,
                                                           @RequestParam(value = "range", required = false) String range,
                                                           @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                                           @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("range", range != null ? range : "7d");
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("items", List.of());
        return ApiResponse.ok(data);
    }
}

