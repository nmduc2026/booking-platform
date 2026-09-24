package com.ndz.venue_service.repository;

import com.ndz.venue_service.entity.Shop;
import com.ndz.venue_service.entity.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {

    List<Shop> findByStatusOrderByNameAsc(ShopStatus status);
}
