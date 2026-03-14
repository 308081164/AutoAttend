package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.domain.BizRecordComment;
import org.example.atuo_attend_backend.collab.mapper.BizRecordCommentMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collab/records")
public class CollabCommentController {

    private final BizRecordCommentMapper commentMapper;
    private final BizUserMapper userMapper;
    private final CollabRecordService recordService;
    private final CollabProjectService projectService;

    public CollabCommentController(BizRecordCommentMapper commentMapper,
                                   BizUserMapper userMapper,
                                   CollabRecordService recordService,
                                   CollabProjectService projectService) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.recordService = recordService;
        this.projectService = projectService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @GetMapping("/{recordId}/comments")
    public ApiResponse<?> listComments(@PathVariable long recordId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int pageSize,
                                      HttpServletRequest req) {
        long userId = requireUserId(req);
        long projectId = recordService.getProjectIdByRecordId(recordId);
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        int offset = (page - 1) * pageSize;
        List<BizRecordComment> list = commentMapper.listByRecordId(recordId, offset, pageSize);
        long total = commentMapper.countByRecordId(recordId);
        List<Map<String, Object>> items = list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("userId", c.getUserId());
            m.put("content", c.getContent());
            m.put("createdAt", c.getCreatedAt());
            var u = userMapper.findById(c.getUserId());
            m.put("userName", u != null ? u.getName() : null);
            m.put("userEmail", u != null ? u.getEmail() : null);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    @PostMapping("/{recordId}/comments")
    public ApiResponse<?> addComment(@PathVariable long recordId,
                                    @RequestBody Map<String, String> body,
                                    HttpServletRequest req) {
        long userId = requireUserId(req);
        long projectId = recordService.getProjectIdByRecordId(recordId);
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        String content = body != null ? body.get("content") : null;
        if (content == null || content.isBlank()) {
            return ApiResponse.error(40000, "内容不能为空");
        }
        BizRecordComment comment = new BizRecordComment();
        comment.setRecordId(recordId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        commentMapper.insert(comment);
        Map<String, Object> data = new HashMap<>();
        data.put("id", comment.getId());
        data.put("createdAt", comment.getCreatedAt());
        return ApiResponse.ok(data);
    }
}
