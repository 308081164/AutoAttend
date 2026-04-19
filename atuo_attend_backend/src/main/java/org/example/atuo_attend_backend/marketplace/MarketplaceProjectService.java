package org.example.atuo_attend_backend.marketplace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.marketplace.domain.MarketplaceProject;
import org.example.atuo_attend_backend.marketplace.mapper.MarketplaceProjectMapper;
import org.example.atuo_attend_backend.nexus.crypto.NexusCryptoService;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MarketplaceProjectService {

    public static final String STATUS_PENDING = "pending_review";
    public static final String STATUS_OPEN = "open";
    public static final String STATUS_CLOSED = "closed";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_REJECTED = "rejected";

    private final MarketplaceProjectMapper projectMapper;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final NexusCryptoService cryptoService;
    private final MarketplaceAccessService accessService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketplaceProjectService(MarketplaceProjectMapper projectMapper,
                                     TenantAdminUserMapper tenantAdminUserMapper,
                                     NexusCryptoService cryptoService,
                                     MarketplaceAccessService accessService) {
        this.projectMapper = projectMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.cryptoService = cryptoService;
        this.accessService = accessService;
    }

    public boolean canPublish(long userId) {
        TenantAdminUser u = tenantAdminUserMapper.findById(userId);
        return u != null && Boolean.TRUE.equals(u.getCanPublishProjectInfo());
    }

    /** 租户已开放发布且当前管理员具备发布权限位 */
    public boolean canPublishProject(long tenantId, long userId) {
        return accessService.isTenantPublishAllowed(tenantId) && canPublish(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketplaceProject create(long tenantId, long userId, Map<String, Object> body) {
        if (!accessService.isPublishWorkflowAllowed(tenantId, userId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        if (!canPublishProject(tenantId, userId)) {
            throw new ForbiddenException("无发布权限（请确认监测台已为本租户开启发布能力，且账号已勾选可发布）");
        }
        String title = str(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title 必填");
        }
        List<String> tech = parseTechStack(body.get("techStack"));
        String techJson = tech.isEmpty() ? null : writeJson(tech);
        String contactPlain = str(body.get("contactValue"));
        if (contactPlain == null || contactPlain.isBlank()) {
            throw new IllegalArgumentException("contactValue 必填");
        }
        String contactType = normalizeContactType(str(body.get("contactType")));

        MarketplaceProject p = new MarketplaceProject();
        p.setTenantId(tenantId);
        p.setPublisherUserId(userId);
        p.setTitle(title.trim());
        p.setDescription(str(body.get("description")));
        p.setTechStackJson(techJson);
        p.setBudgetRange(str(body.get("budgetRange")));
        p.setDuration(str(body.get("duration")));
        p.setLocation(str(body.get("location")));
        p.setContactType(contactType);
        p.setContactValueEnc(cryptoService.encrypt(contactPlain.trim()));
        p.setRejectReason(null);
        p.setViewCount(0);

        boolean review = accessService.requireContentReview();
        LocalDateTime now = LocalDateTime.now();
        if (review) {
            p.setStatus(STATUS_PENDING);
            p.setPublishTime(null);
            p.setExpireTime(null);
        } else {
            p.setStatus(STATUS_OPEN);
            p.setPublishTime(now);
            p.setExpireTime(now.plusDays(30));
        }
        projectMapper.insert(p);
        return projectMapper.findById(p.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketplaceProject update(long tenantId, long userId, long projectId, Map<String, Object> body) {
        if (!accessService.isPublishWorkflowAllowed(tenantId, userId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        if (!canPublishProject(tenantId, userId)) {
            throw new ForbiddenException("无发布权限（请确认监测台已为本租户开启发布能力，且账号已勾选可发布）");
        }
        MarketplaceProject existing = projectMapper.findById(projectId);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (!existing.getPublisherUserId().equals(userId)) {
            throw new ForbiddenException("只能编辑本人发布的项目");
        }
        String st = existing.getStatus();
        if (STATUS_CLOSED.equals(st) || STATUS_COMPLETED.equals(st) || STATUS_REJECTED.equals(st)) {
            throw new IllegalArgumentException("当前状态不可编辑");
        }
        if (str(body.get("title")) != null) {
            existing.setTitle(str(body.get("title")).trim());
        }
        if (body.containsKey("description")) {
            existing.setDescription(str(body.get("description")));
        }
        if (body.containsKey("techStack")) {
            List<String> tech = parseTechStack(body.get("techStack"));
            existing.setTechStackJson(tech.isEmpty() ? null : writeJson(tech));
        }
        if (body.containsKey("budgetRange")) {
            existing.setBudgetRange(str(body.get("budgetRange")));
        }
        if (body.containsKey("duration")) {
            existing.setDuration(str(body.get("duration")));
        }
        if (body.containsKey("location")) {
            existing.setLocation(str(body.get("location")));
        }
        if (body.containsKey("contactType")) {
            existing.setContactType(normalizeContactType(str(body.get("contactType"))));
        }
        if (body.containsKey("contactValue")) {
            String cv = str(body.get("contactValue"));
            if (cv != null && !cv.isBlank() && !cv.contains("****")) {
                existing.setContactValueEnc(cryptoService.encrypt(cv.trim()));
            }
        }
        if (body.containsKey("expireTime")) {
            existing.setExpireTime(parseDateTime(body.get("expireTime")));
        }
        projectMapper.updateEditable(existing);
        return projectMapper.findById(projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void close(long tenantId, long userId, long projectId) {
        if (!accessService.isPublishWorkflowAllowed(tenantId, userId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        if (!canPublishProject(tenantId, userId)) {
            throw new ForbiddenException("无发布权限（请确认监测台已为本租户开启发布能力，且账号已勾选可发布）");
        }
        MarketplaceProject existing = projectMapper.findById(projectId);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (!existing.getPublisherUserId().equals(userId)) {
            throw new ForbiddenException("只能关闭本人发布的项目");
        }
        projectMapper.updateStatus(projectId, tenantId, STATUS_CLOSED, null);
    }

    public Map<String, Object> getDetail(long tenantId, long viewerUserId, long projectId, boolean incrementView) {
        if (!accessService.isModuleVisibleForAdmin(tenantId, viewerUserId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        MarketplaceProject p = projectMapper.findById(projectId);
        if (p == null || !p.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (incrementView && STATUS_OPEN.equals(p.getStatus())) {
            projectMapper.incrementViewCount(projectId, tenantId);
            p = projectMapper.findById(projectId);
        }
        return toMap(p, true);
    }

    public Map<String, Object> list(long tenantId, long viewerUserId, String q, String tech, String location,
                                    String sort, String statusFilter, int page, int pageSize) {
        if (!accessService.isModuleVisibleForAdmin(tenantId, viewerUserId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        int ps = Math.min(Math.max(pageSize, 1), 100);
        int p = Math.max(page, 1);
        int offset = (p - 1) * ps;
        String sf = statusFilter != null && !statusFilter.isBlank() ? statusFilter : null;
        long total = projectMapper.countList(tenantId, emptyToNull(q), emptyToNull(tech), emptyToNull(location), sf);
        String s = sort == null ? "newest" : sort.trim().toLowerCase();
        List<MarketplaceProject> rows;
        if ("hot".equals(s)) {
            rows = projectMapper.listPageHot(tenantId, emptyToNull(q), emptyToNull(tech), emptyToNull(location), sf, offset, ps);
        } else if ("budget".equals(s)) {
            rows = projectMapper.listPageBudget(tenantId, emptyToNull(q), emptyToNull(tech), emptyToNull(location), sf, offset, ps);
        } else {
            rows = projectMapper.listPageNewest(tenantId, emptyToNull(q), emptyToNull(tech), emptyToNull(location), sf, offset, ps);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (MarketplaceProject row : rows) {
            items.add(toMap(row, false));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("page", p);
        out.put("pageSize", ps);
        out.put("items", items);
        return out;
    }

    public Map<String, Object> listMine(long tenantId, long userId, int page, int pageSize) {
        if (!accessService.isPublishWorkflowAllowed(tenantId, userId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        if (!canPublishProject(tenantId, userId)) {
            throw new ForbiddenException("无发布权限（请确认监测台已为本租户开启发布能力，且账号已勾选可发布）");
        }
        int ps = Math.min(Math.max(pageSize, 1), 100);
        int p = Math.max(page, 1);
        int offset = (p - 1) * ps;
        long total = projectMapper.countMine(tenantId, userId);
        List<MarketplaceProject> rows = projectMapper.listMine(tenantId, userId, offset, ps);
        List<Map<String, Object>> items = new ArrayList<>();
        for (MarketplaceProject row : rows) {
            items.add(toMap(row, false));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("page", p);
        out.put("pageSize", ps);
        out.put("items", items);
        return out;
    }

    public List<Map<String, Object>> listPending(long tenantId, long reviewerUserId) {
        if (!accessService.isPublishWorkflowAllowed(tenantId, reviewerUserId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        if (!canPublishProject(tenantId, reviewerUserId)) {
            throw new ForbiddenException("无审核权限（需监测台开启本租户发布能力，且账号具备发布权限）");
        }
        List<MarketplaceProject> rows = projectMapper.listPending(tenantId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (MarketplaceProject row : rows) {
            out.add(toMap(row, false));
        }
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(long tenantId, long reviewerUserId, long projectId) {
        if (!accessService.isPublishWorkflowAllowed(tenantId, reviewerUserId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        if (!canPublishProject(tenantId, reviewerUserId)) {
            throw new ForbiddenException("无审核权限");
        }
        MarketplaceProject p = projectMapper.findById(projectId);
        if (p == null || !p.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (!STATUS_PENDING.equals(p.getStatus())) {
            throw new IllegalArgumentException("非待审核状态");
        }
        LocalDateTime now = LocalDateTime.now();
        projectMapper.approve(projectId, tenantId, STATUS_OPEN, now, now.plusDays(30));
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(long tenantId, long reviewerUserId, long projectId, String reason) {
        if (!accessService.isPublishWorkflowAllowed(tenantId, reviewerUserId)) {
            throw new ForbiddenException("项目信息发布未开放或不在白名单内");
        }
        if (!canPublishProject(tenantId, reviewerUserId)) {
            throw new ForbiddenException("无审核权限");
        }
        MarketplaceProject p = projectMapper.findById(projectId);
        if (p == null || !p.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (!STATUS_PENDING.equals(p.getStatus())) {
            throw new IllegalArgumentException("非待审核状态");
        }
        String r = reason == null ? "" : reason.trim();
        if (r.length() > 500) {
            r = r.substring(0, 500);
        }
        projectMapper.updateStatus(projectId, tenantId, STATUS_REJECTED, r.isEmpty() ? "已驳回" : r);
    }

    /** @return 关闭的条数（近似，取决于 JDBC 驱动返回值） */
    public int runExpireJobReturn() {
        return projectMapper.closeExpiredOpen();
    }

    private Map<String, Object> toMap(MarketplaceProject p, boolean includeContact) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("projectId", p.getId());
        m.put("publisherAdminUserId", p.getPublisherUserId());
        TenantAdminUser pub = tenantAdminUserMapper.findById(p.getPublisherUserId());
        m.put("publisherDisplayName", pub != null && pub.getPhone() != null ? pub.getPhone() : "");
        m.put("title", p.getTitle());
        m.put("description", p.getDescription());
        m.put("techStack", readTechStack(p.getTechStackJson()));
        m.put("budgetRange", p.getBudgetRange());
        m.put("duration", p.getDuration());
        m.put("location", p.getLocation());
        m.put("contactType", p.getContactType());
        if (includeContact && p.getContactValueEnc() != null && !p.getContactValueEnc().isBlank()) {
            try {
                m.put("contactValue", cryptoService.decrypt(p.getContactValueEnc()));
            } catch (Exception e) {
                m.put("contactValue", null);
            }
        } else if (includeContact) {
            m.put("contactValue", null);
        }
        m.put("status", p.getStatus());
        m.put("rejectReason", p.getRejectReason());
        m.put("viewCount", p.getViewCount());
        m.put("publishTime", p.getPublishTime());
        m.put("expireTime", p.getExpireTime());
        m.put("createdAt", p.getCreatedAt());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private String writeJson(List<String> tech) {
        try {
            return objectMapper.writeValueAsString(tech);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readTechStack(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseTechStack(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null && !String.valueOf(o).isBlank()) {
                    out.add(String.valueOf(o).trim());
                }
            }
            return out;
        }
        if (raw instanceof String s && !s.isBlank()) {
            return List.of(s.trim());
        }
        return List.of();
    }

    private static String normalizeContactType(String t) {
        if (t == null || t.isBlank()) {
            return "phone";
        }
        String x = t.trim().toLowerCase();
        if (x.contains("mail") || "email".equals(x)) {
            return "email";
        }
        if (x.contains("站内") || "internal".equals(x) || "im".equals(x)) {
            return "internal";
        }
        return "phone";
    }

    private static LocalDateTime parseDateTime(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDateTime ldt) {
            return ldt;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s.replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
