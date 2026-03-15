package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.admin.dto.*;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectMember;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/team")
public class AdminTeamController {

    /** 预设职务（常见互联网软件公司岗位），便于筛选与统一管理 */
    private static final List<String> JOB_TITLE_PRESETS = Arrays.asList(
            "开发工程师",
            "前端工程师",
            "后端工程师",
            "移动端工程师",
            "测试工程师",
            "产品经理",
            "UI设计师",
            "项目经理",
            "运维工程师",
            "数据工程师",
            "算法工程师",
            "其他"
    );

    private final AdminTeamService teamService;

    public AdminTeamController(AdminTeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/job-titles")
    public ApiResponse<List<String>> jobTitles() {
        return ApiResponse.ok(new ArrayList<>(JOB_TITLE_PRESETS));
    }

    @GetMapping("/members")
    public ApiResponse<List<Map<String, Object>>> listMembers() {
        List<BizUser> list = teamService.listMembers();
        List<Map<String, Object>> items = list.stream().map(u -> toMemberMap(u)).collect(Collectors.toList());
        return ApiResponse.ok(items);
    }

    @GetMapping("/members/{id}")
    public ApiResponse<Map<String, Object>> getMember(@PathVariable long id) {
        BizUser u = teamService.getMember(id);
        if (u == null) return ApiResponse.error(40400, "用户不存在");
        Map<String, Object> m = toMemberMap(u);
        return ApiResponse.ok(m);
    }

    @PostMapping("/members")
    public ApiResponse<Map<String, Object>> createMember(@RequestBody CreateMemberRequest req) {
        try {
            BizUser u = teamService.createMember(req);
            return ApiResponse.ok(toMemberMap(u));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PutMapping("/members/{id}")
    public ApiResponse<Void> updateMember(@PathVariable long id, @RequestBody UpdateMemberRequest req) {
        try {
            teamService.updateMember(id, req);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PutMapping("/members/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable long id, @RequestBody ResetPasswordRequest req) {
        try {
            teamService.resetPassword(id, req.getNewPassword());
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/members/{id}/projects")
    public ApiResponse<Map<String, Object>> getMemberProjects(@PathVariable long id) {
        BizUser user = teamService.getMember(id);
        if (user == null) return ApiResponse.error(40400, "用户不存在");
        List<BizProjectMember> memberships = teamService.getMemberProjects(id);
        List<Map<String, Object>> items = new ArrayList<>();
        for (BizProjectMember m : memberships) {
            BizProject p = teamService.listAllProjects().stream().filter(proj -> proj.getId().equals(m.getProjectId())).findFirst().orElse(null);
            Map<String, Object> item = new HashMap<>();
            item.put("projectId", m.getProjectId());
            item.put("projectName", p != null ? p.getName() : null);
            item.put("repoId", p != null ? p.getRepoId() : null);
            item.put("role", m.getRole());
            item.put("source", m.getSource());
            items.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("isSuperAdmin", "super_admin".equals(user.getRole()));
        return ApiResponse.ok(data);
    }

    @PutMapping("/members/{id}/projects")
    public ApiResponse<Void> setMemberProjects(@PathVariable long id, @RequestBody SetMemberProjectsRequest req) {
        try {
            teamService.setMemberProjects(id, req.getProjects() != null ? req.getProjects() : Collections.emptyList());
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/projects")
    public ApiResponse<List<Map<String, Object>>> listProjects() {
        List<BizProject> list = teamService.listAllProjects();
        List<Map<String, Object>> items = list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("repoId", p.getRepoId());
            m.put("status", p.getStatus());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(items);
    }

    private Map<String, Object> toMemberMap(BizUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("role", u.getRole());
        m.put("avatar", u.getAvatar());
        m.put("remarkName", u.getRemarkName());
        m.put("jobTitle", u.getJobTitle());
        m.put("createdAt", u.getCreatedAt());
        m.put("updatedAt", u.getUpdatedAt());
        return m;
    }
}
