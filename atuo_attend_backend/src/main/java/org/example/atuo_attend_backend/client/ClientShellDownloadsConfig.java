package org.example.atuo_attend_backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 官网展示的 GitHub Releases 下载信息（存 JSON）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientShellDownloadsConfig {

    /** 如 308081164/AutoAttend */
    private String githubRepo = "";
    /** Release tag 前缀，如 app-v */
    private String releaseTagPrefix = "app-v";
    private String latestVersion = "";
    /** 手动覆盖各端直链（可选） */
    private String windowsUrl = "";
    private String linuxDebUrl = "";
    private String androidApkUrl = "";
    private String iosNoteUrl = "";

    public String getGithubRepo() {
        return githubRepo;
    }

    public void setGithubRepo(String githubRepo) {
        this.githubRepo = githubRepo;
    }

    public String getReleaseTagPrefix() {
        return releaseTagPrefix;
    }

    public void setReleaseTagPrefix(String releaseTagPrefix) {
        this.releaseTagPrefix = releaseTagPrefix;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public String getWindowsUrl() {
        return windowsUrl;
    }

    public void setWindowsUrl(String windowsUrl) {
        this.windowsUrl = windowsUrl;
    }

    public String getLinuxDebUrl() {
        return linuxDebUrl;
    }

    public void setLinuxDebUrl(String linuxDebUrl) {
        this.linuxDebUrl = linuxDebUrl;
    }

    public String getAndroidApkUrl() {
        return androidApkUrl;
    }

    public void setAndroidApkUrl(String androidApkUrl) {
        this.androidApkUrl = androidApkUrl;
    }

    public String getIosNoteUrl() {
        return iosNoteUrl;
    }

    public void setIosNoteUrl(String iosNoteUrl) {
        this.iosNoteUrl = iosNoteUrl;
    }
}
