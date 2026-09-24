package com.ndz.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record DemoSeedProperties(
        boolean enabled,
        String adminEmail,
        String adminPassword,
        String adminFullName,
        String managerEmail,
        String managerPassword,
        String managerFullName,
        String userEmail,
        String userPassword,
        String userFullName
) {
}
