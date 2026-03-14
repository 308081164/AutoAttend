package org.example.atuo_attend_backend.collab.service;

import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
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

    public void delete(String storageKey) throws Exception {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(storageKey).build());
    }

    private String guessContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
