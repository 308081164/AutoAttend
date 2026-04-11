package org.example.atuo_attend_backend.nexus.domain;

import java.time.LocalDateTime;

public class NexusCloudInstance {
    private Long tenantId;
    private Long accountId;
    private String instanceId;
    private String instanceName;
    private String status;
    private String instanceType;
    private String zoneId;
    private String publicIp;
    private String privateIp;
    private String osName;
    private Long memoryMb;
    /** 宝塔面板地址（https://...），仅平台侧配置，云同步不覆盖 */
    private String btPanelUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public Long getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(Long memoryMb) {
        this.memoryMb = memoryMb;
    }

    public String getBtPanelUrl() {
        return btPanelUrl;
    }

    public void setBtPanelUrl(String btPanelUrl) {
        this.btPanelUrl = btPanelUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

