package org.example.atuo_attend_backend.platform.dto;

import java.time.LocalDateTime;

/**
 * 监测后台：租户维度汇总（SQL 报表行）。
 */
public class PlatformTenantOpsRow {

    private Long tenantId;
    private String tenantName;
    private String slug;
    private String adminPhone;
    private String planCode;
    private String status;
    private LocalDateTime tenantCreatedAt;
    private Long memberCount;
    private Long githubLinkedProjects;
    private Long commits24h;
    private Long commits7d;
    /** 近 24h 有提交的不重复 author_email 数，作日活近似 */
    private Long dauEmails24h;
    /** aa_commit_diff 按租户汇总，作存储占用近似 */
    private Long storageDiffBytesApprox;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getAdminPhone() {
        return adminPhone;
    }

    public void setAdminPhone(String adminPhone) {
        this.adminPhone = adminPhone;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTenantCreatedAt() {
        return tenantCreatedAt;
    }

    public void setTenantCreatedAt(LocalDateTime tenantCreatedAt) {
        this.tenantCreatedAt = tenantCreatedAt;
    }

    public Long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Long memberCount) {
        this.memberCount = memberCount;
    }

    public Long getGithubLinkedProjects() {
        return githubLinkedProjects;
    }

    public void setGithubLinkedProjects(Long githubLinkedProjects) {
        this.githubLinkedProjects = githubLinkedProjects;
    }

    public Long getCommits24h() {
        return commits24h;
    }

    public void setCommits24h(Long commits24h) {
        this.commits24h = commits24h;
    }

    public Long getCommits7d() {
        return commits7d;
    }

    public void setCommits7d(Long commits7d) {
        this.commits7d = commits7d;
    }

    public Long getDauEmails24h() {
        return dauEmails24h;
    }

    public void setDauEmails24h(Long dauEmails24h) {
        this.dauEmails24h = dauEmails24h;
    }

    public Long getStorageDiffBytesApprox() {
        return storageDiffBytesApprox;
    }

    public void setStorageDiffBytesApprox(Long storageDiffBytesApprox) {
        this.storageDiffBytesApprox = storageDiffBytesApprox;
    }
}
