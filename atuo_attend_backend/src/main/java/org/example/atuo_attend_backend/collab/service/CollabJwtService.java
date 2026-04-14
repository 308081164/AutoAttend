package org.example.atuo_attend_backend.collab.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class CollabJwtService {

    private final SecretKey key;
    private final long expireMs;

    public CollabJwtService(@Value("${collab.jwt.secret}") String secret,
                            @Value("${collab.jwt.expireSeconds:7200}") long expireSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireSeconds * 1000;
    }

    public static final String JWT_MODE_ADMIN = "admin";
    public static final String JWT_MODE_MEMBER = "member";
    /** 仅本租户协作项目（管理员工作台项目管理） */
    public static final String PROJECT_SCOPE_TENANT = "tenant";
    /** 用户参与的全部租户项目（成员首页） */
    public static final String PROJECT_SCOPE_ALL = "all";

    public String createToken(long userId, String email, String role) {
        return createToken(userId, email, role, JWT_MODE_MEMBER, PROJECT_SCOPE_ALL);
    }

    /**
     * @param mode admin=管理员会话（仅 tenant 内协作）；member=成员会话
     * @param projectScope tenant=仅当前租户项目；all=跨租户参与项目
     */
    public String createToken(long userId, String email, String role, String mode, String projectScope) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("mode", mode != null ? mode : JWT_MODE_MEMBER)
                .claim("projectScope", projectScope != null ? projectScope : PROJECT_SCOPE_ALL)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims c = parseToken(token);
        if (c == null) return null;
        try {
            return Long.parseLong(c.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getModeFromToken(String token) {
        Claims c = parseToken(token);
        if (c == null) return null;
        Object m = c.get("mode");
        return m != null ? m.toString() : null;
    }

    public String getProjectScopeFromToken(String token) {
        Claims c = parseToken(token);
        if (c == null) return null;
        Object s = c.get("projectScope");
        return s != null ? s.toString() : null;
    }
}
