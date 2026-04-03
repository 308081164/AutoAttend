package org.example.atuo_attend_backend.platform.service;

import org.example.atuo_attend_backend.platform.mapper.PlatformComponentEventMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class PlatformComponentEventService {

    private final PlatformComponentEventMapper mapper;

    public PlatformComponentEventService(PlatformComponentEventMapper mapper) {
        this.mapper = mapper;
    }

    private long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public void recordClick(Long adminUserId, String adminPhone, String componentKey, String coreApiKey) {
        record(adminUserId, adminPhone, componentKey, coreApiKey, "click");
    }

    public void recordUsage(Long adminUserId, String adminPhone, String componentKey, String coreApiKey) {
        record(adminUserId, adminPhone, componentKey, coreApiKey, "usage");
    }

    private void record(Long adminUserId,
                        String adminPhone,
                        String componentKey,
                        String coreApiKey,
                        String eventType) {
        if (componentKey == null || componentKey.isBlank()) return;
        mapper.insert(
                tid(),
                adminUserId,
                adminPhone,
                componentKey.trim(),
                coreApiKey != null && !coreApiKey.isBlank() ? coreApiKey.trim() : null,
                eventType
        );
    }
}

