package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.auth.CollabAuthFilter;

import org.example.atuo_attend_backend.collab.domain.BizProjectPortalLink;
import org.example.atuo_attend_backend.collab.mapper.BizProjectPortalLinkMapper;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

/**
 * 协作项目「传送门」链接：CRUD + 从其他项目导入。
 */
@RestController
@RequestMapping("/api/collab")
public class CollabPortalLinkController {

    private final CollabProjectService projectService;
    private final BizProjectPortalLinkMapper linkMapper;

    public CollabPortalLinkController(CollabProjectService projectService, BizProjectPortalLinkMapper linkMapper) {
        this.projectService = projectService;
        this.linkMapper = linkMapper;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @GetMapping("/projects/{projectId}/portal-links")
    public ApiResponse<?> list(@PathVariable long projectId, HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        List<BizProjectPortalLink> list = linkMapper.listByProjectId(projectId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (BizProjectPortalLink l : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("label", l.getLabel());
            m.put("url", l.getUrl());
            m.put("sortOrder", l.getSortOrder());
            items.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }

    @PostMapping("/projects/{projectId}/portal-links")
    public ApiResponse<?> create(@PathVariable long projectId, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        String label = asString(body.get("label"));
        String url = asString(body.get("url"));
        Integer sortOrder = asInt(body.get("sortOrder"));
        if (label == null || label.isBlank()) return ApiResponse.error(40000, "展示文本不能为空");
        if (url == null || url.isBlank()) return ApiResponse.error(40000, "URL 不能为空");
        if (!isValidUrl(url)) return ApiResponse.error(40000, "URL 格式不合法（仅支持 http/https）");
        BizProjectPortalLink link = new BizProjectPortalLink();
        link.setProjectId(projectId);
        link.setLabel(label.trim());
        link.setUrl(url.trim());
        link.setSortOrder(sortOrder != null ? sortOrder : 0);
        linkMapper.insert(link);
        Map<String, Object> data = new HashMap<>();
        data.put("id", link.getId());
        return ApiResponse.ok(data);
    }

    @PutMapping("/projects/{projectId}/portal-links/{id}")
    public ApiResponse<?> update(@PathVariable long projectId, @PathVariable long id,
                                 @RequestBody Map<String, Object> body, HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        BizProjectPortalLink existing = linkMapper.findById(id);
        if (existing == null || existing.getProjectId() == null || existing.getProjectId() != projectId) {
            return ApiResponse.error(40400, "链接不存在");
        }
        String label = asString(body.get("label"));
        String url = asString(body.get("url"));
        Integer sortOrder = asInt(body.get("sortOrder"));
        if (label == null || label.isBlank()) return ApiResponse.error(40000, "展示文本不能为空");
        if (url == null || url.isBlank()) return ApiResponse.error(40000, "URL 不能为空");
        if (!isValidUrl(url)) return ApiResponse.error(40000, "URL 格式不合法（仅支持 http/https）");
        existing.setLabel(label.trim());
        existing.setUrl(url.trim());
        existing.setSortOrder(sortOrder != null ? sortOrder : 0);
        linkMapper.update(existing);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/projects/{projectId}/portal-links/{id}")
    public ApiResponse<?> delete(@PathVariable long projectId, @PathVariable long id, HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        BizProjectPortalLink existing = linkMapper.findById(id);
        if (existing == null || existing.getProjectId() == null || existing.getProjectId() != projectId) {
            return ApiResponse.error(40400, "链接不存在");
        }
        linkMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    /** 从其他项目导入：覆盖当前项目全部 links（简单可靠，符合“快速配齐”需求）。 */
    @PostMapping("/projects/{projectId}/portal-links/import")
    public ApiResponse<?> importFromProject(@PathVariable long projectId, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        long userId = requireUserId(req);
        if (!projectService.canAccessProject(userId, projectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问该项目");
        }
        Long fromProjectId = asLong(body.get("fromProjectId"));
        if (fromProjectId == null || fromProjectId <= 0) return ApiResponse.error(40000, "fromProjectId 缺失");
        if (!projectService.canAccessProject(userId, fromProjectId, CollabAuthFilter.projectScopeFrom(req))) {
            return ApiResponse.error(40300, "无权限访问源项目");
        }
        List<BizProjectPortalLink> src = linkMapper.listByProjectId(fromProjectId);
        linkMapper.deleteByProjectId(projectId);
        for (BizProjectPortalLink s : src) {
            if (s == null) continue;
            BizProjectPortalLink c = new BizProjectPortalLink();
            c.setProjectId(projectId);
            c.setLabel(s.getLabel());
            c.setUrl(s.getUrl());
            c.setSortOrder(s.getSortOrder() != null ? s.getSortOrder() : 0);
            linkMapper.insert(c);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("importedCount", src.size());
        return ApiResponse.ok(data);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static boolean isValidUrl(String url) {
        try {
            URI u = URI.create(url.trim());
            String s = u.getScheme();
            return "http".equalsIgnoreCase(s) || "https".equalsIgnoreCase(s);
        } catch (Exception e) {
            return false;
        }
    }
}

