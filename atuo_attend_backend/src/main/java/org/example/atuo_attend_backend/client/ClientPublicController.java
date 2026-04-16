package org.example.atuo_attend_backend.client;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 匿名：版本检查、官网下载区数据。
 */
@RestController
@RequestMapping("/api/public/client")
public class ClientPublicController {

    private final ClientVersionPolicyService policyService;

    public ClientPublicController(ClientVersionPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/version-check")
    public ApiResponse<Map<String, Object>> versionCheck(
            @RequestHeader(value = ClientShellEnforcementFilter.HEADER_VERSION, required = false) String version,
            @RequestHeader(value = ClientShellEnforcementFilter.HEADER_BUILD, required = false) String buildStr,
            @RequestHeader(value = ClientShellEnforcementFilter.HEADER_PLATFORM, required = false) String platform
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("platform", platform);
        Integer build = null;
        if (buildStr != null && !buildStr.isBlank()) {
            try {
                build = Integer.parseInt(buildStr.trim());
            } catch (NumberFormatException ignored) {
                build = null;
            }
        }
        ClientShellPolicy pol = policyService.getPolicy();
        data.put("minSupportedVersion", pol.getMinSupportedVersion());
        data.put("blockedVersions", pol.getBlockedVersions());
        data.put("blockedBuilds", pol.getBlockedBuilds());
        data.put("upgradeUrl", pol.getUpgradeUrl());
        ClientShellDownloadsConfig dl = policyService.getDownloadsConfig();
        data.put("latestVersion", dl.getLatestVersion());
        data.put("downloads", buildDownloadsMap(dl));

        ClientVersionPolicyService.BlockResult block = null;
        if (version != null && !version.isBlank()) {
            block = policyService.evaluate(version.trim(), build);
        }
        data.put("blocked", block != null);
        if (block != null) {
            data.put("message", block.getMessage());
        } else {
            data.put("message", "ok");
        }
        return ApiResponse.ok(data);
    }

    @GetMapping("/downloads")
    public ApiResponse<Map<String, Object>> downloads() {
        ClientShellDownloadsConfig dl = policyService.getDownloadsConfig();
        Map<String, Object> data = new HashMap<>();
        data.put("latestVersion", dl.getLatestVersion());
        data.put("githubRepo", dl.getGithubRepo());
        data.put("releaseTagPrefix", dl.getReleaseTagPrefix());
        data.put("releasePageUrl", buildReleasePageUrl(dl));
        data.putAll(buildDownloadsMap(dl));
        return ApiResponse.ok(data);
    }

    private static String buildReleasePageUrl(ClientShellDownloadsConfig dl) {
        String repo = dl.getGithubRepo();
        String ver = dl.getLatestVersion();
        String prefix = dl.getReleaseTagPrefix();
        if (repo == null || repo.isBlank() || ver == null || ver.isBlank()) {
            return "";
        }
        String tag = (prefix != null ? prefix : "") + ver.trim();
        return "https://github.com/" + repo.trim() + "/releases/tag/" + tag;
    }

    private static Map<String, String> buildDownloadsMap(ClientShellDownloadsConfig dl) {
        Map<String, String> m = new HashMap<>();
        m.put("windowsUrl", nz(dl.getWindowsUrl()));
        m.put("linuxDebUrl", nz(dl.getLinuxDebUrl()));
        m.put("androidApkUrl", nz(dl.getAndroidApkUrl()));
        m.put("iosNoteUrl", nz(dl.getIosNoteUrl()));
        return m;
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }
}
