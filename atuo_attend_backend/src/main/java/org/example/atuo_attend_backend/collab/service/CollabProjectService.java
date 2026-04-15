package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.auth.CollabAccessContext;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMemberMapper;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollabProjectService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String ROLE_SUB_ADMIN = "sub_admin";
    private static final String ROLE_MEMBER = "member";

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
     * 推荐使用：按 JWT + 可选「当前身份」头解析后的上下文列项目。
     */
    public List<BizProject> listProjectsForAccess(CollabAccessContext ctx) {
        BizUser session = userMapper.findById(ctx.getSessionUserId());
        if (session == null) return List.of();
        String scope = resolveProjectScope(ctx.getProjectScope(), session);

        if (CollabJwtService.PROJECT_SCOPE_EMAIL.equals(scope)) {
            BizUser eff = userMapper.findById(ctx.getEffectiveUserId());
            return eff == null ? List.of() : listMemberProjectsSingleAccount(eff);
        }
        if (CollabJwtService.PROJECT_SCOPE_PHONE_MEMBERS.equals(scope)) {
            return listMemberProjectsPhoneUnion(ctx.getPhoneMemberIds(), ctx.getEffectiveUserId());
        }
        if (CollabJwtService.PROJECT_SCOPE_ADMIN_MERGED.equals(scope)) {
            if (ctx.getAdminMergedMemberFilterId() != null) {
                BizUser eff = userMapper.findById(ctx.getEffectiveUserId());
                return eff == null ? List.of() : listMemberProjectsSingleAccount(eff);
            }
            return listProjectsAdminMerged(session);
        }
        if (CollabJwtService.PROJECT_SCOPE_ALL.equals(scope)) {
            return listProjectsAcrossTenantsForNaturalPerson(session);
        }

        long tid = session.getTenantId() != null ? session.getTenantId() : TenantConstants.DEFAULT_TENANT_ID;
        long uid = ctx.getSessionUserId();
        if (ROLE_SUPER_ADMIN.equals(session.getRole())) {
            return projectMapper.listByTenant(tid);
        }
        if (ROLE_SUB_ADMIN.equals(session.getRole())) {
            List<Long> projectIds = memberMapper.listProjectIdsByUserIdAndRole(uid, "admin");
            return projectMapper.listByTenant(tid).stream()
                    .filter(p -> projectIds.contains(p.getId()))
                    .collect(Collectors.toList());
        }
        List<Long> myProjectIds = memberMapper.listProjectIdsByUserId(uid);
        return projectMapper.listByTenant(tid).stream()
                .filter(p -> myProjectIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    /**
     * @param projectScope {@link CollabJwtService#PROJECT_SCOPE_ALL} 等；null 时按角色推断（兼容旧 JWT）
     * @param phoneMemberIds 仅 {@link CollabJwtService#PROJECT_SCOPE_PHONE_MEMBERS}：同号成员 userId 列表（可含 subject）
     */
    public List<BizProject> listProjectsForUser(long userId, String projectScope, List<Long> phoneMemberIds) {
        BizUser user = userMapper.findById(userId);
        if (user == null) return List.of();

        String scope = resolveProjectScope(projectScope, user);

        if (CollabJwtService.PROJECT_SCOPE_EMAIL.equals(scope)) {
            return listMemberProjectsSingleAccount(user);
        }
        if (CollabJwtService.PROJECT_SCOPE_PHONE_MEMBERS.equals(scope)) {
            return listMemberProjectsPhoneUnion(phoneMemberIds, userId);
        }
        if (CollabJwtService.PROJECT_SCOPE_ADMIN_MERGED.equals(scope)) {
            return listProjectsAdminMerged(user);
        }
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

    public List<BizProject> listProjectsForUser(long userId, String projectScope) {
        return listProjectsForUser(userId, projectScope, null);
    }

    /** @deprecated 使用 {@link #listProjectsForUser(long, String)} */
    @Deprecated
    public List<BizProject> listProjectsForUser(long userId) {
        return listProjectsForUser(userId, null);
    }

    /** 成员邮箱登录：仅当前 biz_user 在本租户内参与的项目 */
    private List<BizProject> listMemberProjectsSingleAccount(BizUser user) {
        long tid = user.getTenantId() != null ? user.getTenantId() : TenantConstants.DEFAULT_TENANT_ID;
        if (ROLE_SUPER_ADMIN.equals(user.getRole())) {
            return projectMapper.listByTenant(tid);
        }
        if (ROLE_SUB_ADMIN.equals(user.getRole())) {
            List<Long> projectIds = memberMapper.listProjectIdsByUserIdAndRole(user.getId(), "admin");
            return projectMapper.listByTenant(tid).stream()
                    .filter(p -> projectIds.contains(p.getId()))
                    .collect(Collectors.toList());
        }
        List<Long> myProjectIds = memberMapper.listProjectIdsByUserId(user.getId());
        return projectMapper.listByTenant(tid).stream()
                .filter(p -> myProjectIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    /** 成员手机登录：同号下多个成员账号参与项目的并集 */
    private List<BizProject> listMemberProjectsPhoneUnion(List<Long> phoneMemberIds, long fallbackUserId) {
        Set<Long> uid = new LinkedHashSet<>();
        if (phoneMemberIds != null) {
            for (Long id : phoneMemberIds) {
                if (id != null) uid.add(id);
            }
        }
        if (uid.isEmpty()) {
            uid.add(fallbackUserId);
        }
        List<Long> pids = memberMapper.listProjectIdsByUserIds(new ArrayList<>(uid));
        if (pids == null || pids.isEmpty()) {
            return List.of();
        }
        Set<Long> uniq = new LinkedHashSet<>(pids);
        return projectMapper.listByIds(new ArrayList<>(uniq));
    }

    /**
     * 管理员协作 JWT：本租户全部项目 + 同手机号下其它成员账号（不含本租户）参与的项目，后者仅成员权限范围。
     */
    private List<BizProject> listProjectsAdminMerged(BizUser adminBiz) {
        if (adminBiz == null || !ROLE_SUPER_ADMIN.equals(adminBiz.getRole())) {
            return List.of();
        }
        Long homeTid = adminBiz.getTenantId();
        if (homeTid == null) {
            return List.of();
        }
        String phone = adminBiz.getEmail() != null ? adminBiz.getEmail().trim() : "";
        if (phone.isEmpty() || !phone.startsWith("+")) {
            return projectMapper.listByTenant(homeTid);
        }
        List<BizProject> home = projectMapper.listByTenant(homeTid);
        List<BizUser> samePhone = userMapper.listByPhoneE164(phone);
        List<Long> siblingMemberIds = samePhone.stream()
                .filter(u -> ROLE_MEMBER.equals(u.getRole()))
                .map(BizUser::getId)
                .collect(Collectors.toList());
        if (siblingMemberIds.isEmpty()) {
            return home;
        }
        List<Long> extPids = memberMapper.listProjectIdsByUserIds(siblingMemberIds);
        if (extPids == null || extPids.isEmpty()) {
            return home;
        }
        Set<Long> uniq = new LinkedHashSet<>(extPids);
        List<BizProject> fromMembers = projectMapper.listByIds(new ArrayList<>(uniq)).stream()
                .filter(p -> p.getTenantId() != null && !p.getTenantId().equals(homeTid))
                .collect(Collectors.toList());
        return mergeById(home, fromMembers);
    }

    private static List<BizProject> mergeById(List<BizProject> a, List<BizProject> b) {
        Map<Long, BizProject> map = new LinkedHashMap<>();
        for (BizProject p : a) {
            if (p != null && p.getId() != null) map.put(p.getId(), p);
        }
        for (BizProject p : b) {
            if (p != null && p.getId() != null) map.putIfAbsent(p.getId(), p);
        }
        return new ArrayList<>(map.values());
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
     * 同一自然人在各租户下的 biz_user 行通过相同 email 关联；聚合全部参与项目（旧 JWT {@link CollabJwtService#PROJECT_SCOPE_ALL}）。
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
        return projectMapper.listByIds(new ArrayList<>(uniq));
    }

    public BizProject getById(long projectId) {
        return projectMapper.findById(projectId);
    }

    public boolean canAccessProject(CollabAccessContext ctx, long projectId) {
        BizUser session = userMapper.findById(ctx.getSessionUserId());
        if (session == null) return false;
        String scope = resolveProjectScope(ctx.getProjectScope(), session);

        if (CollabJwtService.PROJECT_SCOPE_EMAIL.equals(scope)) {
            BizUser eff = userMapper.findById(ctx.getEffectiveUserId());
            return eff != null && canAccessMemberSingleAccount(eff, projectId);
        }
        if (CollabJwtService.PROJECT_SCOPE_PHONE_MEMBERS.equals(scope)) {
            return canAccessMemberPhoneUnion(ctx.getPhoneMemberIds(), ctx.getEffectiveUserId(), projectId);
        }
        if (CollabJwtService.PROJECT_SCOPE_ADMIN_MERGED.equals(scope)) {
            if (ctx.getAdminMergedMemberFilterId() != null) {
                BizUser eff = userMapper.findById(ctx.getEffectiveUserId());
                return eff != null && canAccessMemberSingleAccount(eff, projectId);
            }
            return canAccessAdminMerged(session, projectId);
        }
        if (CollabJwtService.PROJECT_SCOPE_ALL.equals(scope)) {
            List<Long> userIds = userMapper.listIdsByEmailNormalized(session.getEmail());
            if (userIds == null || userIds.isEmpty()) return false;
            List<Long> pids = memberMapper.listProjectIdsByUserIds(userIds);
            return pids != null && pids.stream().anyMatch(id -> Objects.equals(id, projectId));
        }

        if (ROLE_SUPER_ADMIN.equals(session.getRole())) return true;
        List<Long> ids = memberMapper.listProjectIdsByUserId(ctx.getSessionUserId());
        return ids.contains(projectId);
    }

    public boolean canAccessProject(long userId, long projectId, String projectScope, List<Long> phoneMemberIds) {
        BizUser user = userMapper.findById(userId);
        if (user == null) return false;

        String scope = resolveProjectScope(projectScope, user);

        if (CollabJwtService.PROJECT_SCOPE_EMAIL.equals(scope)) {
            return canAccessMemberSingleAccount(user, projectId);
        }
        if (CollabJwtService.PROJECT_SCOPE_PHONE_MEMBERS.equals(scope)) {
            return canAccessMemberPhoneUnion(phoneMemberIds, userId, projectId);
        }
        if (CollabJwtService.PROJECT_SCOPE_ADMIN_MERGED.equals(scope)) {
            return canAccessAdminMerged(user, projectId);
        }
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

    public boolean canAccessProject(long userId, long projectId, String projectScope) {
        return canAccessProject(userId, projectId, projectScope, null);
    }

    private boolean canAccessMemberSingleAccount(BizUser user, long projectId) {
        if (ROLE_SUPER_ADMIN.equals(user.getRole())) {
            BizProject p = projectMapper.findById(projectId);
            return p != null && p.getTenantId() != null && p.getTenantId().equals(
                    user.getTenantId() != null ? user.getTenantId() : TenantConstants.DEFAULT_TENANT_ID);
        }
        List<Long> ids = memberMapper.listProjectIdsByUserId(user.getId());
        return ids.contains(projectId);
    }

    private boolean canAccessMemberPhoneUnion(List<Long> phoneMemberIds, long fallbackUserId, long projectId) {
        Set<Long> uid = new LinkedHashSet<>();
        if (phoneMemberIds != null) {
            for (Long id : phoneMemberIds) {
                if (id != null) uid.add(id);
            }
        }
        if (uid.isEmpty()) {
            uid.add(fallbackUserId);
        }
        for (Long id : uid) {
            List<Long> pids = memberMapper.listProjectIdsByUserId(id);
            if (pids != null && pids.contains(projectId)) {
                return true;
            }
        }
        return false;
    }

    private boolean canAccessAdminMerged(BizUser adminBiz, long projectId) {
        if (!ROLE_SUPER_ADMIN.equals(adminBiz.getRole())) {
            return false;
        }
        Long homeTid = adminBiz.getTenantId();
        if (homeTid == null) return false;
        BizProject p = projectMapper.findById(projectId);
        if (p == null) return false;
        if (p.getTenantId() != null && p.getTenantId().equals(homeTid)) {
            return true;
        }
        String phone = adminBiz.getEmail() != null ? adminBiz.getEmail().trim() : "";
        if (phone.isEmpty() || !phone.startsWith("+")) {
            return false;
        }
        List<BizUser> samePhone = userMapper.listByPhoneE164(phone);
        List<Long> siblingMemberIds = samePhone.stream()
                .filter(u -> ROLE_MEMBER.equals(u.getRole()))
                .map(BizUser::getId)
                .collect(Collectors.toList());
        for (Long mid : siblingMemberIds) {
            List<Long> pids = memberMapper.listProjectIdsByUserId(mid);
            if (pids != null && pids.contains(projectId)) {
                return true;
            }
        }
        return false;
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
     * 管理员以成员身份参与的其它团队项目（不含本租户）。用于仅展示「外部」列表的接口。
     */
    public List<BizProject> listExternalProjectsForAdmin(long homeTenantId, String adminPhoneE164) {
        if (adminPhoneE164 == null || adminPhoneE164.isBlank()) {
            return List.of();
        }
        String phone = adminPhoneE164.trim();
        List<BizUser> samePhone = userMapper.listByPhoneE164(phone);
        List<Long> memberIds = samePhone.stream()
                .filter(u -> ROLE_MEMBER.equals(u.getRole()))
                .map(BizUser::getId)
                .collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            return List.of();
        }
        List<Long> projectIds = memberMapper.listProjectIdsByUserIds(memberIds);
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
