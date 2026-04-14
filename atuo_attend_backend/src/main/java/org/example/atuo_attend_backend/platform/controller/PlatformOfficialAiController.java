package org.example.atuo_attend_backend.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.example.atuo_attend_backend.ai.mapper.TokenRedeemCodeMapper;
import org.example.atuo_attend_backend.ai.official.OfficialAiPoolProperties;
import org.example.atuo_attend_backend.ai.service.OfficialAiTokenRedeemService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.platform.auth.PlatformAuthFilter;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsAuditMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监测台：官方 API 池配置、兑换码、用量排行。
 */
@RestController
@RequestMapping("/api/platform/official-ai")
public class PlatformOfficialAiController {

    private final SystemConfigService systemConfigService;
    private final OfficialAiPoolProperties officialAiPoolProperties;
    private final OfficialAiTokenRedeemService officialAiTokenRedeemService;
    private final TokenRedeemCodeMapper tokenRedeemCodeMapper;
    private final AiTokenUsageMapper aiTokenUsageMapper;
    private final PlatformOpsAuditMapper platformOpsAuditMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlatformOfficialAiController(SystemConfigService systemConfigService,
                                        OfficialAiPoolProperties officialAiPoolProperties,
                                        OfficialAiTokenRedeemService officialAiTokenRedeemService,
                                        TokenRedeemCodeMapper tokenRedeemCodeMapper,
                                        AiTokenUsageMapper aiTokenUsageMapper,
                                        PlatformOpsAuditMapper platformOpsAuditMapper) {
        this.systemConfigService = systemConfigService;
        this.officialAiPoolProperties = officialAiPoolProperties;
        this.officialAiTokenRedeemService = officialAiTokenRedeemService;
        this.tokenRedeemCodeMapper = tokenRedeemCodeMapper;
        this.aiTokenUsageMapper = aiTokenUsageMapper;
        this.platformOpsAuditMapper = platformOpsAuditMapper;
    }

    private void audit(HttpServletRequest request, String action, Map<String, Object> payload) {
        Object sid = request.getAttribute(PlatformAuthFilter.ATTR_PLATFORM_SESSION_ID);
        Long sessionId = sid instanceof Long ? (Long) sid : null;
        String json;
        try {
            json = payload != null ? objectMapper.writeValueAsString(payload) : "{}";
        } catch (JsonProcessingException e) {
            json = "{}";
        }
        platformOpsAuditMapper.insert(sessionId, "platform", action, null, json);
    }

    @GetMapping("/pool")
    public ApiResponse<Map<String, Object>> getPool() {
        Map<String, Object> data = new HashMap<>();
        data.put("appEnabled", officialAiPoolProperties.isEnabled());
        data.put("registrationGrantCny", officialAiPoolProperties.getRegistrationGrantCny());
        data.put("deepseekInputPer1mCny", officialAiPoolProperties.getDeepseekInputPer1mCny());
        data.put("deepseekOutputPer1mCny", officialAiPoolProperties.getDeepseekOutputPer1mCny());
        data.put("qwenInputPer1mCny", officialAiPoolProperties.getQwenInputPer1mCny());
        data.put("qwenOutputPer1mCny", officialAiPoolProperties.getQwenOutputPer1mCny());
        data.put("platformPoolEnabled", systemConfigService.isOfficialAiPoolEnabled());
        data.put("deepseekApiKeyMasked", systemConfigService.getOfficialDeepseekApiKeyMasked());
        data.put("qwenApiKeyMasked", systemConfigService.getOfficialQwenApiKeyMasked());
        return ApiResponse.ok(data);
    }

    @PutMapping("/pool")
    public ApiResponse<Void> putPool(@RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        if (body == null) {
            return ApiResponse.ok(null);
        }
        if (body.get("platformPoolEnabled") instanceof Boolean b) {
            systemConfigService.setOfficialAiPoolEnabled(b);
        }
        String dsk = body.get("deepseekApiKey") != null ? String.valueOf(body.get("deepseekApiKey")) : null;
        if (dsk != null && !dsk.isBlank() && !dsk.contains("****")) {
            systemConfigService.setOfficialDeepseekApiKey(dsk);
        }
        String qw = body.get("qwenApiKey") != null ? String.valueOf(body.get("qwenApiKey")) : null;
        if (qw != null && !qw.isBlank() && !qw.contains("****")) {
            systemConfigService.setOfficialQwenApiKey(qw);
        }
        audit(request, "platform.official_ai.pool", Map.of("savedKeys", "platformPoolEnabled,optionalKeys"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/redeem-codes")
    public ApiResponse<Map<String, Object>> createRedeemCode(@RequestBody(required = false) Map<String, Object> body,
                                                             HttpServletRequest request) {
        BigDecimal grant = body != null && body.get("grantCny") != null
                ? new BigDecimal(String.valueOf(body.get("grantCny"))) : BigDecimal.valueOf(20);
        int maxUses = 1;
        if (body != null && body.get("maxUses") instanceof Number n) {
            maxUses = n.intValue();
        }
        String note = body != null && body.get("note") != null ? String.valueOf(body.get("note")) : null;
        LocalDateTime exp = null;
        if (body != null && body.get("expiresAt") != null) {
            String raw = String.valueOf(body.get("expiresAt")).trim();
            if (!raw.isEmpty()) {
                exp = OfficialAiTokenRedeemService.parseExpires(raw);
            }
        }
        try {
            Map<String, Object> created = officialAiTokenRedeemService.createCode(grant, maxUses, exp, note);
            audit(request, "platform.official_ai.redeem_code_create",
                    Map.of("grantCny", grant, "maxUses", maxUses));
            return ApiResponse.ok(created);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/redeem-codes")
    public ApiResponse<List<Map<String, Object>>> listRedeemCodes(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(tokenRedeemCodeMapper.listRecent(Math.min(Math.max(limit, 1), 200)));
    }

    @GetMapping("/usage/by-tenant")
    public ApiResponse<List<Map<String, Object>>> usageByTenant(@RequestParam(defaultValue = "30") int days,
                                                                @RequestParam(defaultValue = "50") int limit) {
        int d = Math.min(Math.max(days, 1), 365);
        LocalDateTime since = LocalDateTime.now().minusDays(d);
        return ApiResponse.ok(aiTokenUsageMapper.sumOfficialCostByTenantSince(since, Math.min(Math.max(limit, 1), 200)));
    }
}
