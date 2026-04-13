package com.footballstats.backend.repository;

import com.footballstats.backend.domain.UserTeamScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTeamScopeRepository extends JpaRepository<UserTeamScope, Long> {

    List<UserTeamScope> findByUser_IdAndActiveTrue(Long userId);

    Optional<UserTeamScope> findByUser_IdAndTeam_IdAndActiveTrue(Long userId, Long teamId);
}
