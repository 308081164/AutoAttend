package org.example.atuo_attend_backend.lab.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.lab.mapper.LabFeedbackMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 增效实验室：租户提交问题反馈（文本 + 可选图片）。
 */
@RestController
@RequestMapping("/api/admin/lab/feedback")
public class AdminLabFeedbackController {

    private final LabFeedbackMapper labFeedbackMapper;
    private final MinioService minioService;

    public AdminLabFeedbackController(LabFeedbackMapper labFeedbackMapper, MinioService minioService) {
        this.labFeedbackMapper = labFeedbackMapper;
        this.minioService = minioService;
    }

    private static long tenantId(HttpServletRequest request) {
        Object v = request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        return v instanceof Long ? (Long) v : 0L;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, String> body, HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        String content = body != null && body.get("content") != null ? body.get("content").trim() : "";
        if (content.isEmpty()) {
            return ApiResponse.error(40000, "请填写反馈内容");
        }
        if (content.length() > 8000) {
            return ApiResponse.error(40000, "反馈内容过长");
        }
        String imageKey = body != null && body.get("imageKey") != null ? body.get("imageKey").trim() : null;
        if (imageKey != null && imageKey.isEmpty()) {
            imageKey = null;
        }
        if (imageKey != null && !imageKey.startsWith("feedback/")) {
            return ApiResponse.error(40000, "图片无效");
        }
        labFeedbackMapper.insert(tid, content, imageKey);
        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        return ApiResponse.ok(data);
    }

    @PostMapping("/image-upload")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(40000, "请选择图片");
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".gif") && !lower.endsWith(".webp")) {
                return ApiResponse.error(40000, "仅支持 PNG/JPG/GIF/WEBP");
            }
        }
        try {
            String key = minioService.uploadLabFeedbackImage(name, file.getInputStream(), file.getSize());
            Map<String, String> data = new HashMap<>();
            data.put("key", key);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error(50000, "上传失败: " + e.getMessage());
        }
    }

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
