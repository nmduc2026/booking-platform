package com.ndz.venue_service.repository;

import com.ndz.venue_service.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByShopIdOrderByNameAsc(UUID shopId);

    Optional<Resource> findByIdAndShopId(UUID id, UUID shopId);
}
