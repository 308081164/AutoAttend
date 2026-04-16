package org.example.atuo_attend_backend.nexus.dto;

/**
 * 手工登记备案信息（阿里云备案控制台无稳定公开查询 API 时的 MVP）。
 */
public class NexusIcpSiteWriteRequest {
    private String domainName;
    private String siteName;
    private String icpLicense;
    private String statusText;
    private String remark;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getIcpLicense() {
        return icpLicense;
    }

    public void setIcpLicense(String icpLicense) {
        this.icpLicense = icpLicense;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
