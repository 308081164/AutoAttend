package org.example.atuo_attend_backend.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.platform.auth.PlatformAuthFilter;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsAuditMapper;
import org.example.atuo_attend_backend.tenant.domain.InviteCode;
import org.example.atuo_attend_backend.tenant.referral.InviteCodeService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台官方邀请码：与租户无关，由监测台统一生成与管理。
 */
@RestController
@RequestMapping("/api/platform/invite-codes")
public class PlatformInviteCodeController {

    private final InviteCodeService inviteCodeService;
    private final PlatformOpsAuditMapper platformOpsAuditMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlatformInviteCodeController(InviteCodeService inviteCodeService,
                                        PlatformOpsAuditMapper platformOpsAuditMapper) {
        this.inviteCodeService = inviteCodeService;
        this.platformOpsAuditMapper = platformOpsAuditMapper;
    }

    private Long sessionId(HttpServletRequest request) {
        Object v = request.getAttribute(PlatformAuthFilter.ATTR_PLATFORM_SESSION_ID);
        return v instanceof Long ? (Long) v : null;
    }

    private void audit(HttpServletRequest request, String action, Map<String, Object> payload) {
        Long sid = sessionId(request);
        String json = null;
        if (payload != null) {
            try {
                json = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                json = "{}";
            }
        }
        platformOpsAuditMapper.insert(sid, "platform", action, null, json);
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<InviteCode> rows = inviteCodeService.listGlobalPlatformCodes(limit);
        List<Map<String, Object>> out = rows.stream().map(this::toRow).collect(Collectors.toList());
        return ApiResponse.ok(out);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest request) {
        LocalDateTime expiresAt = null;
        Integer maxUses = 1;
        if (body != null) {
            Object iso = body.get("expiresAt");
            if (iso != null && !String.valueOf(iso).isBlank()) {
                try {
                    expiresAt = LocalDateTime.parse(String.valueOf(iso).trim().replace(" ", "T"));
                } catch (DateTimeParseException e) {
                    return ApiResponse.error(40000, "expiresAt 格式无效，使用 ISO 日期时间");
                }
            } else if (body.get("validDays") instanceof Number n) {
                int d = n.intValue();
                if (d > 0 && d <= 3650) {
                    expiresAt = LocalDateTime.now().plusDays(d);
                }
            }
            if (body.get("maxUses") instanceof Number n) {
                maxUses = n.intValue();
            }
        }
        if (maxUses == null || maxUses < 1 || maxUses > 1_000_000) {
            return ApiResponse.error(40000, "maxUses 须为 1～1000000 的整数");
        }
        try {
            InviteCode inv = inviteCodeService.createGlobalPlatformCode(expiresAt, maxUses);
            audit(request, "platform.invite_code_global",
                    Map.of("code", inv.getCode(),
                            "expiresAt", inv.getExpiresAt() != null ? inv.getExpiresAt().toString() : "",
                            "maxUses", String.valueOf(inv.getMaxUses())));
            return ApiResponse.ok(toRow(inv));
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "生成失败");
        }
    }

    private Map<String, Object> toRow(InviteCode inv) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", inv.getId());
        m.put("code", inv.getCode());
        m.put("expiresAt", inv.getExpiresAt());
        m.put("maxUses", inv.getMaxUses());
        m.put("usedCount", inv.getUsedCount());
        m.put("disabled", inv.getDisabled());
        m.put("createdAt", inv.getCreatedAt());
        return m;
    }
}
