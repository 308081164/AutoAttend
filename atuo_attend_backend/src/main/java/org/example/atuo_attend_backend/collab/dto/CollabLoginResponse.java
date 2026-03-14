package org.example.atuo_attend_backend.collab.dto;

public class CollabLoginResponse {
    private String token;
    private long expiresIn;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
