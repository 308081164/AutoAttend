package org.example.atuo_attend_backend.biz.controller;

import org.example.atuo_attend_backend.biz.domain.Customer;
import org.example.atuo_attend_backend.biz.domain.CustomerFollowup;
import org.example.atuo_attend_backend.biz.mapper.CustomerFollowupMapper;
import org.example.atuo_attend_backend.biz.mapper.CustomerMapper;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/biz/customers")
public class CustomerController {

    private final CustomerMapper customerMapper;
    private final CustomerFollowupMapper followupMapper;
    private final QuoteProjectMapper quoteProjectMapper;

    public CustomerController(CustomerMapper customerMapper, CustomerFollowupMapper followupMapper,
                              QuoteProjectMapper quoteProjectMapper) {
        this.customerMapper = customerMapper;
        this.followupMapper = followupMapper;
        this.quoteProjectMapper = quoteProjectMapper;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Customer body) {
        body.setTenantId(tid());
        body.setStage(body.getStage() != null ? body.getStage() : "lead");
        body.setLastContactedAt(LocalDateTime.now());
        customerMapper.insert(body);
        Map<String, Object> data = new HashMap<>();
        data.put("id", body.getId());
        return ApiResponse.ok(data);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody Customer body) {
        body.setId(id);
        body.setTenantId(tid());
        customerMapper.update(body);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> get(@PathVariable long id) {
        Customer c = customerMapper.findById(tid(), id);
        return c != null ? ApiResponse.ok(c) : ApiResponse.error(404, "客户不存在");
    }

    /** 该客户关联的报价项目（用于客户详情「报价历史」） */
    @GetMapping("/{customerId}/quote-projects")
    public ApiResponse<List<Map<String, Object>>> listQuoteProjects(@PathVariable long customerId) {
        if (customerMapper.findById(tid(), customerId) == null) {
            return ApiResponse.error(404, "客户不存在");
        }
        List<QuoteProject> list = quoteProjectMapper.listByCustomerId(tid(), customerId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QuoteProject p : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("status", p.getStatus());
            m.put("updatedAt", p.getUpdatedAt());
            rows.add(m);
        }
        return ApiResponse.ok(rows);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) String keyword) {
        int offset = (page - 1) * pageSize;
        long total;
        List<Customer> items;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            items = customerMapper.search(tid(), kw, offset, pageSize);
            total = customerMapper.countSearch(tid(), kw);
        } else {
            items = customerMapper.listPaged(tid(), offset, pageSize);
            total = customerMapper.countAll(tid());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return ApiResponse.ok(data);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        customerMapper.deleteById(tid(), id);
        return ApiResponse.ok(null);
    }

    // ---- 跟进记录 ----

    @PostMapping("/{customerId}/followups")
    public ApiResponse<Map<String, Object>> addFollowup(@PathVariable long customerId, @RequestBody CustomerFollowup body) {
        body.setTenantId(tid());
        body.setCustomerId(customerId);
        followupMapper.insert(body);
        Map<String, Object> data = new HashMap<>();
        data.put("id", body.getId());
        return ApiResponse.ok(data);
    }

    @GetMapping("/{customerId}/followups")
    public ApiResponse<Map<String, Object>> listFollowups(@PathVariable long customerId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<CustomerFollowup> items = followupMapper.listByCustomer(tid(), customerId, offset, pageSize);
        long total = followupMapper.countByCustomer(tid(), customerId);
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }
}
