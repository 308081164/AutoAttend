package org.example.atuo_attend_backend.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.admin.dto.TenantAdminUserItem;
import org.example.atuo_attend_backend.admin.dto.UpdateTenantAdminUserRequest;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 当前租户下「后台登录手机号」账号（aa_tenant_admin_user）的查看与微调；需管理员登录。
 */
@RestController
@RequestMapping("/api/admin/tenant-admins")
public class AdminTenantAdminUserController {

    private final AdminTenantAdminUserService tenantAdminUserService;

    public AdminTenantAdminUserController(AdminTenantAdminUserService tenantAdminUserService) {
        this.tenantAdminUserService = tenantAdminUserService;
    }

    @GetMapping
    public ApiResponse<List<TenantAdminUserItem>> list(HttpServletRequest request) {
        long tenantId = (Long) request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        return ApiResponse.ok(tenantAdminUserService.listForTenant(tenantId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable("id") long id,
                                    @RequestBody UpdateTenantAdminUserRequest body,
                                    HttpServletRequest request) {
        long tenantId = (Long) request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        try {
            tenantAdminUserService.update(tenantId, id, body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }
}
