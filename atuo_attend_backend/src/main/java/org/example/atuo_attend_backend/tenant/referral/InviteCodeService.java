package org.example.atuo_attend_backend.tenant.referral;

import org.example.atuo_attend_backend.tenant.domain.InviteCode;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.InviteCodeMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.example.atuo_attend_backend.tenant.billing.TenantBillingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 邀请码：平台发放（可设过期）、用户永久码、兑换尝鲜版一月。
 */
@Service
public class InviteCodeService {

    public static final String BY_PLATFORM = "platform";
    public static final String BY_USER = "user";

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LEN = 8;
    private final SecureRandom random = new SecureRandom();

    private final InviteCodeMapper inviteCodeMapper;
    private final TenantMapper tenantMapper;
    private final TenantBillingService tenantBillingService;

    public InviteCodeService(InviteCodeMapper inviteCodeMapper,
                             TenantMapper tenantMapper,
                             TenantBillingService tenantBillingService) {
        this.inviteCodeMapper = inviteCodeMapper;
        this.tenantMapper = tenantMapper;
        this.tenantBillingService = tenantBillingService;
    }

    public String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public InviteCode findValid(String raw) {
        String c = normalizeCode(raw);
        if (c.length() < 4) {
            return null;
        }
        InviteCode row = inviteCodeMapper.findActiveByCode(c);
        if (row == null) {
            return null;
        }
        if (Boolean.TRUE.equals(row.getDisabled())) {
            return null;
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return row;
    }

    @Transactional
    public InviteCode ensureUserInviteCode(long tenantId, long adminUserId) {
        InviteCode existing = inviteCodeMapper.findUserCodeForTenant(tenantId);
        if (existing != null) {
            return existing;
        }
        for (int i = 0; i < 20; i++) {
            String code = randomCode();
            try {
                InviteCode row = new InviteCode();
                row.setCode(code);
                row.setCreatedBy(BY_USER);
                row.setReferrerTenantId(tenantId);
                row.setCreatorUserId(adminUserId);
                row.setExpiresAt(null);
                row.setMaxUses(null); // 用户邀请码：永久有效、不限次数
                inviteCodeMapper.insert(row);
                return row;
            } catch (Exception ignored) {
                // duplicate code, retry
            }
        }
        throw new IllegalStateException("生成邀请码失败，请重试");
    }

    /**
     * 平台官方邀请码（与任何租户无关，类似全局激活码）：必须设置可使用次数；未传过期时间时默认自现在起 30 天。
     * {@code referrer_tenant_id} 存为 {@code null}。
     */
    @Transactional
    public InviteCode createGlobalPlatformCode(LocalDateTime expiresAt, Integer maxUses) {
        if (maxUses == null || maxUses < 1) {
            throw new IllegalArgumentException("官方邀请码必须设置可使用次数（≥1）");
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(30);
        }
        for (int i = 0; i < 20; i++) {
            String code = randomCode();
            try {
                InviteCode row = new InviteCode();
                row.setCode(code);
                row.setCreatedBy(BY_PLATFORM);
                row.setReferrerTenantId(null);
                row.setCreatorUserId(null);
                row.setExpiresAt(expiresAt);
                row.setMaxUses(maxUses);
                inviteCodeMapper.insert(row);
                return row;
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("生成邀请码失败");
    }

    public List<InviteCode> listGlobalPlatformCodes(int limit) {
        int lim = Math.min(Math.max(limit, 1), 200);
        return inviteCodeMapper.listGlobalPlatformCodes(lim);
    }

    /**
     * 注册成功后：绑定推荐关系并给推荐方 +1 积分（每成功注册一次）。
     */
    @Transactional
    public void onTenantRegisteredWithInvite(long newTenantId, String rawInviteCode) {
        InviteCode inv = findValid(rawInviteCode);
        if (inv == null) {
            return;
        }
        Long ref = inv.getReferrerTenantId();
        if (ref != null && ref == newTenantId) {
            return;
        }
        if (!tryConsumeUse(inv)) {
            return;
        }
        if (ref != null) {
            tenantMapper.updateReferrerIfNull(newTenantId, ref);
            tenantMapper.addMemberPoints(ref, 1);
        }
    }

    /**
     * 已登录租户兑换：免费 30 天尝鲜版（team），每租户限一次。
     */
    @Transactional
    public void redeemFreeMonth(long tenantId, String rawCode) {
        Tenant t = tenantMapper.findById(tenantId);
        if (t == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        if (Boolean.TRUE.equals(t.getInviteCodeRedeemed())) {
            throw new IllegalStateException("您已使用过邀请兑换，不可重复领取");
        }
        InviteCode inv = findValid(rawCode);
        if (inv == null) {
            throw new IllegalArgumentException("邀请码无效或已过期");
        }
        Long ref = inv.getReferrerTenantId();
        if (ref != null && ref == tenantId) {
            throw new IllegalArgumentException("不能使用自己的邀请码");
        }
        if (!tryConsumeUse(inv)) {
            throw new IllegalArgumentException("邀请码已用尽或暂时不可用，请稍后再试");
        }
        tenantBillingService.grantPlanWindow(tenantId, "team", 30);
        if (ref != null) {
            tenantMapper.updateReferrerIfNull(tenantId, ref);
            tenantMapper.addMemberPoints(ref, 1);
        }
        tenantMapper.markInviteRedeemed(tenantId);
    }

    /** 占用一次使用额度（与 DB 原子校验并发、过期、次数）。 */
    private boolean tryConsumeUse(InviteCode inv) {
        return inviteCodeMapper.tryIncrementUse(inv.getId()) > 0;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
