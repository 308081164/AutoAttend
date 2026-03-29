package org.example.atuo_attend_backend.tenant.invite;

import org.example.atuo_attend_backend.tenant.domain.TenantInvite;
import org.example.atuo_attend_backend.tenant.mapper.TenantInviteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 协作成员邀请：生成令牌，受邀者凭令牌注册 {@link org.example.atuo_attend_backend.collab.service.CollabAuthService#registerByInvite}。
 */
@Service
public class TenantInviteService {

    private final TenantInviteMapper inviteMapper;

    public TenantInviteService(TenantInviteMapper inviteMapper) {
        this.inviteMapper = inviteMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantInvite createInvite(long tenantId, int maxUses, int expiresInDays, String note) {
        if (maxUses < 1) {
            maxUses = 1;
        }
        if (expiresInDays < 1) {
            expiresInDays = 7;
        }
        TenantInvite row = new TenantInvite();
        row.setTenantId(tenantId);
        row.setToken(UUID.randomUUID().toString().replace("-", ""));
        row.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        row.setMaxUses(maxUses);
        row.setUsedCount(0);
        row.setNote(note != null && !note.isBlank() ? note.trim() : null);
        inviteMapper.insert(row);
        return row;
    }

    public List<TenantInvite> listInvites(long tenantId) {
        return inviteMapper.listByTenant(tenantId);
    }
}
