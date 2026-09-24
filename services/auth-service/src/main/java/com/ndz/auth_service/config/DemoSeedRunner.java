package com.ndz.auth_service.config;

import com.ndz.auth_service.entity.Role;
import com.ndz.auth_service.entity.User;
import com.ndz.auth_service.entity.UserShopMapping;
import com.ndz.auth_service.entity.UserStatus;
import com.ndz.auth_service.repository.UserRepository;
import com.ndz.auth_service.repository.UserShopMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);

    private final DemoSeedProperties properties;
    private final UserRepository userRepository;
    private final UserShopMappingRepository mappingRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoSeedRunner(
            DemoSeedProperties properties,
            UserRepository userRepository,
            UserShopMappingRepository mappingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.mappingRepository = mappingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin = ensureUser(
                DemoSeedIds.ADMIN_ID,
                properties.adminEmail(),
                properties.adminPassword(),
                properties.adminFullName(),
                Role.ADMIN
        );
        User manager = ensureUser(
                DemoSeedIds.MANAGER_ID,
                properties.managerEmail(),
                properties.managerPassword(),
                properties.managerFullName(),
                Role.SHOP_MANAGER
        );
        ensureUser(
                DemoSeedIds.USER_ID,
                properties.userEmail(),
                properties.userPassword(),
                properties.userFullName(),
                Role.USER
        );

        if (!mappingRepository.existsByUserIdAndShopId(manager.getId(), DemoSeedIds.SHOP_ID)) {
            UserShopMapping mapping = new UserShopMapping();
            mapping.setUser(manager);
            mapping.setShopId(DemoSeedIds.SHOP_ID);
            mappingRepository.save(mapping);
        }

        log.info(
                "Demo seed ready — admin={} / manager={} / user={} / shopId={} (createdByAdmin logical={})",
                properties.adminEmail(),
                properties.managerEmail(),
                properties.userEmail(),
                DemoSeedIds.SHOP_ID,
                admin.getId()
        );
    }

    private User ensureUser(UUID id, String email, String password, String fullName, Role role) {
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(id);
                    user.setEmail(email.trim().toLowerCase());
                    user.setPasswordHash(passwordEncoder.encode(password));
                    user.setFullName(fullName);
                    user.setRole(role);
                    user.setStatus(UserStatus.ACTIVE);
                    User saved = userRepository.save(user);
                    log.info("Seeded {} user {}", role, saved.getEmail());
                    return saved;
                });
    }
}
