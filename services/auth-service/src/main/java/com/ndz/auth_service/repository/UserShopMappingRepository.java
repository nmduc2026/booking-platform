package com.ndz.auth_service.repository;

import com.ndz.auth_service.entity.UserShopMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserShopMappingRepository extends JpaRepository<UserShopMapping, UUID> {

    boolean existsByUserIdAndShopId(UUID userId, UUID shopId);

    List<UserShopMapping> findByUserId(UUID userId);
}
