package org.example.atuo_attend_backend.platform.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.platform.dto.PlatformTenantOpsRow;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsReportMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 监测后台：运维报表（需 {@code /api/platform} 会话，见 {@link org.example.atuo_attend_backend.platform.auth.PlatformAuthFilter}）。
 */
@RestController
@RequestMapping("/api/platform/ops")
public class PlatformOpsController {

    private final PlatformOpsReportMapper platformOpsReportMapper;

    public PlatformOpsController(PlatformOpsReportMapper platformOpsReportMapper) {
        this.platformOpsReportMapper = platformOpsReportMapper;
    }

    /**
     * 租户列表 + 成员数、GitHub 绑定数、提交量、日活（邮箱去重近似）、diff 存储近似。
     */
    @GetMapping("/reports/tenants")
    public ApiResponse<List<PlatformTenantOpsRow>> tenantOpsReport() {
        return ApiResponse.ok(platformOpsReportMapper.listTenantOpsRows());
    }
}
