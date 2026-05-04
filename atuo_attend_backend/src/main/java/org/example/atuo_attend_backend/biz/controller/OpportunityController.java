package org.example.atuo_attend_backend.biz.controller;

import org.example.atuo_attend_backend.biz.domain.Opportunity;
import org.example.atuo_attend_backend.biz.mapper.OpportunityMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/biz/opportunities")
public class OpportunityController {

    private final OpportunityMapper opportunityMapper;

    public OpportunityController(OpportunityMapper opportunityMapper) {
        this.opportunityMapper = opportunityMapper;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Opportunity body) {
        body.setTenantId(tid());
        body.setStage(body.getStage() != null ? body.getStage() : "discovery");
        opportunityMapper.insert(body);
        Map<String, Object> data = new HashMap<>();
        data.put("id", body.getId());
        return ApiResponse.ok(data);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody Opportunity body) {
        body.setId(id);
        body.setTenantId(tid());
        opportunityMapper.update(body);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}")
    public ApiResponse<Opportunity> get(@PathVariable long id) {
        Opportunity o = opportunityMapper.findById(tid(), id);
        return o != null ? ApiResponse.ok(o) : ApiResponse.error(404, "商机不存在");
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) String stage) {
        long total = opportunityMapper.countAll(tid());
        List<Opportunity> items;
        if (stage != null && !stage.trim().isEmpty()) {
            items = opportunityMapper.listByStage(tid(), stage.trim());
        } else {
            int offset = (page - 1) * pageSize;
            items = opportunityMapper.listPaged(tid(), offset, pageSize);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return ApiResponse.ok(data);
    }

    @GetMapping("/board")
    public ApiResponse<Map<String, Object>> board() {
        List<Map<String, Object>> stageGroups = opportunityMapper.groupByStage(tid());
        Map<String, Object> data = new HashMap<>();
        data.put("stageGroups", stageGroups);
        return ApiResponse.ok(data);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        opportunityMapper.deleteById(tid(), id);
        return ApiResponse.ok();
    }
}
