package com.footballstats.backend.repository;

import com.footballstats.backend.domain.RefreshTokenSession;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {

    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenSession> findLockedByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE RefreshTokenSession session
        SET session.revokedAt = :revokedAt
        WHERE session.user.id = :userId
          AND session.revokedAt IS NULL
        """)
    int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("revokedAt") OffsetDateTime revokedAt);
}
