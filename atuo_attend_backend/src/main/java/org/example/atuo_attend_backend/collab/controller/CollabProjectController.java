package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.auth.CollabAccessContext;
import org.example.atuo_attend_backend.collab.service.CollabJwtService;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectMember;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collab/projects")
public class CollabProjectController {

    private final CollabProjectService projectService;
    private final BizProjectMemberMapper memberMapper;
    private final BizUserMapper userMapper;

    public CollabProjectController(CollabProjectService projectService,
                                   BizProjectMemberMapper memberMapper,
                                   BizUserMapper userMapper) {
        this.projectService = projectService;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest req) {
        CollabAccessContext ctx = CollabAccessContext.from(req);
        List<BizProject> list = projectService.listProjectsForAccess(ctx);
        BizUser sessionUser = userMapper.findById(ctx.getSessionUserId());
        Long homeTid = sessionUser != null ? sessionUser.getTenantId() : null;
        String scope = ctx.getProjectScope();
        List<Map<String, Object>> items = list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("tenantId", p.getTenantId());
            m.put("tenantName", projectService.tenantNameForProject(p));
            m.put("name", p.getName());
            m.put("description", p.getDescription());
            m.put("repoId", p.getRepoId());
            m.put("status", p.getStatus());
            m.put("createdAt", p.getCreatedAt());
            m.put("projectParticipation", resolveParticipation(scope, homeTid, p));
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }

    /** organization=本组织名下项目；participation=仅以成员身份参与 */
    private static String resolveParticipation(String scope, Long adminHomeTenantId, BizProject p) {
        if (scope == null || p == null || p.getTenantId() == null) {
            return "participation";
        }
        if (CollabJwtService.PROJECT_SCOPE_ADMIN_MERGED.equals(scope) && adminHomeTenantId != null) {
            return p.getTenantId().equals(adminHomeTenantId) ? "organization" : "participation";
        }
        return "participation";
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable long id, HttpServletRequest req) {
        CollabAccessContext ctx = CollabAccessContext.from(req);
        if (!projectService.canAccessProject(ctx, id)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        BizProject p = projectService.getById(id);
        if (p == null) return ApiResponse.error(40400, "项目不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("id", p.getId());
        data.put("tenantId", p.getTenantId());
        data.put("tenantName", projectService.tenantNameForProject(p));
        data.put("name", p.getName());
        data.put("description", p.getDescription());
        data.put("repoId", p.getRepoId());
        data.put("status", p.getStatus());
        data.put("createdAt", p.getCreatedAt());
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}/members")
    public ApiResponse<?> listMembers(@PathVariable long id, HttpServletRequest req) {
        CollabAccessContext ctx = CollabAccessContext.from(req);
        if (!projectService.canAccessProject(ctx, id)) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        List<BizProjectMember> members = memberMapper.listByProjectId(id);
        List<Map<String, Object>> items = members.stream().map(m -> {
            BizUser u = userMapper.findById(m.getUserId());
            Map<String, Object> m2 = new HashMap<>();
            m2.put("userId", m.getUserId());
            m2.put("role", m.getRole());
            m2.put("name", u != null ? u.getName() : null);
            m2.put("email", u != null ? u.getEmail() : null);
            return m2;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }
}
