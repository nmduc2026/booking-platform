package com.ndz.auth_service.config;

import com.ndz.auth_service.entity.Role;
import com.ndz.auth_service.entity.User;
import com.ndz.auth_service.entity.UserStatus;
import com.ndz.auth_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(
            BootstrapAdminProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(properties.email())) {
            return;
        }

        User admin = new User();
        admin.setEmail(properties.email().trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(properties.password()));
        admin.setFullName(properties.fullName());
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        log.info("Bootstrap admin created: {}", properties.email());
    }
}
