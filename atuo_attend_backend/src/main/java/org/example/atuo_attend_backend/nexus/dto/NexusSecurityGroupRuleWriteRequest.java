package org.example.atuo_attend_backend.nexus.dto;

/**
 * 新增或修改安全组规则（入/出方向由 direction 指定）。
 */
public class NexusSecurityGroupRuleWriteRequest {

    /** ingress | egress */
    private String direction;
    private String ipProtocol;
    private String portRange;
    /** 入方向：授权来源；出方向：可为空 */
    private String sourceCidrIp;
    /** 出方向：授权目标；入方向可为空 */
    private String destCidrIp;
    /** accept | drop，默认 accept */
    private String policy;
    private String priority;
    /** internet | intranet，默认 internet */
    private String nicType;
    private String description;

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getIpProtocol() {
        return ipProtocol;
    }

    public void setIpProtocol(String ipProtocol) {
        this.ipProtocol = ipProtocol;
    }

    public String getPortRange() {
        return portRange;
    }

    public void setPortRange(String portRange) {
        this.portRange = portRange;
    }

    public String getSourceCidrIp() {
        return sourceCidrIp;
    }

    public void setSourceCidrIp(String sourceCidrIp) {
        this.sourceCidrIp = sourceCidrIp;
    }

    public String getDestCidrIp() {
        return destCidrIp;
    }

    public void setDestCidrIp(String destCidrIp) {
        this.destCidrIp = destCidrIp;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNicType() {
        return nicType;
    }

    public void setNicType(String nicType) {
        this.nicType = nicType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
