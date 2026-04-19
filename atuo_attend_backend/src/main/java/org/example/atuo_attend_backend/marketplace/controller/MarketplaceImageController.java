package org.example.atuo_attend_backend.marketplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.common.ApiResponse;
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
 * 项目信息发布：图片上传与受控读取（仅 marketplace/mp/ 前缀）。
 */
@RestController
@RequestMapping("/api/admin/marketplace")
public class MarketplaceImageController {

    private static final int PRESIGN_SECONDS = 3600;

    private final MinioService minioService;

    public MarketplaceImageController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload-image")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (request.getAttribute(AdminAuthFilter.ATTR_USER_ID) == null) {
            return ApiResponse.error(40300, "未授权");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(40000, "请选择图片文件");
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg")
                    && !lower.endsWith(".gif") && !lower.endsWith(".webp")) {
                return ApiResponse.error(40000, "仅支持 PNG/JPG/GIF/WEBP 图片");
            }
        }
        try {
            String key = minioService.uploadMarketplaceImage(name, file.getInputStream(), file.getSize());
            String url = "/api/admin/marketplace/image?key=" + java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8);
            Map<String, String> data = new HashMap<>();
            data.put("key", key);
            data.put("url", url);
            data.put("presignedUrl", minioService.generatePresignedUrl(key, PRESIGN_SECONDS));
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error(50000, "上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/image")
    public ResponseEntity<Resource> getImage(@RequestParam("key") String key) {
        if (key == null || !key.startsWith("marketplace/mp/") || key.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            InputStream stream = minioService.download(key);
            String lower = key.toLowerCase();
            MediaType mediaType = MediaType.IMAGE_PNG;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else if (lower.endsWith(".gif")) {
                mediaType = MediaType.IMAGE_GIF;
            } else if (lower.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .contentType(mediaType)
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
