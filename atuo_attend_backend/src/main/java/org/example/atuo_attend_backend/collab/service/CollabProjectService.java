package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollabProjectService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String ROLE_SUB_ADMIN = "sub_admin";

    private final BizProjectMapper projectMapper;
    private final BizProjectMemberMapper memberMapper;
    private final BizUserMapper userMapper;
    private final TenantMapper tenantMapper;

    public CollabProjectService(BizProjectMapper projectMapper,
                                BizProjectMemberMapper memberMapper,
                                BizUserMapper userMapper,
                                TenantMapper tenantMapper) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
    }

    /**
     * @param projectScope {@link CollabJwtService#PROJECT_SCOPE_ALL} 或 {@link CollabJwtService#PROJECT_SCOPE_TENANT}；null 时按角色推断（兼容无 claim 的旧 JWT）
     */
    public List<BizProject> listProjectsForUser(long userId, String projectScope) {
        BizUser user = userMapper.findById(userId);
        if (user == null) return List.of();

        String scope = resolveProjectScope(projectScope, user);

        if (CollabJwtService.PROJECT_SCOPE_ALL.equals(scope)) {
            return listProjectsAcrossTenantsForNaturalPerson(user);
        }

        long tid = user.getTenantId() != null ? user.getTenantId() : TenantConstants.DEFAULT_TENANT_ID;
        if (ROLE_SUPER_ADMIN.equals(user.getRole())) {
            return projectMapper.listByTenant(tid);
        }
        if (ROLE_SUB_ADMIN.equals(user.getRole())) {
            List<Long> projectIds = memberMapper.listProjectIdsByUserIdAndRole(userId, "admin");
            return projectMapper.listByTenant(tid).stream()
                    .filter(p -> projectIds.contains(p.getId()))
                    .collect(Collectors.toList());
        }
        List<Long> myProjectIds = memberMapper.listProjectIdsByUserId(userId);
        return projectMapper.listByTenant(tid).stream()
                .filter(p -> myProjectIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    /** @deprecated 使用 {@link #listProjectsForUser(long, String)} */
    @Deprecated
    public List<BizProject> listProjectsForUser(long userId) {
        return listProjectsForUser(userId, null);
    }

    private static String resolveProjectScope(String projectScope, BizUser user) {
        if (projectScope != null && !projectScope.isBlank()) {
            return projectScope;
        }
        if (ROLE_SUPER_ADMIN.equals(user.getRole())) {
            return CollabJwtService.PROJECT_SCOPE_TENANT;
        }
        return CollabJwtService.PROJECT_SCOPE_ALL;
    }

    /**
     * 同一自然人在各租户下的 biz_user 行通过相同 email 关联；聚合全部参与项目。
     */
    private List<BizProject> listProjectsAcrossTenantsForNaturalPerson(BizUser primary) {
        List<Long> userIds = userMapper.listIdsByEmailNormalized(primary.getEmail());
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> projectIds = memberMapper.listProjectIdsByUserIds(userIds);
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniq = new LinkedHashSet<>(projectIds);
        List<Long> idList = new ArrayList<>(uniq);
        return projectMapper.listByIds(idList);
    }

    public BizProject getById(long projectId) {
        return projectMapper.findById(projectId);
    }

    public boolean canAccessProject(long userId, long projectId, String projectScope) {
        BizUser user = userMapper.findById(userId);
        if (user == null) return false;

        String scope = resolveProjectScope(projectScope, user);

        if (CollabJwtService.PROJECT_SCOPE_ALL.equals(scope)) {
            List<Long> userIds = userMapper.listIdsByEmailNormalized(user.getEmail());
            if (userIds == null || userIds.isEmpty()) return false;
            List<Long> pids = memberMapper.listProjectIdsByUserIds(userIds);
            return pids != null && pids.stream().anyMatch(id -> Objects.equals(id, projectId));
        }

        if (ROLE_SUPER_ADMIN.equals(user.getRole())) return true;
        List<Long> ids = memberMapper.listProjectIdsByUserId(userId);
        return ids.contains(projectId);
    }

    /** @deprecated 使用 {@link #canAccessProject(long, long, String)} */
    @Deprecated
    public boolean canAccessProject(long userId, long projectId) {
        return canAccessProject(userId, projectId, null);
    }

    public String tenantNameForProject(BizProject p) {
        if (p == null || p.getTenantId() == null) return "";
        String name = tenantMapper.findNameById(p.getTenantId());
        return name != null ? name : "";
    }

    /**
     * 管理员以成员身份参与的其它团队项目（不含本租户）。
     */
    public List<BizProject> listExternalProjectsForAdmin(long homeTenantId, String adminPhoneE164) {
        if (adminPhoneE164 == null || adminPhoneE164.isBlank()) {
            return List.of();
        }
        String phone = adminPhoneE164.trim();
        BizUser homeShadow = userMapper.findByTenantAndEmail(homeTenantId, phone);
        if (homeShadow == null) {
            return List.of();
        }
        List<Long> userIds = userMapper.listIdsByEmailNormalized(homeShadow.getEmail());
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> projectIds = memberMapper.listProjectIdsByUserIds(userIds);
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniq = new LinkedHashSet<>(projectIds);
        List<BizProject> all = projectMapper.listByIds(new ArrayList<>(uniq));
        return all.stream()
                .filter(p -> p.getTenantId() != null && p.getTenantId() != homeTenantId)
                .collect(Collectors.toList());
    }
}
