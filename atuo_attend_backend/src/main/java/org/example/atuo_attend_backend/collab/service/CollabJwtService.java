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

    public String createToken(long userId, String email, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
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
}
