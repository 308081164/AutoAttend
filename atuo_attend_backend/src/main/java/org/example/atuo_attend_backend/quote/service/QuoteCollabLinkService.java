package org.example.atuo_attend_backend.quote.service;

import org.example.atuo_attend_backend.collab.CollabTablePurpose;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;
import org.example.atuo_attend_backend.collab.domain.BizTableColumn;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectTableMapper;
import org.example.atuo_attend_backend.collab.mapper.BizTableColumnMapper;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabSyncService;
import org.example.atuo_attend_backend.quote.domain.QuoteItem;
import org.example.atuo_attend_backend.quote.domain.QuoteModule;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;
import org.example.atuo_attend_backend.quote.mapper.QuoteItemMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteModuleMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 报价项目与协作多维表（link_table_id / 仓库）之间的需求摘要拉取与「待开发功能清单」建表。
 */
@Service
public class QuoteCollabLinkService {

    private final QuoteProjectMapper quoteProjectMapper;
    private final QuoteModuleMapper quoteModuleMapper;
    private final QuoteItemMapper quoteItemMapper;
    private final BizProjectMapper bizProjectMapper;
    private final BizProjectTableMapper bizProjectTableMapper;
    private final BizTableColumnMapper bizTableColumnMapper;
    private final CollabRecordService collabRecordService;
    private final CollabSyncService collabSyncService;

    public QuoteCollabLinkService(QuoteProjectMapper quoteProjectMapper,
                                  QuoteModuleMapper quoteModuleMapper,
                                  QuoteItemMapper quoteItemMapper,
                                  BizProjectMapper bizProjectMapper,
                                  BizProjectTableMapper bizProjectTableMapper,
                                  BizTableColumnMapper bizTableColumnMapper,
                                  CollabRecordService collabRecordService,
                                  CollabSyncService collabSyncService) {
        this.quoteProjectMapper = quoteProjectMapper;
        this.quoteModuleMapper = quoteModuleMapper;
        this.quoteItemMapper = quoteItemMapper;
        this.bizProjectMapper = bizProjectMapper;
        this.bizProjectTableMapper = bizProjectTableMapper;
        this.bizTableColumnMapper = bizTableColumnMapper;
        this.collabRecordService = collabRecordService;
        this.collabSyncService = collabSyncService;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    /**
     * 解析报价项目关联的协作项目 ID：优先 link_table_id，其次 github 仓库 full_name。
     *
     * @param tenantId 显式租户（管理端 {@link TenantContext} 可能已清空，勿仅依赖 ThreadLocal）
     */
    public long resolveCollabProjectId(QuoteProject qp, long tenantId) {
        if (qp == null) {
            throw new IllegalArgumentException("报价项目不存在");
        }
        if (qp.getLinkTableId() != null && qp.getLinkTableId() > 0) {
            return qp.getLinkTableId();
        }
        String repo = qp.getGithubRepoFullName();
        if (repo != null && !repo.isBlank()) {
            BizProject bp = bizProjectMapper.findByTenantAndRepoId(tenantId, repo.trim());
            if (bp != null) {
                return bp.getId();
            }
        }
        throw new IllegalArgumentException("请先完成「创建仓库」或手动绑定协作项目（link_table_id / GitHub 仓库）");
    }

    /**
     * 同 {@link #resolveCollabProjectId(QuoteProject, long)}，租户取自当前 {@link TenantContext}。
     */
    public long resolveCollabProjectId(QuoteProject qp) {
        return resolveCollabProjectId(qp, tid());
    }

    /**
     * 根据报价项目主键解析关联的协作项目 ID（校验租户归属）。
     */
    public long resolveCollabProjectIdForQuote(long tenantId, long quoteProjectId) {
        QuoteProject qp = quoteProjectMapper.findById(tenantId, quoteProjectId);
        if (qp == null) {
            throw new IllegalArgumentException("报价项目不存在");
        }
        return resolveCollabProjectId(qp, tenantId);
    }

    /**
     * 尝试解析协作项目 ID，未绑定时返回 null 而非抛异常。
     */
    public Long tryResolveCollabProjectIdForQuote(long tenantId, long quoteProjectId) {
        QuoteProject qp = quoteProjectMapper.findById(tenantId, quoteProjectId);
        if (qp == null) {
            return null;
        }
        if (qp.getLinkTableId() != null && qp.getLinkTableId() > 0) {
            return qp.getLinkTableId();
        }
        String repo = qp.getGithubRepoFullName();
        if (repo != null && !repo.isBlank()) {
            BizProject bp = bizProjectMapper.findByTenantAndRepoId(tenantId, repo.trim());
            if (bp != null) {
                return bp.getId();
            }
        }
        return null;
    }

    /**
     * 从「项目调整」表（issue_tracking）拉取需求记录摘要，供报价页勾选导入。
     */
    public List<Map<String, Object>> listLinkTableRequirements(long quoteProjectId) {
        QuoteProject qp = quoteProjectMapper.findById(tid(), quoteProjectId);
        if (qp == null) {
            throw new IllegalArgumentException("报价项目不存在");
        }
        long collabProjectId = resolveCollabProjectId(qp);
        BizProjectTable table = bizProjectTableMapper.findByProjectIdAndPurpose(collabProjectId, CollabTablePurpose.ISSUE_TRACKING);
        if (table == null) {
            return List.of();
        }
        Map<String, Long> nameToColId = new HashMap<>();
        for (BizTableColumn c : bizTableColumnMapper.listByTableId(table.getId())) {
            nameToColId.put(c.getName(), c.getId());
        }
        Long descColId = nameToColId.get("问题描述");
        Long moduleColId = nameToColId.get("归属模块");

        List<Map<String, Object>> rows = collabRecordService.listRecords(table.getId(), 1, 500);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Object idObj = row.get("id");
            if (idObj == null) continue;
            long rid = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            item.put("recordId", rid);
            String desc = descColId != null ? str(row.get("c" + descColId)) : "";
            String mod = moduleColId != null ? str(row.get("c" + moduleColId)) : "";
            String title = desc.isEmpty() ? ("记录#" + rid) : (desc.length() > 200 ? desc.substring(0, 200) + "…" : desc);
            item.put("title", title);
            item.put("module", mod);
            item.put("summary", desc);
            out.add(item);
        }
        return out;
    }

    /**
     * 将选中的多维表记录追加为报价「功能点」（新模块「从多维表导入」）。
     */
    @Transactional(rollbackFor = Exception.class)
    public int applyLinkTableRequirements(long quoteProjectId, List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0;
        }
        QuoteProject qp = quoteProjectMapper.findById(tid(), quoteProjectId);
        if (qp == null) {
            throw new IllegalArgumentException("报价项目不存在");
        }
        long collabProjectId = resolveCollabProjectId(qp);
        BizProjectTable table = bizProjectTableMapper.findByProjectIdAndPurpose(collabProjectId, CollabTablePurpose.ISSUE_TRACKING);
        if (table == null) {
            throw new IllegalStateException("协作项目下未找到「项目调整」多维表");
        }
        Map<String, Long> nameToColId = new HashMap<>();
        for (BizTableColumn c : bizTableColumnMapper.listByTableId(table.getId())) {
            nameToColId.put(c.getName(), c.getId());
        }
        Long descColId = nameToColId.get("问题描述");
        if (descColId == null) {
            throw new IllegalStateException("多维表缺少「问题描述」列");
        }

        Set<Long> wanted = new HashSet<>();
        for (Long id : recordIds) {
            if (id != null && id > 0) wanted.add(id);
        }
        if (wanted.isEmpty()) {
            return 0;
        }

        List<Map<String, Object>> rows = collabRecordService.listRecords(table.getId(), 1, 500);
        Map<Long, String> idToDesc = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("id");
            if (idObj == null) continue;
            long rid = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            if (!wanted.contains(rid)) continue;
            idToDesc.put(rid, str(row.get("c" + descColId)));
        }

        List<String> itemNames = new ArrayList<>();
        for (Long rid : recordIds) {
            if (rid == null || rid <= 0) continue;
            String name = idToDesc.get(rid);
            if (name == null || name.isBlank()) continue;
            String itemName = name.trim();
            if (itemName.length() > 512) {
                itemName = itemName.substring(0, 512) + "…";
            }
            itemNames.add(itemName);
        }
        if (itemNames.isEmpty()) {
            return 0;
        }

        QuoteModule m = new QuoteModule();
        m.setTenantId(tid());
        m.setQuoteProjectId(quoteProjectId);
        m.setName("从多维表导入");
        m.setSortOrder(9999);
        quoteModuleMapper.insert(m);

        int ii = 0;
        for (String itemName : itemNames) {
            QuoteItem item = new QuoteItem();
            item.setTenantId(tid());
            item.setModuleId(m.getId());
            item.setName(itemName);
            item.setComplexity("standard");
            item.setQuantity(1);
            item.setEstimatedDays(BigDecimal.ZERO);
            item.setSortOrder(ii++);
            quoteItemMapper.insert(item);
        }
        return itemNames.size();
    }

    /**
     * 为关联协作项目创建「待开发功能清单」多维表（幂等）。
     */
    public Map<String, Object> ensureCollabFeatureTable(long quoteProjectId) {
        QuoteProject qp = quoteProjectMapper.findById(tid(), quoteProjectId);
        if (qp == null) {
            throw new IllegalArgumentException("报价项目不存在");
        }
        long collabProjectId = resolveCollabProjectId(qp);
        BizProjectTable t = collabSyncService.ensureFeatureBacklogTable(collabProjectId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("collabProjectId", collabProjectId);
        data.put("tableId", t.getId());
        data.put("purpose", t.getPurpose());
        data.put("name", t.getName());
        return data;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
