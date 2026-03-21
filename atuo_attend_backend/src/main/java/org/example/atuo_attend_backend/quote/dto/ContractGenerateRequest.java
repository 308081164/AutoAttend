package org.example.atuo_attend_backend.quote.dto;

public class ContractGenerateRequest {
    private String clientName;
    private String companyName;
    /** software_dev | maintenance */
    private String templateType = "software_dev";

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
}
