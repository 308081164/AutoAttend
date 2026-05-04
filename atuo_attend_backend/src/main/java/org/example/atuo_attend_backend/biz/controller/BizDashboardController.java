package org.example.atuo_attend_backend.biz.controller;

import org.example.atuo_attend_backend.biz.mapper.CustomerMapper;
import org.example.atuo_attend_backend.biz.mapper.OpportunityMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/biz/dashboard")
public class BizDashboardController {

    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;
    private final QuoteProjectMapper quoteProjectMapper;

    public BizDashboardController(CustomerMapper customerMapper,
                                  OpportunityMapper opportunityMapper,
                                  QuoteProjectMapper quoteProjectMapper) {
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
        this.quoteProjectMapper = quoteProjectMapper;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        long tid = tid();
        long customerCount = customerMapper.countAll(tid);
        long opportunityCount = opportunityMapper.countAll(tid);
        long quoteCount = quoteProjectMapper.countAll(tid);
        List<Map<String, Object>> stageGroups = opportunityMapper.groupByStage(tid);

        // 计算各阶段金额
        double totalPipeline = 0;
        double wonAmount = 0;
        for (Map<String, Object> row : stageGroups) {
            Object amt = row.get("totalAmount");
            double v = amt instanceof Number ? ((Number) amt).doubleValue() : 0;
            String stage = (String) row.get("stage");
            if ("closed_won".equals(stage)) {
                wonAmount += v;
            } else {
                totalPipeline += v;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("customerCount", customerCount);
        data.put("opportunityCount", opportunityCount);
        data.put("quoteCount", quoteCount);
        data.put("totalPipeline", totalPipeline);
        data.put("wonAmount", wonAmount);
        data.put("stageGroups", stageGroups);
        return ApiResponse.ok(data);
    }
}
