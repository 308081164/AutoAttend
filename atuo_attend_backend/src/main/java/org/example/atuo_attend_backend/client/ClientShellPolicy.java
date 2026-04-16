package org.example.atuo_attend_backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 壳客户端版本策略（存 JSON 于 aa_system_config）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientShellPolicy {

    private String minSupportedVersion = "";
    private List<String> blockedVersions = new ArrayList<>();
    private List<Integer> blockedBuilds = new ArrayList<>();
    private String blockMessage = "当前客户端版本已停止使用，请下载安装新版本。";
    private String upgradeUrl = "";

    public String getMinSupportedVersion() {
        return minSupportedVersion;
    }

    public void setMinSupportedVersion(String minSupportedVersion) {
        this.minSupportedVersion = minSupportedVersion;
    }

    public List<String> getBlockedVersions() {
        return blockedVersions;
    }

    public void setBlockedVersions(List<String> blockedVersions) {
        this.blockedVersions = blockedVersions != null ? blockedVersions : new ArrayList<>();
    }

    public List<Integer> getBlockedBuilds() {
        return blockedBuilds;
    }

    public void setBlockedBuilds(List<Integer> blockedBuilds) {
        this.blockedBuilds = blockedBuilds != null ? blockedBuilds : new ArrayList<>();
    }

    public String getBlockMessage() {
        return blockMessage;
    }

    public void setBlockMessage(String blockMessage) {
        this.blockMessage = blockMessage;
    }

    public String getUpgradeUrl() {
        return upgradeUrl;
    }

    public void setUpgradeUrl(String upgradeUrl) {
        this.upgradeUrl = upgradeUrl;
    }
}
