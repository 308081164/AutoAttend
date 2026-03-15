package org.example.atuo_attend_backend.admin.dto;

public class AdminLoginResponse {

    private String token;
    private long expiresIn;
    /** 协作模块 JWT，管理员登录时一并下发，访问项目协作无需再次登录 */
    private String collabToken;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getCollabToken() {
        return collabToken;
    }

    public void setCollabToken(String collabToken) {
        this.collabToken = collabToken;
    }
}

