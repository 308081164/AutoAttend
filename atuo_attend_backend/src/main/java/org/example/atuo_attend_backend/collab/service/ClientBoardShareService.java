package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectClientBoard;
import org.example.atuo_attend_backend.collab.mapper.BizProjectClientBoardMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ClientBoardShareService {

    private final BizProjectClientBoardMapper boardMapper;
    private final BizProjectMapper projectMapper;
    private final TenantResourceQuotaService resourceQuotaService;

    public ClientBoardShareService(BizProjectClientBoardMapper boardMapper,
                                   BizProjectMapper projectMapper,
                                   TenantResourceQuotaService resourceQuotaService) {
        this.boardMapper = boardMapper;
        this.projectMapper = projectMapper;
        this.resourceQuotaService = resourceQuotaService;
    }

    public Map<String, Object> getBoardConfig(long projectId) {
        BizProjectClientBoard row = boardMapper.findByProjectId(projectId);
        Map<String, Object> m = new LinkedHashMap<>();
        if (row == null) {
            m.put("enabled", false);
            m.put("hasToken", false);
            m.put("publicToken", "");
            m.put("showProgressDashboard", true);
            m.put("showFeatureBacklog", false);
            m.put("showAiTableEntry", false);
            m.put("aiPurpose", "issue_tracking");
            return m;
        }
        m.put("enabled", Boolean.TRUE.equals(row.getEnabled()));
        m.put("hasToken", row.getPublicToken() != null && !row.getPublicToken().isBlank());
        m.put("publicToken", row.getPublicToken() != null ? row.getPublicToken() : "");
        m.put("showProgressDashboard", row.getShowProgressDashboard() == null || row.getShowProgressDashboard());
        m.put("showFeatureBacklog", Boolean.TRUE.equals(row.getShowFeatureBacklog()));
        m.put("showAiTableEntry", Boolean.TRUE.equals(row.getShowAiTableEntry()));
        m.put("aiPurpose", row.getAiPurpose() != null && !row.getAiPurpose().isBlank() ? row.getAiPurpose() : "issue_tracking");
        return m;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveBoardConfig(long projectId, long tenantId, Map<String, Object> body) {
        BizProject project = projectMapper.findById(projectId);
        if (project == null || project.getTenantId() == null || project.getTenantId().longValue() != tenantId) {
            throw new IllegalArgumentException("项目不存在或无权操作");
        }
        BizProjectClientBoard row = boardMapper.findByProjectId(projectId);
        boolean wasEnabled = row != null && Boolean.TRUE.equals(row.getEnabled());
        boolean enabled = bool(body.get("enabled"), false);
        boolean showProgress = bool(body.get("showProgressDashboard"), true);
        boolean showFeature = bool(body.get("showFeatureBacklog"), false);
        boolean showAi = bool(body.get("showAiTableEntry"), false);
        String aiPurpose = str(body.get("aiPurpose"), "issue_tracking");
        if (aiPurpose.isBlank()) aiPurpose = "issue_tracking";
        boolean regenerate = bool(body.get("regenerateToken"), false);

        resourceQuotaService.assertCanEnableClientBoard(tenantId, wasEnabled, enabled);

        if (row == null) {
            row = new BizProjectClientBoard();
            row.setTenantId(tenantId);
            row.setProjectId(projectId);
            row.setEnabled(enabled);
            row.setPublicToken(newToken());
            row.setShowProgressDashboard(showProgress);
            row.setShowFeatureBacklog(showFeature);
            row.setShowAiTableEntry(showAi);
            row.setAiPurpose(aiPurpose);
            boardMapper.insert(row);
        } else {
            if (row.getTenantId() == null || row.getTenantId().longValue() != tenantId) {
                throw new IllegalArgumentException("无权操作该看板配置");
            }
            row.setEnabled(enabled);
            if (row.getPublicToken() == null || row.getPublicToken().isBlank() || regenerate) {
                row.setPublicToken(newToken());
            }
            row.setShowProgressDashboard(showProgress);
            row.setShowFeatureBacklog(showFeature);
            row.setShowAiTableEntry(showAi);
            row.setAiPurpose(aiPurpose);
            boardMapper.update(row);
        }
        return getBoardConfig(projectId);
    }

    public BizProjectClientBoard requireEnabledBoard(String token) {
        if (token == null || token.isBlank()) return null;
        BizProjectClientBoard row = boardMapper.findByPublicToken(token.trim());
        if (row == null || !Boolean.TRUE.equals(row.getEnabled())) return null;
        return row;
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static boolean bool(Object o, boolean def) {
        if (o == null) return def;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        String s = String.valueOf(o).trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    private static String str(Object o, String def) {
        if (o == null) return def;
        return String.valueOf(o).trim();
    }
}
