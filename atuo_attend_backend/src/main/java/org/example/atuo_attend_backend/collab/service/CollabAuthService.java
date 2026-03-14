package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.springframework.stereotype.Service;

@Service
public class CollabAuthService {

    private final BizUserMapper userMapper;
    private final CollabPasswordService passwordService;
    private final CollabJwtService jwtService;

    public CollabAuthService(BizUserMapper userMapper,
                             CollabPasswordService passwordService,
                             CollabJwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    public String login(String email, String password) {
        if (email == null || email.isBlank()) return null;
        BizUser user = userMapper.findByEmail(email.trim());
        if (user == null || !passwordService.verify(password, user.getPasswordHash())) {
            return null;
        }
        return jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
    }

    public BizUser getCurrentUser(long userId) {
        return userMapper.findById(userId);
    }
}
