package org.example.atuo_attend_backend.commit;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class CommitController {

    private final CommitService commitService;

    public CommitController(CommitService commitService) {
        this.commitService = commitService;
    }

    @GetMapping("/commits")
    public ApiResponse<Map<String, Object>> listCommits(@RequestParam(value = "userId", required = false) Long userId,
                                                        @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                                        @RequestParam(value = "range", required = false) String range,
                                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                                        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        List<CommitRecord> pageItems;
        long total;
        if (repoFullName != null && !repoFullName.isBlank()) {
            pageItems = commitService.listPagedByRepo(repoFullName, page, pageSize);
            total = commitService.countByRepo(repoFullName);
        } else {
            pageItems = commitService.listPaged(page, pageSize);
            total = commitService.countAll();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", total);
        data.put("repoFullName", repoFullName);
        data.put("items", pageItems);
        return ApiResponse.ok(data);
    }

    @GetMapping("/commits/{commitSha}/diff")
    public ApiResponse<?> getDiff(@PathVariable("commitSha") String commitSha,
                                  @RequestParam(value = "repoFullName", required = false) String repoFullName,
                                  @RequestParam(value = "mode", required = false, defaultValue = "raw") String mode,
                                  @RequestParam(value = "chunk", required = false, defaultValue = "1") int chunk) {
        if (repoFullName == null || repoFullName.isBlank()) {
            return ApiResponse.error(40000, "repoFullName is required");
        }
        Optional<CommitRecord> recordOpt = commitService.findCommit(repoFullName, commitSha);
        if (recordOpt.isEmpty()) return ApiResponse.error(40400, "commit not found");
        CommitRecord record = recordOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("repoFullName", record.getRepoFullName());
        data.put("commitSha", record.getCommitSha());
        data.put("diffText", record.getDiffText());
        data.put("chunk", 1);
        data.put("chunkCount", 1);
        return ApiResponse.ok(data);
    }
}

