package org.example.atuo_attend_backend.collab.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 单次协作请求上的身份与项目范围（JWT + 可选 {@link CollabAuthFilter#HEADER_ACTING_USER_ID}）。
 */
public final class CollabAccessContext {

    private final long sessionUserId;
    private final long effectiveUserId;
    private final String projectScope;
    private final List<Long> phoneMemberIds;
    private final Long adminMergedMemberFilterId;

    private CollabAccessContext(long sessionUserId, long effectiveUserId, String projectScope,
                               List<Long> phoneMemberIds, Long adminMergedMemberFilterId) {
        this.sessionUserId = sessionUserId;
        this.effectiveUserId = effectiveUserId;
        this.projectScope = projectScope;
        this.phoneMemberIds = phoneMemberIds;
        this.adminMergedMemberFilterId = adminMergedMemberFilterId;
    }

    public static CollabAccessContext from(HttpServletRequest req) {
        long session = CollabAuthFilter.requireCollabUserId(req);
        long effective = CollabAuthFilter.effectiveUserId(req);
        return new CollabAccessContext(
                session,
                effective,
                CollabAuthFilter.projectScopeFrom(req),
                CollabAuthFilter.phoneMemberIdsFrom(req),
                CollabAuthFilter.adminMergedMemberFilterId(req)
        );
    }

    public long getSessionUserId() {
        return sessionUserId;
    }

    public long getEffectiveUserId() {
        return effectiveUserId;
    }

    public String getProjectScope() {
        return projectScope;
    }

    public List<Long> getPhoneMemberIds() {
        return phoneMemberIds;
    }

    public Long getAdminMergedMemberFilterId() {
        return adminMergedMemberFilterId;
    }
}
