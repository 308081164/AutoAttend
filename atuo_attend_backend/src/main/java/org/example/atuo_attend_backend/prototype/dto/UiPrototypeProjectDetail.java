package org.example.atuo_attend_backend.prototype.dto;

import java.util.List;

public class UiPrototypeProjectDetail {
    private Long id;
    private String name;
    private Integer currentSpecVersion;
    /** Penpot Beta：最近一次文件与预览（方案 A：映射在服务端账号下） */
    private String penpotTeamId;
    private String penpotProjectId;
    private String penpotFileId;
    private String penpotPreviewUrl;
    private List<UiPrototypeSpecItem> specs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCurrentSpecVersion() { return currentSpecVersion; }
    public void setCurrentSpecVersion(Integer currentSpecVersion) { this.currentSpecVersion = currentSpecVersion; }

    public List<UiPrototypeSpecItem> getSpecs() { return specs; }
    public void setSpecs(List<UiPrototypeSpecItem> specs) { this.specs = specs; }

    public String getPenpotTeamId() { return penpotTeamId; }
    public void setPenpotTeamId(String penpotTeamId) { this.penpotTeamId = penpotTeamId; }

    public String getPenpotProjectId() { return penpotProjectId; }
    public void setPenpotProjectId(String penpotProjectId) { this.penpotProjectId = penpotProjectId; }

    public String getPenpotFileId() { return penpotFileId; }
    public void setPenpotFileId(String penpotFileId) { this.penpotFileId = penpotFileId; }

    public String getPenpotPreviewUrl() { return penpotPreviewUrl; }
    public void setPenpotPreviewUrl(String penpotPreviewUrl) { this.penpotPreviewUrl = penpotPreviewUrl; }
}

