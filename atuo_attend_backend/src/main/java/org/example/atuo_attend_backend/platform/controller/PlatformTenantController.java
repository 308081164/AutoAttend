package org.example.atuo_attend_backend.platform.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platform")
public class PlatformTenantController {

    private final TenantMapper tenantMapper;

    public PlatformTenantController(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @GetMapping("/tenants")
    public ApiResponse<List<Tenant>> listTenants() {
        return ApiResponse.ok(tenantMapper.listAll());
    }
}
