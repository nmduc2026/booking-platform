package com.ndz.venue_service.repository;

import com.ndz.venue_service.entity.Shop;
import com.ndz.venue_service.entity.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {

    List<Shop> findByStatusOrderByNameAsc(ShopStatus status);

    @Query("""
            SELECT s FROM Shop s
            WHERE (:status IS NULL OR s.status = :status)
              AND (
                :q IS NULL OR :q = '' OR
                LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(COALESCE(s.address, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Shop> search(@Param("status") ShopStatus status,
                      @Param("q") String q,
                      Pageable pageable);
}
