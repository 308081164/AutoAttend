package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 阿里云 ECS / CMS 等 OpenAPI 常见要求：UTC、整秒、{@code yyyy-MM-dd'T'HH:mm:ssZ}（字面 Z）。
 * {@link DateTimeFormatter#ISO_INSTANT} 在亚秒非零时会输出小数秒，部分接口会报 StartTime 无效。
 */
public final class AliyunOpenApiTimeUtil {

    private static final DateTimeFormatter UTC_Z =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private AliyunOpenApiTimeUtil() {
    }

    public static String utcSecondZ(Instant t) {
        Instant i = t != null ? t : Instant.now();
        return UTC_Z.format(i.truncatedTo(ChronoUnit.SECONDS));
    }
}
