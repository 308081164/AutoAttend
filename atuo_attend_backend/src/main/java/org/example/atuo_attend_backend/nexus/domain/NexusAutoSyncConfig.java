package org.example.atuo_attend_backend.nexus.domain;

public class NexusAutoSyncConfig {
    private Long tenantId;
    private boolean enabled;
    private int globalIntervalSeconds;
    private int cpuPeriodSeconds;
    private int cpuWindowMinutes;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getGlobalIntervalSeconds() {
        return globalIntervalSeconds;
    }

    public void setGlobalIntervalSeconds(int globalIntervalSeconds) {
        this.globalIntervalSeconds = globalIntervalSeconds;
    }

    public int getCpuPeriodSeconds() {
        return cpuPeriodSeconds;
    }

    public void setCpuPeriodSeconds(int cpuPeriodSeconds) {
        this.cpuPeriodSeconds = cpuPeriodSeconds;
    }

    public int getCpuWindowMinutes() {
        return cpuWindowMinutes;
    }

    public void setCpuWindowMinutes(int cpuWindowMinutes) {
        this.cpuWindowMinutes = cpuWindowMinutes;
    }
}

