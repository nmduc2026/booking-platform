package com.ndz.auth_service.service;

import com.ndz.auth_service.dto.AdminCreateUserRequest;
import com.ndz.auth_service.dto.PageResponse;
import com.ndz.auth_service.dto.UserResponse;
import com.ndz.auth_service.dto.UserShopMappingRequest;
import com.ndz.auth_service.dto.UserShopMappingResponse;
import com.ndz.auth_service.entity.Role;
import com.ndz.auth_service.entity.User;
import com.ndz.auth_service.entity.UserShopMapping;
import com.ndz.auth_service.entity.UserStatus;
import com.ndz.auth_service.exception.ApiException;
import com.ndz.auth_service.repository.UserRepository;
import com.ndz.auth_service.repository.UserShopMappingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserShopMappingRepository userShopMappingRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            UserShopMappingRepository userShopMappingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userShopMappingRepository = userShopMappingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(int page, int size, Role role, String q) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = userRepository.search(
                role,
                blankToNull(q),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(result.map(user -> UserResponse.from(user, shopIdsOf(user.getId()))));
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
        return UserResponse.from(user, List.of());
    }

    @Transactional
    public UserShopMappingResponse assignShop(UserShopMappingRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.SHOP_MANAGER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only SHOP_MANAGER can be assigned to a shop");
        }

        if (userShopMappingRepository.existsByUserIdAndShopId(request.userId(), request.shopId())) {
            throw new ApiException(HttpStatus.CONFLICT, "User already assigned to this shop");
        }

        UserShopMapping mapping = new UserShopMapping();
        mapping.setUser(user);
        mapping.setShopId(request.shopId());
        userShopMappingRepository.save(mapping);

        return new UserShopMappingResponse(mapping.getId(), user.getId(), mapping.getShopId());
    }

    private List<UUID> shopIdsOf(UUID userId) {
        return userShopMappingRepository.findByUserId(userId).stream()
                .map(UserShopMapping::getShopId)
                .toList();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
