package org.example.atuo_attend_backend.prototype.penpot;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.atuo_attend_backend.prototype.config.PenpotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 确保存在 Penpot 团队/项目（每个快原型项目一个 project，便于隔离）。
 */
@Service
public class PenpotWorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(PenpotWorkspaceService.class);

    private final PenpotRpcClient rpc;
    private final PenpotProperties props;

    public PenpotWorkspaceService(PenpotRpcClient rpc, PenpotProperties props) {
        this.rpc = rpc;
        this.props = props;
    }

    public record TeamProject(String teamId, String projectId) {}

    /**
     * 解析或创建：优先使用租户已保存的 team/project；否则选默认团队并创建子项目。
     */
    public TeamProject resolveTeamAndProject(String existingTeamId, String existingProjectId, String projectName,
                                             String tenantAccessToken) {
        String teamId = pickTeamId(existingTeamId, tenantAccessToken);
        String projectId = existingProjectId;
        if (!StringUtils.hasText(projectId)) {
            projectId = createProject(teamId, safeProjectName(projectName), tenantAccessToken);
        }
        return new TeamProject(teamId, projectId);
    }

    public String createDesignFile(String projectId, String fileName, String tenantAccessToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", projectId);
        body.put("name", truncate(fileName, 240));
        JsonNode res = rpc.command("create-file", body, tenantAccessToken);
        JsonNode id = res.get("id");
        if (id == null || id.isNull() || !StringUtils.hasText(id.asText())) {
            throw new IllegalStateException("create-file 未返回 id");
        }
        return id.asText();
    }

    public String buildWorkspaceUrl(String projectId, String fileId) {
        String base = trimSlash(props.getEffectiveBrowserBaseUrl());
        return base + "/workspace/" + projectId + "/" + fileId;
    }

    private String pickTeamId(String savedTeamId, String tenantAccessToken) {
        if (StringUtils.hasText(savedTeamId)) {
            return savedTeamId.trim();
        }
        JsonNode teams = rpc.command("get-teams", Map.of(), tenantAccessToken);
        if (!teams.isArray() || teams.isEmpty()) {
            throw new IllegalStateException("Penpot get-teams 为空：请确认服务账号已加入团队");
        }
        for (JsonNode t : teams) {
            if (t.has("isDefault") && t.get("isDefault").asBoolean()) {
                return t.get("id").asText();
            }
        }
        return teams.get(0).get("id").asText();
    }

    private String createProject(String teamId, String name, String tenantAccessToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("teamId", teamId);
        body.put("name", name);
        JsonNode res = rpc.command("create-project", body, tenantAccessToken);
        if (res.has("id") && !res.get("id").isNull()) {
            return res.get("id").asText();
        }
        throw new IllegalStateException("create-project 未返回 id");
    }

    private static String safeProjectName(String name) {
        String n = name != null ? name.trim() : "";
        if (n.isEmpty()) {
            return "快原型-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return truncate("AA-" + n, 240);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String trimSlash(String u) {
        if (u == null) return "";
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }
}
