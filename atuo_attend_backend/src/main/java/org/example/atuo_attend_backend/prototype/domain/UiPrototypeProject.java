package org.example.atuo_attend_backend.prototype.domain;

import java.time.LocalDateTime;

public class UiPrototypeProject {
    private Long id;
    private Long tenantId;
    private String name;
    private Integer currentSpecVersion;
    /** Penpot 团队 UUID（方案 A：服务端账号隔离） */
    private String penpotTeamId;
    private String penpotProjectId;
    private String penpotFileId;
    private String penpotPreviewUrl;
    private String penpotExportUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCurrentSpecVersion() { return currentSpecVersion; }
    public void setCurrentSpecVersion(Integer currentSpecVersion) { this.currentSpecVersion = currentSpecVersion; }

    public String getPenpotTeamId() { return penpotTeamId; }
    public void setPenpotTeamId(String penpotTeamId) { this.penpotTeamId = penpotTeamId; }

    public String getPenpotProjectId() { return penpotProjectId; }
    public void setPenpotProjectId(String penpotProjectId) { this.penpotProjectId = penpotProjectId; }

    public String getPenpotFileId() { return penpotFileId; }
    public void setPenpotFileId(String penpotFileId) { this.penpotFileId = penpotFileId; }

    public String getPenpotPreviewUrl() { return penpotPreviewUrl; }
    public void setPenpotPreviewUrl(String penpotPreviewUrl) { this.penpotPreviewUrl = penpotPreviewUrl; }

    public String getPenpotExportUrl() { return penpotExportUrl; }
    public void setPenpotExportUrl(String penpotExportUrl) { this.penpotExportUrl = penpotExportUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

