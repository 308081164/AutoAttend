package org.example.atuo_attend_backend.platform.service;

import org.example.atuo_attend_backend.platform.domain.PlatformSession;
import org.example.atuo_attend_backend.platform.mapper.PlatformSessionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PlatformAuthService {

    private static final long SESSION_SECONDS = 7200L;

    private final PlatformSessionMapper platformSessionMapper;

    @Value("${platform.ops.password:}")
    private String platformPassword;

    public PlatformAuthService(PlatformSessionMapper platformSessionMapper) {
        this.platformSessionMapper = platformSessionMapper;
    }

    /**
     * @return token，未配置平台密码或密码错误时返回 null
     */
    public String login(String password) {
        if (platformPassword == null || platformPassword.isEmpty()) {
            return null;
        }
        if (password == null || !platformPassword.equals(password)) {
            return null;
        }
        platformSessionMapper.deleteExpired();
        String token = UUID.randomUUID().toString();
        PlatformSession s = new PlatformSession();
        s.setToken(token);
        s.setExpiresAt(LocalDateTime.now().plusSeconds(SESSION_SECONDS));
        platformSessionMapper.insert(s);
        return token;
    }

    public long getExpiresInSeconds() {
        return SESSION_SECONDS;
    }
}
