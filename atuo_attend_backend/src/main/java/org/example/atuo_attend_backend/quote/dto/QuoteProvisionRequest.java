package org.example.atuo_attend_backend.quote.dto;

public class QuoteProvisionRequest {
    private String repoName;
    private Boolean repoPrivate;
    private String description;
    private Boolean autoInit;
    private Boolean syncMd;
    private Boolean syncCollabTable;
    private Boolean createWebhook;
    /** 是否在仓库根目录创建 AGENTS.md（开放格式，供 AI 编码助手读取） */
    private Boolean createAgentsMd;

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public Boolean getRepoPrivate() { return repoPrivate; }
    public void setRepoPrivate(Boolean repoPrivate) { this.repoPrivate = repoPrivate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getAutoInit() { return autoInit; }
    public void setAutoInit(Boolean autoInit) { this.autoInit = autoInit; }
    public Boolean getSyncMd() { return syncMd; }
    public void setSyncMd(Boolean syncMd) { this.syncMd = syncMd; }
    public Boolean getSyncCollabTable() { return syncCollabTable; }
    public void setSyncCollabTable(Boolean syncCollabTable) { this.syncCollabTable = syncCollabTable; }
    public Boolean getCreateWebhook() { return createWebhook; }
    public void setCreateWebhook(Boolean createWebhook) { this.createWebhook = createWebhook; }
    public Boolean getCreateAgentsMd() { return createAgentsMd; }
    public void setCreateAgentsMd(Boolean createAgentsMd) { this.createAgentsMd = createAgentsMd; }
}

