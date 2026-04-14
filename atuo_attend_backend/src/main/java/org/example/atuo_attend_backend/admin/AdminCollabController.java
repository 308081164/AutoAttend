package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/collab")
public class AdminCollabController {

    private final CollabProjectService collabProjectService;

    public AdminCollabController(CollabProjectService collabProjectService) {
        this.collabProjectService = collabProjectService;
    }

    /** 以成员身份参与的其它团队项目（需管理员已绑定手机并完成成员侧关联） */
    @GetMapping("/external-projects")
    public ApiResponse<Map<String, Object>> externalProjects(HttpServletRequest request) {
        String phone = (String) request.getAttribute(AdminAuthFilter.ATTR_PHONE);
        long tid = TenantContext.getTenantIdOrDefault(1L);
        List<BizProject> list = collabProjectService.listExternalProjectsForAdmin(tid, phone);
        List<Map<String, Object>> items = list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("tenantId", p.getTenantId());
            m.put("tenantName", collabProjectService.tenantNameForProject(p));
            m.put("name", p.getName());
            m.put("description", p.getDescription());
            m.put("repoId", p.getRepoId());
            m.put("status", p.getStatus());
            m.put("createdAt", p.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }
}
