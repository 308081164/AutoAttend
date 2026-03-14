package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.commit.mapper.CommitMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final CommitService commitService;

    public AdminStatsController(CommitService commitService) {
        this.commitService = commitService;
    }

    /** 总览：仓库数、总提交数、开发者数 */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(
            @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        List<String> repos = commitService.listRepos();
        long repoCount = repos.size();
        long totalCommits = repoFullName != null && !repoFullName.isBlank()
                ? commitService.countByRepo(repoFullName)
                : commitService.countAll();
        long authorCount = commitService.countDistinctAuthors(repoFullName);
        Map<String, Object> data = new HashMap<>();
        data.put("repoCount", repoCount);
        data.put("totalCommits", totalCommits);
        data.put("authorCount", authorCount);
        return ApiResponse.ok(data);
    }

    /** 按日统计提交数与代码量，用于趋势图 */
    @GetMapping("/commits-by-day")
    public ApiResponse<List<Map<String, Object>>> commitsByDay(
            @RequestParam(value = "range", defaultValue = "7d") String range,
            @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        int days = "30d".equalsIgnoreCase(range) ? 30 : 7;
        List<CommitMapper.CommitByDay> list = commitService.listCommitsByDay(days, repoFullName);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String, Object>> data = list.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date", d.getDay() != null ? fmt.format(d.getDay()) : null);
            m.put("count", d.getCount());
            m.put("insertions", d.getInsertions());
            m.put("deletions", d.getDeletions());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(data);
    }

    /** 各仓库提交数，用于饼图 */
    @GetMapping("/commits-by-repo")
    public ApiResponse<List<Map<String, Object>>> commitsByRepo() {
        List<CommitMapper.RepoCount> list = commitService.listCommitsByRepo();
        List<Map<String, Object>> data = list.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("repoFullName", r.getRepoFullName());
            m.put("count", r.getCount());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(data);
    }

    /** 开发者提交排名（全库或按仓库），用于横向柱状图 */
    @GetMapping("/authors")
    public ApiResponse<List<Map<String, Object>>> authors(
            @RequestParam(value = "repoFullName", required = false) String repoFullName) {
        List<CommitMapper.AuthorAggregate> list = repoFullName != null && !repoFullName.isBlank()
                ? commitService.aggregateByAuthor(repoFullName)
                : commitService.aggregateByAuthorAll();
        List<Map<String, Object>> data = list.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("authorName", a.getAuthorName());
            m.put("authorEmail", a.getAuthorEmail());
            m.put("commitCount", a.getCommitCount());
            m.put("lastCommittedAt", a.getLastCommittedAt());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(data);
    }
}
