package org.example.atuo_attend_backend.tenant.referral;

import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.ReferralCommissionMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 被推荐租户首年内消费，推荐方获得 10% 分成（模拟订单金额）。
 */
@Service
public class ReferralCommissionService {

    private static final double RATE = 0.10;

    private final TenantMapper tenantMapper;
    private final ReferralCommissionMapper commissionMapper;

    public ReferralCommissionService(TenantMapper tenantMapper,
                                     ReferralCommissionMapper commissionMapper) {
        this.tenantMapper = tenantMapper;
        this.commissionMapper = commissionMapper;
    }

    @Transactional
    public void onMockPurchase(long sourceTenantId, Long subscriptionOrderId, int amountCents) {
        if (amountCents <= 0) {
            return;
        }
        Tenant t = tenantMapper.findById(sourceTenantId);
        if (t == null || t.getReferrerTenantId() == null) {
            return;
        }
        long ref = t.getReferrerTenantId();
        if (ref == sourceTenantId) {
            return;
        }
        LocalDateTime created = t.getCreatedAt();
        if (created == null) {
            return;
        }
        LocalDateTime yearEnd = created.plusYears(1);
        if (LocalDateTime.now().isAfter(yearEnd)) {
            return;
        }
        int commission = (int) Math.round(amountCents * RATE);
        if (commission <= 0) {
            return;
        }
        commissionMapper.insert(ref, sourceTenantId, amountCents, commission, subscriptionOrderId);
        tenantMapper.addMemberPoints(ref, commission);
    }
}
