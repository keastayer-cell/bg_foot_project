package com.footballstats.backend.service;

import com.footballstats.backend.domain.*;
import com.footballstats.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FavoriteService {
    private final UserFavoriteRepository repository;
    private final AppUserRepository users;
    private final TeamRepository teams;
    private final PlayerRepository players;

    public FavoriteService(UserFavoriteRepository repository, AppUserRepository users, TeamRepository teams, PlayerRepository players) {
        this.repository=repository; this.users=users; this.teams=teams; this.players=players;
    }

    @Transactional(readOnly=true)
    public List<FavoriteData> list(Long userId) {
        return repository.findByUser_IdOrderByCreatedAtDesc(userId).stream().map(this::toData).toList();
    }

    @Transactional
    public FavoriteData add(Long userId, FavoriteType type, Long targetId) {
        validateTarget(type, targetId);
        UserFavorite favorite = repository.findByUser_IdAndTargetTypeAndTargetId(userId, type, targetId)
            .orElseGet(() -> {
                UserFavorite value = new UserFavorite();
                value.setUser(users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Пользователь не найден.")));
                value.setTargetType(type); value.setTargetId(targetId);
                return repository.save(value);
            });
        return toData(favorite);
    }

    @Transactional
    public void remove(Long userId, FavoriteType type, Long targetId) {
        repository.findByUser_IdAndTargetTypeAndTargetId(userId, type, targetId).ifPresent(repository::delete);
    }

    private void validateTarget(FavoriteType type, Long id) {
        if (type == null || id == null || id <= 0) throw new IllegalArgumentException("Некорректный объект избранного.");
        boolean exists = type == FavoriteType.TEAM ? teams.existsById(id) : players.existsById(id);
        if (!exists) throw new IllegalArgumentException("Объект избранного не найден.");
    }

    private FavoriteData toData(UserFavorite value) {
        String name = value.getTargetType() == FavoriteType.TEAM
            ? teams.findById(value.getTargetId()).map(Team::getName).orElse("Команда удалена")
            : players.findById(value.getTargetId()).map(Player::getFullName).orElse("Игрок удалён");
        return new FavoriteData(value.getTargetType(), value.getTargetId(), name);
    }

    public record FavoriteData(FavoriteType type, Long targetId, String name) {}
}
