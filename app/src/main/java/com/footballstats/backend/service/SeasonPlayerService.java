package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.SeasonPlayerRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SeasonPlayerService {

    private final SeasonPlayerRepository seasonPlayerRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final SeasonTeamRepository seasonTeamRepository;

    public SeasonPlayerService(
        SeasonPlayerRepository seasonPlayerRepository,
        SeasonRepository seasonRepository,
        TeamRepository teamRepository,
        PlayerRepository playerRepository,
        PlayerTeamRepository playerTeamRepository,
        SeasonTeamRepository seasonTeamRepository
    ) {
        this.seasonPlayerRepository = seasonPlayerRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.seasonTeamRepository = seasonTeamRepository;
    }

    @Transactional(readOnly = true)
    public List<Season> listAvailableSeasonsForTeam(Long teamId) {
        return seasonTeamRepository.findAllByTeamIdOrderBySeasonCreatedAtDesc(teamId).stream()
            .map(item -> item.getSeason())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SeasonPlayer> listActiveSeasonPlayers(Long teamId, Long seasonId) {
        validateSeasonMembership(teamId, seasonId);
        return seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, teamId);
    }

    @Transactional(readOnly = true)
    public List<PlayerTeam> listEligibleRosterMemberships(Long teamId, Long seasonId) {
        validateSeasonMembership(teamId, seasonId);
        return playerTeamRepository.findCurrentRosterByTeamIdAndSeasonId(teamId, seasonId);
    }

    @Transactional(readOnly = true)
    public List<Player> listAvailablePlayersForSeason(Long teamId, Long seasonId) {
        validateSeasonMembership(teamId, seasonId);
        return playerRepository.findActiveAvailableForSeasonAndNotInTeam(seasonId, teamId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getActivePlayerIds(Long teamId, Long seasonId) {
        return seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, teamId).stream()
            .map(item -> item.getPlayer().getId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public List<SeasonPlayer> listActiveSeasonAssignmentsForTeam(Long teamId) {
        return seasonPlayerRepository.findAllActiveDetailedByTeamId(teamId);
    }

    @Transactional(readOnly = true)
    public List<SeasonPlayer> listActiveSeasonAssignmentsForPlayer(Long teamId, Long playerId) {
        return seasonPlayerRepository.findAllActiveDetailedByTeamIdAndPlayerId(teamId, playerId);
    }

    @Transactional(readOnly = true)
    public long countActiveSeasonPlayers(Long teamId, Long seasonId) {
        return seasonPlayerRepository.countBySeason_IdAndTeam_IdAndActiveTrue(seasonId, teamId);
    }

    @Transactional
    public void replaceSeasonPlayers(Long teamId, Long seasonId, List<Long> playerIds, Long actorUserId) {
        validateSeasonMembership(teamId, seasonId);
        Set<Long> targetPlayerIds = new LinkedHashSet<>(playerIds == null ? List.of() : playerIds.stream().filter(id -> id != null).toList());

        validateRosterMembership(teamId, targetPlayerIds);
        validateSeasonUniqueness(teamId, seasonId, targetPlayerIds);

        List<SeasonPlayer> activeAssignments = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndActiveTrue(seasonId, teamId);
        OffsetDateTime now = OffsetDateTime.now();

        for (SeasonPlayer assignment : activeAssignments) {
            if (!targetPlayerIds.contains(assignment.getPlayer().getId())) {
                assignment.setActive(false);
                assignment.setUpdatedByUserId(actorUserId);
                assignment.setUpdatedAt(now);
                seasonPlayerRepository.save(assignment);
            }
        }

        for (Long playerId : targetPlayerIds) {
            activateSeasonPlayer(teamId, seasonId, playerId, actorUserId, now);
        }
    }

    @Transactional
    public void addSeasonPlayer(Long teamId, Long seasonId, Long playerId, Long actorUserId) {
        validateSeasonMembership(teamId, seasonId);
        validateRosterMembership(teamId, Set.of(playerId));
        validateSeasonUniqueness(teamId, seasonId, Set.of(playerId));
        activateSeasonPlayer(teamId, seasonId, playerId, actorUserId, OffsetDateTime.now());
    }

    @Transactional
    public void attachAvailablePlayerToTeamAndSeason(Long teamId, Long seasonId, Long playerId, Long actorUserId) {
        validateSeasonMembership(teamId, seasonId);
        validateSeasonUniqueness(teamId, seasonId, Set.of(playerId));
        playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));
        activateSeasonPlayer(teamId, seasonId, playerId, actorUserId, OffsetDateTime.now());
    }

    @Transactional
    public void removeSeasonPlayer(Long teamId, Long seasonId, Long playerId, Long actorUserId) {
        SeasonPlayer seasonPlayer = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(seasonId, teamId, playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден в заявке сезона."));
        seasonPlayer.setActive(false);
        seasonPlayer.setUpdatedByUserId(actorUserId);
        seasonPlayer.setUpdatedAt(OffsetDateTime.now());
        seasonPlayerRepository.save(seasonPlayer);
    }

    @Transactional
    public void deactivateSeasonPlayersForRemovedTeams(Long seasonId, Set<Long> activeTeamIds, Long actorUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<SeasonTeamRepository.TeamSeasonProjection> existingTeams = seasonTeamRepository.findTeamIdsBySeasonId(seasonId);
        Set<Long> currentTeamIds = existingTeams.stream().map(SeasonTeamRepository.TeamSeasonProjection::teamId).collect(java.util.stream.Collectors.toSet());
        currentTeamIds.removeAll(activeTeamIds);
        if (currentTeamIds.isEmpty()) {
            return;
        }

        for (Long removedTeamId : currentTeamIds) {
            List<SeasonPlayer> assignments = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndActiveTrue(seasonId, removedTeamId);
            for (SeasonPlayer assignment : assignments) {
                assignment.setActive(false);
                assignment.setUpdatedByUserId(actorUserId);
                assignment.setUpdatedAt(now);
                seasonPlayerRepository.save(assignment);
            }
        }
    }

    private void activateSeasonPlayer(Long teamId, Long seasonId, Long playerId, Long actorUserId, OffsetDateTime now) {
        validateSeasonUniqueness(teamId, seasonId, Set.of(playerId));

        SeasonPlayer existing = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(seasonId, teamId, playerId).orElse(null);
        if (existing != null) {
            existing.setUpdatedByUserId(actorUserId);
            existing.setUpdatedAt(now);
            seasonPlayerRepository.save(existing);
            return;
        }

        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));

        SeasonPlayer seasonPlayer = new SeasonPlayer();
        seasonPlayer.setSeason(season);
        seasonPlayer.setTeam(team);
        seasonPlayer.setPlayer(player);
        seasonPlayer.setCreatedByUserId(actorUserId);
        seasonPlayer.setUpdatedByUserId(actorUserId);
        seasonPlayer.setCreatedAt(now);
        seasonPlayer.setUpdatedAt(now);
        seasonPlayer.setActive(true);
        seasonPlayerRepository.save(seasonPlayer);
    }

    private void validateSeasonMembership(Long teamId, Long seasonId) {
        if (!seasonTeamRepository.existsBySeason_IdAndTeam_Id(seasonId, teamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда не участвует в выбранном сезоне.");
        }
    }

    private void validateRosterMembership(Long teamId, Set<Long> playerIds) {
        if (playerIds.isEmpty()) {
            return;
        }
        Set<Long> rosterPlayerIds = playerTeamRepository.findCurrentRosterByTeamId(teamId).stream()
            .map(item -> item.getPlayer().getId())
            .collect(java.util.stream.Collectors.toSet());
        for (Long playerId : playerIds) {
            if (!rosterPlayerIds.contains(playerId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "В заявку сезона можно включать только игроков текущего состава команды.");
            }
        }
    }

    private void validateSeasonUniqueness(Long teamId, Long seasonId, Set<Long> playerIds) {
        if (playerIds.isEmpty()) {
            return;
        }
        for (Long playerId : playerIds) {
            SeasonPlayer existing = seasonPlayerRepository.findBySeason_IdAndPlayer_IdAndActiveTrue(seasonId, playerId).orElse(null);
            if (existing != null && !existing.getTeam().getId().equals(teamId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Игрок уже заявлен за другую команду в этом сезоне.");
            }
        }
    }
}