package com.footballstats.backend.repository;

import com.footballstats.backend.domain.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByPasswordResetTokenHash(String passwordResetTokenHash);

    @Query("""
        SELECT u
        FROM AppUser u
        WHERE (:name IS NULL OR lower(u.name) LIKE :name)
          AND (:email IS NULL OR lower(u.email) LIKE :email)
        ORDER BY u.email
        """)
    Page<AppUser> searchUsers(
        @Param("name") String name,
        @Param("email") String email,
        Pageable pageable
    );

    @Query("""
        SELECT DISTINCT u
        FROM AppUser u
        JOIN UserRole ur ON ur.user = u
        WHERE ur.active = true
          AND ur.role.code = :roleCode
          AND (:name IS NULL OR lower(u.name) LIKE :name)
          AND (:email IS NULL OR lower(u.email) LIKE :email)
        ORDER BY u.email
        """)
    Page<AppUser> searchUsersByRole(
        @Param("name") String name,
        @Param("email") String email,
        @Param("roleCode") com.footballstats.backend.domain.RoleCode roleCode,
        Pageable pageable
    );
}
