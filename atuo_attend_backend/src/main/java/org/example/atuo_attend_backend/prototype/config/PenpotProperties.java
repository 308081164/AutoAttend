package org.example.atuo_attend_backend.prototype.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Penpot 集成（方案 A：服务端 Access Token 或密码登录后持 Cookie 调 RPC）。
 */
@ConfigurationProperties(prefix = "app.penpot")
public class PenpotProperties {

    /** 为 false 时不调用 Penpot，快原型页隐藏 Beta 入口 */
    private boolean enabled = false;

    /** 容器内访问 Penpot 前端（Nginx 反代 /api 至后端），如 http://penpot-frontend:8080 */
    private String baseUrl = "http://penpot-frontend:8080";

    /** 浏览器打开工作区的基址，用于拼接 /workspace/{projectId}/{fileId} */
    private String publicUri = "http://localhost:9001";

    /**
     * 个人访问令牌（推荐）：在 Penpot 网页 设置 → Access tokens 生成，配置在部署环境变量。
     * 与邮箱密码二选一；优先使用本字段。
     */
    private String accessToken = "";

    /** 备选：内置账号邮箱（与 password 同时配置时，启动时登录获取 Cookie） */
    private String email = "";

    /** 备选：内置账号密码 */
    private String password = "";

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

    public String getPublicUri() {
        return publicUri;
    }

    public void setPublicUri(String publicUri) {
        this.publicUri = publicUri;
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
}
