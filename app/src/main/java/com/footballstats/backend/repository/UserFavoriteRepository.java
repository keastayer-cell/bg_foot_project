package com.footballstats.backend.repository;

import com.footballstats.backend.domain.FavoriteType;
import com.footballstats.backend.domain.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    List<UserFavorite> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<UserFavorite> findByUser_IdAndTargetTypeAndTargetId(Long userId, FavoriteType type, Long targetId);
    List<UserFavorite> findByTargetTypeAndTargetId(FavoriteType type, Long targetId);
}
