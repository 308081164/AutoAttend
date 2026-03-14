package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.domain.BizAttachment;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collab")
public class CollabAttachmentController {

    private final BizAttachmentMapper attachmentMapper;
    private final MinioService minioService;
    private final CollabRecordService recordService;
    private final CollabProjectService projectService;

    public CollabAttachmentController(BizAttachmentMapper attachmentMapper,
                                     MinioService minioService,
                                     CollabRecordService recordService,
                                     CollabProjectService projectService) {
        this.attachmentMapper = attachmentMapper;
        this.minioService = minioService;
        this.recordService = recordService;
        this.projectService = projectService;
    }

    private long requireUserId(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("collabUserId");
        if (id == null) throw new IllegalStateException("unauthorized");
        return id;
    }

    @GetMapping("/records/{recordId}/attachments")
    public ApiResponse<?> listAttachments(@PathVariable long recordId, HttpServletRequest req) {
        long userId = requireUserId(req);
        long projectId = recordService.getProjectIdByRecordId(recordId);
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        List<BizAttachment> list = attachmentMapper.listByRecordId(recordId);
        List<Map<String, Object>> items = list.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("fileName", a.getFileName());
            m.put("fileSize", a.getFileSize());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }

    @PostMapping("/records/{recordId}/attachments")
    public ApiResponse<?> uploadAttachment(@PathVariable long recordId,
                                           @RequestParam("file") MultipartFile file,
                                           HttpServletRequest req) {
        long userId = requireUserId(req);
        long projectId = recordService.getProjectIdByRecordId(recordId);
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(40000, "请选择文件");
        }
        try {
            String key = minioService.upload(projectId, recordId, file.getOriginalFilename(), file.getInputStream(), file.getSize());
            BizAttachment att = new BizAttachment();
            att.setRecordId(recordId);
            att.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
            att.setFileSize(file.getSize());
            att.setStorageKey(key);
            att.setUploadedBy(userId);
            attachmentMapper.insert(att);
            Map<String, Object> data = new HashMap<>();
            data.put("id", att.getId());
            data.put("fileName", att.getFileName());
            data.put("createdAt", att.getCreatedAt());
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error(50000, "上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<InputStream> download(@PathVariable long id, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("collabUserId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        BizAttachment att = attachmentMapper.findById(id);
        if (att == null) return ResponseEntity.notFound().build();
        long projectId = recordService.getProjectIdByRecordId(att.getRecordId());
        if (projectId < 0 || !projectService.canAccessProject(userId, projectId)) {
            return ResponseEntity.status(403).build();
        }
        try {
            InputStream stream = minioService.download(att.getStorageKey());
            String filename = att.getFileName();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/attachments/{id}")
    public ApiResponse<?> deleteAttachment(@PathVariable long id, HttpServletRequest req) {
        long userId = requireUserId(req);
        BizAttachment att = attachmentMapper.findById(id);
        if (att == null) return ApiResponse.error(40400, "附件不存在");
        long projectId = recordService.getProjectIdByRecordId(att.getRecordId());
        if (!projectService.canAccessProject(userId, projectId)) {
            return ApiResponse.error(40300, "无权限访问");
        }
        try {
            minioService.delete(att.getStorageKey());
        } catch (Exception ignored) {}
        attachmentMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
