package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectClientBoard;
import org.example.atuo_attend_backend.collab.domain.BizProjectPortalLink;
import org.example.atuo_attend_backend.collab.mapper.BizProjectClientBoardMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectPortalLinkMapper;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ClientBoardShareService {

    private static final Logger log = LoggerFactory.getLogger(ClientBoardShareService.class);

    /**
     * 传送门中由系统自动维护的「客户阅览看板」入口展示名；与手动链接区分，保存看板配置时会 upsert 此标签对应的 URL。
     */
    public static final String AUTO_CLIENT_BOARD_PORTAL_LABEL = "客户阅览看板";

    private final BizProjectClientBoardMapper boardMapper;
    private final BizProjectMapper projectMapper;
    private final TenantResourceQuotaService resourceQuotaService;
    private final BizProjectPortalLinkMapper portalLinkMapper;
    private final SystemConfigService systemConfigService;

    public ClientBoardShareService(BizProjectClientBoardMapper boardMapper,
                                   BizProjectMapper projectMapper,
                                   TenantResourceQuotaService resourceQuotaService,
                                   BizProjectPortalLinkMapper portalLinkMapper,
                                   SystemConfigService systemConfigService) {
        this.boardMapper = boardMapper;
        this.projectMapper = projectMapper;
        this.resourceQuotaService = resourceQuotaService;
        this.portalLinkMapper = portalLinkMapper;
        this.systemConfigService = systemConfigService;
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
        try {
            syncAutoClientBoardPortalLink(projectId, enabled, row.getPublicToken());
        } catch (Exception e) {
            log.warn("自动同步客户看板传送门失败 projectId={}: {}", projectId, e.getMessage());
        }
        return getBoardConfig(projectId);
    }

    /**
     * 开启看板或更新令牌后，在传送门中创建/更新固定名称的入口；关闭看板时移除该入口。
     */
    private void syncAutoClientBoardPortalLink(long projectId, boolean enabled, String publicToken) {
        List<BizProjectPortalLink> links = portalLinkMapper.listByProjectId(projectId);
        BizProjectPortalLink autoLink = null;
        for (BizProjectPortalLink l : links) {
            if (l != null && AUTO_CLIENT_BOARD_PORTAL_LABEL.equals(l.getLabel())) {
                autoLink = l;
                break;
            }
        }
        if (autoLink == null) {
            for (BizProjectPortalLink l : links) {
                if (l == null || l.getUrl() == null) continue;
                if (l.getUrl().contains("/client-board/")) {
                    autoLink = l;
                    break;
                }
            }
        }

        if (!enabled || publicToken == null || publicToken.isBlank()) {
            if (autoLink != null && autoLink.getId() != null) {
                portalLinkMapper.deleteById(autoLink.getId());
            }
            return;
        }

        String url = resolveClientBoardPortalUrl(publicToken);
        int maxOrder = 0;
        for (BizProjectPortalLink l : links) {
            if (l == null || l.getSortOrder() == null) continue;
            maxOrder = Math.max(maxOrder, l.getSortOrder());
        }

        if (autoLink != null && autoLink.getId() != null) {
            autoLink.setLabel(AUTO_CLIENT_BOARD_PORTAL_LABEL);
            autoLink.setUrl(url);
            if (autoLink.getSortOrder() == null) {
                autoLink.setSortOrder(maxOrder + 1);
            }
            portalLinkMapper.update(autoLink);
        } else {
            BizProjectPortalLink nl = new BizProjectPortalLink();
            nl.setProjectId(projectId);
            nl.setLabel(AUTO_CLIENT_BOARD_PORTAL_LABEL);
            nl.setUrl(url);
            nl.setSortOrder(maxOrder + 1);
            portalLinkMapper.insert(nl);
        }
    }

    /** 若平台配置了 {@link SystemConfigService#KEY_PUBLIC_BASE_URL}，则生成绝对 URL，否则为站内相对路径（与前端缺省当前 origin 行为一致）。 */
    private String resolveClientBoardPortalUrl(String token) {
        String base = systemConfigService.getPublicBaseUrl();
        String path = clientBoardPath(token);
        if (base != null && !base.isBlank()) {
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            return base + path;
        }
        return path;
    }

    private static String clientBoardPath(String token) {
        String t = token.trim();
        return "/client-board/" + URLEncoder.encode(t, StandardCharsets.UTF_8).replace("+", "%20");
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
