package org.example.atuo_attend_backend.nexus.dto;

public class NexusInstanceOpsRequest {
    /** 完整 https URL，可为 null 表示清空 */
    private String btPanelUrl;

    public String getBtPanelUrl() {
        return btPanelUrl;
    }

    public void setBtPanelUrl(String btPanelUrl) {
        this.btPanelUrl = btPanelUrl;
    }
}
