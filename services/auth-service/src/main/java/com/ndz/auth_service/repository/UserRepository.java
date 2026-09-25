package com.ndz.auth_service.repository;

import com.ndz.auth_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (
                :q IS NULL OR :q = '' OR
                LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<User> search(@Param("role") com.ndz.auth_service.entity.Role role,
                      @Param("q") String q,
                      Pageable pageable);
}
