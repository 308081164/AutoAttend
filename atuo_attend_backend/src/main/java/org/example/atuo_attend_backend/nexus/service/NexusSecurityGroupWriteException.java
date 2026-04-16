package org.example.atuo_attend_backend.nexus.service;

/**
 * 安全组写操作失败，携带业务错误码（与 {@link org.example.atuo_attend_backend.common.ApiResponse} 一致）。
 */
public class NexusSecurityGroupWriteException extends Exception {

    private final int businessCode;

    public NexusSecurityGroupWriteException(int businessCode, String message, Throwable cause) {
        super(message, cause);
        this.businessCode = businessCode;
    }

    public int getBusinessCode() {
        return businessCode;
    }
}
