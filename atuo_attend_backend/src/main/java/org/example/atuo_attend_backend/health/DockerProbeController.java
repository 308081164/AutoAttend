package org.example.atuo_attend_backend.health;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供 Docker / K8s 探活使用：不依赖 Actuator 子路径与聚合 health 的 HTTP 码行为。
 * 仅当 Web 层已就绪时返回 200，路径不在 /api/* 下，不受管理端鉴权过滤器影响。
 */
@RestController
public class DockerProbeController {

    @GetMapping(value = "/internal/healthz", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("ok");
    }
}
