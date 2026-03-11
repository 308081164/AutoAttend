package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.admin.model.AdminUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminAuthService {

    @Value("${autoattend.admin.username:admin}")
    private String adminUsername;

    @Value("${autoattend.admin.password:admin123}")
    private String adminPassword;

    public String login(String username, String password) {
        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            return null;
        }
        return UUID.randomUUID().toString();
    }

    public AdminUser currentAdmin() {
        return new AdminUser(1L, adminUsername, "ADMIN");
    }
}

