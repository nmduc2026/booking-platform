package com.ndz.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String email,
        String password,
        String fullName
) {
}
