package org.example.atuo_attend_backend.dashboard;

import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class RepoController {

    private final CommitService commitService;

    public RepoController(CommitService commitService) {
        this.commitService = commitService;
    }

    @GetMapping("/repos")
    public ApiResponse<Map<String, Object>> listRepos() {
        List<String> repos = commitService.listRepos();
        Map<String, Object> data = new HashMap<>();
        data.put("items", repos);
        return ApiResponse.ok(data);
    }
}

