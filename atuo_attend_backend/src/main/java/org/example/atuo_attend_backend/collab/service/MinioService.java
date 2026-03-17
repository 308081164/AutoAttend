package org.example.atuo_attend_backend.collab.service;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient client;
    private final String bucket;

    public MinioService(@Value("${minio.endpoint}") String endpoint,
                        @Value("${minio.accessKey}") String accessKey,
                        @Value("${minio.secretKey}") String secretKey,
                        @Value("${minio.bucket}") String bucket) {
        this.bucket = bucket;
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO ensure bucket failed: {}", e.getMessage());
        }
    }

    /** 上传头像，key = avatars/uuid.ext，供团队管理等使用 */
    public String uploadAvatar(String originalFilename, InputStream inputStream, long size) throws Exception {
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase() : "";
        if (!ext.matches("^\\.(png|jpe?g|gif|webp)$")) ext = ".png";
        String key = "avatars/" + UUID.randomUUID() + ext;
        client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .stream(inputStream, size, -1)
                .contentType(guessContentType(originalFilename))
                .build());
        return key;
    }

    /**
     * 上传文件，key = projectId/recordId/uuid_filename
     */
    public String upload(long projectId, long recordId, String originalFilename, InputStream inputStream, long size) throws Exception {
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
        String key = projectId + "/" + recordId + "/" + UUID.randomUUID() + ext;
        client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .stream(inputStream, size, -1)
                .contentType(guessContentType(originalFilename))
                .build());
        return key;
    }

    public InputStream download(String storageKey) throws Exception {
        return client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(storageKey)
                .build());
    }

    /** 最大允许转 Base64 的图片大小（字节），避免内存过大；超出则返回 null */
    private static final int MAX_IMAGE_SIZE_FOR_BASE64 = 10 * 1024 * 1024;

    /**
     * 将 MinIO 中的图片对象下载并转为 DashScope 支持的 data URL（data:image/xxx;base64,...）。
     * 仅支持 png/jpeg/gif/webp；非图片或超大小返回 null。
     */
    public String getObjectAsImageDataUrl(String storageKey, String filename) {
        if (storageKey == null || storageKey.isBlank()) return null;
        String contentType = guessContentType(filename);
        if (!contentType.startsWith("image/")) return null;
        try (InputStream in = download(storageKey)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(256 * 1024);
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
                if (buf.size() > MAX_IMAGE_SIZE_FOR_BASE64) return null;
            }
            String b64 = Base64.getEncoder().encodeToString(buf.toByteArray());
            return "data:" + contentType + ";base64," + b64;
        } catch (Exception e) {
            log.warn("MinIO getObjectAsImageDataUrl failed: {} - {}", storageKey, e.getMessage());
            return null;
        }
    }

    public void delete(String storageKey) throws Exception {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(storageKey).build());
    }

    /**
     * 生成对象的预签名访问 URL（短期有效，单位秒）。
     */
    public String generatePresignedUrl(String storageKey, int expireSeconds) throws Exception {
        if (storageKey == null || storageKey.isBlank()) return null;
        int expiry = Math.max(60, Math.min(expireSeconds, 3600));
        return client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket)
                        .object(storageKey)
                        .expiry(expiry)
                        .method(Method.GET)
                        .build()
        );
    }

    private String guessContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
