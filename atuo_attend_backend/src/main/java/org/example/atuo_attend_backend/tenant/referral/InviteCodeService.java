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
                inviteCodeMapper.insert(row);
                return row;
            } catch (Exception ignored) {
                // duplicate code, retry
            }
        }
        throw new IllegalStateException("生成邀请码失败，请重试");
    }

    @Transactional
    public InviteCode createPlatformCode(long referrerTenantId, LocalDateTime expiresAt) {
        for (int i = 0; i < 20; i++) {
            String code = randomCode();
            try {
                InviteCode row = new InviteCode();
                row.setCode(code);
                row.setCreatedBy(BY_PLATFORM);
                row.setReferrerTenantId(referrerTenantId);
                row.setCreatorUserId(null);
                row.setExpiresAt(expiresAt);
                inviteCodeMapper.insert(row);
                return row;
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("生成邀请码失败");
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
        long ref = inv.getReferrerTenantId();
        if (ref == newTenantId) {
            return;
        }
        tenantMapper.updateReferrerIfNull(newTenantId, ref);
        tenantMapper.addMemberPoints(ref, 1);
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
        long ref = inv.getReferrerTenantId();
        if (ref == tenantId) {
            throw new IllegalArgumentException("不能使用自己的邀请码");
        }
        tenantBillingService.grantPlanWindow(tenantId, "team", 30);
        tenantMapper.updateReferrerIfNull(tenantId, ref);
        tenantMapper.markInviteRedeemed(tenantId);
        tenantMapper.addMemberPoints(ref, 1);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
