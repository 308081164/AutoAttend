package org.example.atuo_attend_backend.platform.controller;

import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.lab.dto.LabFeedbackListItem;
import org.example.atuo_attend_backend.lab.mapper.LabFeedbackMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监测台：查看增效实验室用户反馈。
 */
@RestController
@RequestMapping("/api/platform/lab/feedback")
public class PlatformLabFeedbackController {

    private final LabFeedbackMapper labFeedbackMapper;
    private final MinioService minioService;

    public PlatformLabFeedbackController(LabFeedbackMapper labFeedbackMapper, MinioService minioService) {
        this.labFeedbackMapper = labFeedbackMapper;
        this.minioService = minioService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                                 @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        int ps = Math.min(Math.max(pageSize, 1), 100);
        int p = Math.max(page, 1);
        long total = labFeedbackMapper.countAll();
        int offset = (p - 1) * ps;
        List<LabFeedbackListItem> items = labFeedbackMapper.listPage(offset, ps);
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("page", p);
        data.put("pageSize", ps);
        return ApiResponse.ok(data);
    }

    /** 代理读取 MinIO 中的反馈附图（需平台运维会话）。 */
    @GetMapping("/image")
    public ResponseEntity<Resource> getImage(@RequestParam("key") String key) {
        if (key == null || !key.startsWith("feedback/") || key.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            InputStream stream = minioService.download(key);
            String lower = key.toLowerCase();
            MediaType mediaType = MediaType.IMAGE_PNG;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mediaType = MediaType.IMAGE_JPEG;
            else if (lower.endsWith(".gif")) mediaType = MediaType.IMAGE_GIF;
            else if (lower.endsWith(".webp")) mediaType = MediaType.parseMediaType("image/webp");
            Resource resource = new InputStreamResource(stream);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
