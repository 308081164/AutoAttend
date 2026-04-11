package org.example.atuo_attend_backend.nexus.dto;

public class NexusSyncConfigUpdateRequest {
    private Boolean enabled;
    private Integer globalIntervalSeconds;
    private Integer cpuPeriodSeconds;
    private Integer cpuWindowMinutes;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getGlobalIntervalSeconds() {
        return globalIntervalSeconds;
    }

    public void setGlobalIntervalSeconds(Integer globalIntervalSeconds) {
        this.globalIntervalSeconds = globalIntervalSeconds;
    }

    public Integer getCpuPeriodSeconds() {
        return cpuPeriodSeconds;
    }

    public void setCpuPeriodSeconds(Integer cpuPeriodSeconds) {
        this.cpuPeriodSeconds = cpuPeriodSeconds;
    }

    public Integer getCpuWindowMinutes() {
        return cpuWindowMinutes;
    }

    public void setCpuWindowMinutes(Integer cpuWindowMinutes) {
        this.cpuWindowMinutes = cpuWindowMinutes;
    }
}
