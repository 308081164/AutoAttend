package org.example.atuo_attend_backend.prototype.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Penpot 集成（方案 A：服务端 Access Token 或密码登录后持 Cookie 调 RPC）。
 */
@ConfigurationProperties(prefix = "app.penpot")
public class PenpotProperties {

    /** 为 false 时不调用 Penpot，快原型页隐藏 Beta 入口 */
    private boolean enabled = false;

    /**
     * 容器内访问 Penpot 前端（Nginx 反代 /api 至后端），RPC 默认走此地址。
     * 若需与「仅浏览器可访问的外网地址」分离，可再配 {@link #apiBaseUrl}。
     */
    private String baseUrl = "http://penpot-frontend:80";

    /**
     * 可选：仅用于后端 RPC 的基址（与 {@link #baseUrl} 二选一逻辑见 getter）。
     * 不配置时与 baseUrl 相同。
     */
    private String apiBaseUrl = "";

    /**
     * 浏览器打开工作区 /workspace/... 的基址（与容器内 RPC 地址可以不同）。
     * 可不配置：此时预览链接退化为 {@link #getEffectiveBrowserBaseUrl()}（多为内网地址，仅供运维调试）。
     */
    private String publicUri = "";

    /**
     * 个人访问令牌（推荐）：在 Penpot 网页 设置 → Access tokens 生成，配置在部署环境变量。
     * 与邮箱密码二选一；优先使用本字段。
     */
    private String accessToken = "";

    /** 备选：内置账号邮箱（与 password 同时配置时，启动时登录获取 Cookie） */
    private String email = "";

    /** 备选：内置账号密码 */
    private String password = "";

    /**
     * 租户自动开户时生成的邮箱域（仅标签，需与 Penpot 实例允许的邮箱格式一致；自托管可任意后缀）。
     */
    private String tenantEmailDomain = "penpot.local";

    /** 租户 Access Token 名称前缀（create-access-token 的 name） */
    private String tenantTokenNamePrefix = "autoattend";

    /**
     * 派生租户 Penpot 登录密码时的盐（与 tenantId 组合哈希）；生产务必通过环境变量覆盖。
     */
    private String tenantCredentialPepper = "";

    /**
     * Penpot 2.12+ RPC 路径为 /api/main/methods/；旧版为 /api/rpc/command/。
     * auto：先尝试新路径，遇 HTTP 404 再回退旧路径并缓存。
     */
    private String rpcPathStyle = "auto";

    /**
     * 与官方 Penpot 前端一致：RPC 需带 {@code x-client}（启用 client-header-check 时）。
     */
    private String clientHeader = "penpot-backend";

    /**
     * 直连 Penpot 后端 JVM（官方镜像默认端口 6060），与 {@code penpot-frontend:80} 上 Nginx 反代的 /api 等价。
     * 当 {@link #getEffectiveRpcBaseUrl()} 误指向本应用或其它无 Penpot RPC 的地址时，客户端会再尝试此地址。
     */
    private String backendDirectUri = "http://penpot-backend:6060";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    /** 调用 /api/rpc 的基址 */
    public String getEffectiveRpcBaseUrl() {
        if (apiBaseUrl != null && !apiBaseUrl.isBlank()) {
            return apiBaseUrl.trim();
        }
        return baseUrl != null ? baseUrl.trim() : "";
    }

    public String getPublicUri() {
        return publicUri;
    }

    public void setPublicUri(String publicUri) {
        this.publicUri = publicUri;
    }

    /** 生成用户可点击的 workspace 链接时的基址 */
    public String getEffectiveBrowserBaseUrl() {
        if (publicUri != null && !publicUri.isBlank()) {
            return publicUri.trim();
        }
        return getEffectiveRpcBaseUrl();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantEmailDomain() {
        return tenantEmailDomain;
    }

    public void setTenantEmailDomain(String tenantEmailDomain) {
        this.tenantEmailDomain = tenantEmailDomain;
    }

    public String getTenantTokenNamePrefix() {
        return tenantTokenNamePrefix;
    }

    public void setTenantTokenNamePrefix(String tenantTokenNamePrefix) {
        this.tenantTokenNamePrefix = tenantTokenNamePrefix;
    }

    public String getTenantCredentialPepper() {
        return tenantCredentialPepper;
    }

    public void setTenantCredentialPepper(String tenantCredentialPepper) {
        this.tenantCredentialPepper = tenantCredentialPepper;
    }

    public String getRpcPathStyle() {
        return rpcPathStyle;
    }

    public void setRpcPathStyle(String rpcPathStyle) {
        this.rpcPathStyle = rpcPathStyle;
    }

    public String getClientHeader() {
        return clientHeader;
    }

    public void setClientHeader(String clientHeader) {
        this.clientHeader = clientHeader;
    }

    public String getBackendDirectUri() {
        return backendDirectUri;
    }

    public void setBackendDirectUri(String backendDirectUri) {
        this.backendDirectUri = backendDirectUri;
    }
}
