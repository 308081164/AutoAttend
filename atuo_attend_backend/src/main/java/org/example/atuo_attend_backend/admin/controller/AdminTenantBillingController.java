package org.example.atuo_attend_backend.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.billing.TenantBillingService;
import org.example.atuo_attend_backend.tenant.domain.SubscriptionOrder;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.ReferralCommissionMapper;
import org.example.atuo_attend_backend.tenant.mapper.SubscriptionOrderMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.ai.official.OfficialAiPoolService;
import org.example.atuo_attend_backend.ai.service.OfficialAiTokenRedeemService;
import org.example.atuo_attend_backend.tenant.referral.InviteCodeService;
import org.example.atuo_attend_backend.tenant.referral.ReferralCommissionService;
import org.example.atuo_attend_backend.tenant.plan.TenantPlanCatalog;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租户侧：模拟支付开通会员（不跳转第三方支付）。
 */
@RestController
@RequestMapping("/api/admin/billing")
public class AdminTenantBillingController {

    private final TenantMapper tenantMapper;
    private final TenantBillingService tenantBillingService;
    private final SubscriptionOrderMapper subscriptionOrderMapper;
    private final TenantResourceQuotaService tenantResourceQuotaService;
    private final ReferralCommissionService referralCommissionService;
    private final InviteCodeService inviteCodeService;
    private final OfficialAiTokenRedeemService officialAiTokenRedeemService;
    private final OfficialAiPoolService officialAiPoolService;
    private final ReferralCommissionMapper referralCommissionMapper;

    public AdminTenantBillingController(TenantMapper tenantMapper,
                                        TenantBillingService tenantBillingService,
                                        SubscriptionOrderMapper subscriptionOrderMapper,
                                        TenantResourceQuotaService tenantResourceQuotaService,
                                        ReferralCommissionService referralCommissionService,
                                        InviteCodeService inviteCodeService,
                                        OfficialAiTokenRedeemService officialAiTokenRedeemService,
                                        OfficialAiPoolService officialAiPoolService,
                                        ReferralCommissionMapper referralCommissionMapper) {
        this.tenantMapper = tenantMapper;
        this.tenantBillingService = tenantBillingService;
        this.subscriptionOrderMapper = subscriptionOrderMapper;
        this.tenantResourceQuotaService = tenantResourceQuotaService;
        this.referralCommissionService = referralCommissionService;
        this.inviteCodeService = inviteCodeService;
        this.officialAiTokenRedeemService = officialAiTokenRedeemService;
        this.officialAiPoolService = officialAiPoolService;
        this.referralCommissionMapper = referralCommissionMapper;
    }

    private static long tenantId(HttpServletRequest request) {
        Object v = request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID);
        return v instanceof Long ? (Long) v : 0L;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        var t = tenantBillingService.ensureCurrent(tid);
        if (t == null) {
            return ApiResponse.error(40400, "租户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("planCode", t.getPlanCode());
        data.put("billingBaselinePlanCode", t.getBillingBaselinePlanCode());
        data.put("subscriptionEndsAt", t.getSubscriptionEndsAt());
        data.put("teamPriceCents", TenantBillingService.PRICE_TEAM_MONTHLY_CENTS);
        data.put("teamFirstMonthPriceCents", TenantBillingService.PRICE_TEAM_FIRST_MONTH_CENTS);
        data.put("memberPoints", t.getMemberPoints() != null ? t.getMemberPoints() : 0);
        data.put("referrerTenantId", t.getReferrerTenantId());
        data.put("inviteCodeRedeemed", Boolean.TRUE.equals(t.getInviteCodeRedeemed()));
        data.put("teamFirstMonthUsed", Boolean.TRUE.equals(t.getTeamFirstMonthUsed()));
        data.put("proAnnualPriceCents", TenantBillingService.PRICE_PRO_ANNUAL_CENTS);
        data.put("proPlusAnnualPriceCents", TenantBillingService.PRICE_PRO_PLUS_ANNUAL_CENTS);
        data.put("planDisplayLabel", TenantPlanCatalog.displayLabel(t.getPlanCode()));
        Map<String, Object> quotas = new HashMap<>();
        quotas.put("free", planQuotaMap(TenantPlanCatalog.FREE));
        quotas.put("team", planQuotaMap(TenantPlanCatalog.TEAM));
        quotas.put("pro", planQuotaMap(TenantPlanCatalog.PRO));
        quotas.put("pro_plus", planQuotaMap(TenantPlanCatalog.PRO_PLUS));
        data.put("planQuotas", quotas);
        var eff = tenantResourceQuotaService.planForTenant(tid);
        data.put("effectivePlan", planQuotaMap(eff));
        data.put("usage", tenantResourceQuotaService.usageSnapshot(tid));
        data.put("officialApiCnyBalance", t.getOfficialApiCnyBalance());
        data.put("officialAiPoolEnabled", officialAiPoolService.isFeatureEnabled());
        return ApiResponse.ok(data);
    }

    @PostMapping("/mock-pay")
    public ApiResponse<Map<String, Object>> mockPay(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        String plan = body != null && body.get("planCode") != null ? String.valueOf(body.get("planCode")) : null;
        boolean usePoints = body == null || body.get("useMemberPoints") == null
                || Boolean.parseBoolean(String.valueOf(body.get("useMemberPoints")));
        try {
            Tenant tBefore = tenantMapper.findById(tid);
            if (tBefore == null) {
                return ApiResponse.error(40400, "租户不存在");
            }
            String purchased = normalizePurchased(plan);
            int listCents = listPriceCentsForPurchase(tBefore, purchased);
            LocalDateTime ends = tenantBillingService.applyMockPurchase(tid, plan);
            int pointsUsed = 0;
            if (usePoints && listCents > 0) {
                int mp = tBefore.getMemberPoints() != null ? tBefore.getMemberPoints() : 0;
                pointsUsed = Math.min(mp, listCents);
                if (pointsUsed > 0) {
                    int n = tenantMapper.addMemberPoints(tid, -pointsUsed);
                    if (n <= 0) {
                        return ApiResponse.error(40000, "积分抵扣失败");
                    }
                }
            }
            Tenant t = tenantMapper.findById(tid);
            SubscriptionOrder ord = new SubscriptionOrder();
            ord.setTenantId(tid);
            ord.setPlanCode(purchased);
            ord.setAmountCents(listCents);
            ord.setCurrency("CNY");
            ord.setProvider("mock");
            ord.setStatus("completed");
            subscriptionOrderMapper.insertEntity(ord);
            Long orderId = ord.getId();
            referralCommissionService.onMockPurchase(tid, orderId, listCents);
            Map<String, Object> data = new HashMap<>();
            data.put("planCode", t != null ? t.getPlanCode() : plan);
            data.put("subscriptionEndsAt", ends);
            data.put("listPriceCents", listCents);
            data.put("pointsUsed", pointsUsed);
            String purchasedNorm = t != null ? t.getPlanCode() : purchased;
            String msg = "team".equalsIgnoreCase(purchasedNorm)
                    ? "模拟支付成功，已续期 30 天尝鲜版权益（无真实扣款）"
                    : "模拟支付成功，已续期 365 天专业档权益（按年费计价演示，无真实扣款）";
            data.put("message", msg);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    private int listPriceCentsForPurchase(Tenant t, String purchasedNorm) {
        if ("team".equalsIgnoreCase(purchasedNorm)) {
            return tenantBillingService.teamPurchaseListPriceCents(t);
        }
        return tenantBillingService.priceCentsForPlan(purchasedNorm);
    }

    @PostMapping("/official-api/redeem")
    public ApiResponse<Map<String, Object>> redeemOfficialApiCode(@RequestBody(required = false) Map<String, String> body,
                                                                    HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        String code = body != null ? body.get("code") : null;
        try {
            BigDecimal added = officialAiTokenRedeemService.redeemForTenant(tid, code != null ? code : "");
            Tenant t = tenantMapper.findById(tid);
            Map<String, Object> data = new HashMap<>();
            data.put("addedCny", added);
            data.put("officialApiCnyBalance", t != null ? t.getOfficialApiCnyBalance() : null);
            data.put("message", "兑换成功，官方 API 额度已入账");
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/invite/redeem")
    public ApiResponse<Map<String, Object>> redeemInvite(@RequestBody Map<String, String> body,
                                                         HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        String code = body != null ? body.get("inviteCode") : null;
        try {
            inviteCodeService.redeemFreeMonth(tid, code != null ? code : "");
            Tenant t = tenantMapper.findById(tid);
            Map<String, Object> data = new HashMap<>();
            data.put("planCode", t != null ? t.getPlanCode() : null);
            data.put("subscriptionEndsAt", t != null ? t.getSubscriptionEndsAt() : null);
            data.put("message", "兑换成功，已获赠 30 天尝鲜版权益");
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /**
     * 联合创始人：通过本租户邀请码注册成功的组织列表。
     */
    @GetMapping("/referral/invitees")
    public ApiResponse<Map<String, Object>> referralInvitees(HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        var rows = tenantMapper.listReferralInvitees(tid, 200);
        Map<String, Object> data = new HashMap<>();
        data.put("items", rows);
        return ApiResponse.ok(data);
    }

    /**
     * 邀请积分流水：被邀请方首年内模拟付费产生的分成（与 member_points 入账一致）。
     */
    @GetMapping("/referral/points-ledger")
    public ApiResponse<Map<String, Object>> referralPointsLedger(HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        var rows = referralCommissionMapper.listByReferrer(tid, 500);
        List<Map<String, Object>> items = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("sourceTenantId", r.getSourceTenantId());
            m.put("sourceTenantName", r.getSourceTenantName());
            m.put("orderAmountCents", r.getOrderAmountCents());
            m.put("commissionCents", r.getCommissionCents());
            m.put("subscriptionOrderId", r.getSubscriptionOrderId());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ApiResponse.ok(data);
    }

    @GetMapping("/invite/my-code")
    public ApiResponse<Map<String, Object>> myInviteCode(HttpServletRequest request) {
        long tid = tenantId(request);
        if (tid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        Object uidObj = request.getAttribute(AdminAuthFilter.ATTR_USER_ID);
        long uid = uidObj instanceof Long ? (Long) uidObj : 0L;
        if (uid <= 0) {
            return ApiResponse.error(40101, "未登录");
        }
        try {
            var inv = inviteCodeService.ensureUserInviteCode(tid, uid);
            Map<String, Object> m = new HashMap<>();
            m.put("code", inv.getCode());
            m.put("expiresAt", inv.getExpiresAt());
            return ApiResponse.ok(m);
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "生成失败");
        }
    }

    private static Map<String, Object> planQuotaMap(TenantPlanCatalog.TenantPlan p) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", p.code());
        m.put("label", p.label());
        m.put("maxMembers", p.maxMembers());
        m.put("maxGithubRepos", p.maxGithubRepos());
        m.put("maxQuoteProjects", p.maxQuoteProjects());
        m.put("maxClientBoardsEnabled", p.maxClientBoardsEnabled());
        m.put("maxAgentSessions", p.maxAgentSessions());
        m.put("maxCollabProjects", p.maxCollabProjects());
        m.put("maxNexusAccounts", p.maxNexusAccounts());
        return m;
    }

    private static String normalizePurchased(String plan) {
        if (plan == null || plan.isBlank()) {
            return TenantPlanCatalog.FREE.code();
        }
        String s = plan.trim().toLowerCase();
        if ("enterprise".equals(s)) {
            return TenantPlanCatalog.PRO_PLUS.code();
        }
        return TenantPlanCatalog.resolve(plan).code();
    }
}
