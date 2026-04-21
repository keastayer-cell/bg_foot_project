package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.SeasonStatus;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    public ActiveSeasonAssignment getLatestActiveSeasonAssignment(Long playerId) {
        return seasonPlayerRepository.findAllActiveDetailedByPlayerId(playerId).stream()
            .filter(item -> item.getSeason().getStatus() == SeasonStatus.ACTIVE)
            .findFirst()
            .map(this::mapActiveSeasonAssignment)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, ActiveSeasonAssignment> getLatestActiveSeasonAssignments(List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ActiveSeasonAssignment> result = new LinkedHashMap<>();
        for (SeasonPlayer assignment : seasonPlayerRepository.findAllActiveDetailedByPlayerIds(playerIds)) {
            if (assignment.getSeason().getStatus() != SeasonStatus.ACTIVE) {
                continue;
            }
            result.putIfAbsent(assignment.getPlayer().getId(), mapActiveSeasonAssignment(assignment));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ActiveSeasonAssignment getBlockingActiveSeasonAssignment(Long playerId, Long targetTeamId) {
        return seasonPlayerRepository.findAllActiveDetailedByPlayerId(playerId).stream()
            .filter(item -> item.getSeason().getStatus() == SeasonStatus.ACTIVE)
            .filter(item -> !item.getTeam().getId().equals(targetTeamId))
            .findFirst()
            .map(this::mapActiveSeasonAssignment)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public long countActiveSeasonPlayers(Long teamId, Long seasonId) {
        return seasonPlayerRepository.countBySeason_IdAndTeam_IdAndActiveTrue(seasonId, teamId);
    }

    @Transactional(readOnly = true)
    public Season getSeasonForTeam(Long teamId, Long seasonId) {
        validateSeasonMembership(teamId, seasonId);
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
    }

    @Transactional(readOnly = true)
    public SeasonPlayer getActiveAssignmentForSeason(Long seasonId, Long playerId) {
        return seasonPlayerRepository.findBySeason_IdAndPlayer_IdAndActiveTrue(seasonId, playerId).orElse(null);
    }

    @Transactional
    public void replaceSeasonPlayers(Long teamId, Long seasonId, List<Long> playerIds, Long actorUserId) {
        Season season = requireSeasonForMutation(teamId, seasonId);
        Set<Long> targetPlayerIds = new LinkedHashSet<>(playerIds == null ? List.of() : playerIds.stream().filter(id -> id != null).toList());

        validateRosterMembership(teamId, targetPlayerIds);
        validateSeasonUniqueness(teamId, seasonId, targetPlayerIds);
        validateSeasonRosterCapacity(season, teamId, (long) targetPlayerIds.size(), null);

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
        requireSeasonForMutation(teamId, seasonId);
        validateRosterMembership(teamId, Set.of(playerId));
        validateSeasonUniqueness(teamId, seasonId, Set.of(playerId));
        activateSeasonPlayer(teamId, seasonId, playerId, actorUserId, OffsetDateTime.now());
    }

    @Transactional
    public void attachAvailablePlayerToTeamAndSeason(Long teamId, Long seasonId, Long playerId, Long actorUserId) {
        requireSeasonForMutation(teamId, seasonId);
        validateSeasonUniqueness(teamId, seasonId, Set.of(playerId));
        ensurePlayerAssignedToTeam(teamId, playerId, actorUserId);

        activateSeasonPlayer(teamId, seasonId, playerId, actorUserId, OffsetDateTime.now());
    }

    @Transactional
    public void ensurePlayerAssignedToTeam(Long teamId, Long playerId, Long actorUserId) {
        ActiveSeasonAssignment blockingAssignment = getBlockingActiveSeasonAssignment(playerId, teamId);
        if (blockingAssignment != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Игрок уже заявлен за команду «"
                    + blockingAssignment.teamName()
                    + "» в активном сезоне «"
                    + blockingAssignment.seasonName()
                    + "». Сначала уберите его из активной заявки."
            );
        }

        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));

        reassignPlayerToTeam(player, team, actorUserId, true);
    }

    @Transactional
    public void removeSeasonPlayer(Long teamId, Long seasonId, Long playerId, Long actorUserId) {
        requireSeasonForMutation(teamId, seasonId);
        SeasonPlayer seasonPlayer = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(seasonId, teamId, playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден в заявке сезона."));
        seasonPlayer.setActive(false);
        seasonPlayer.setUpdatedByUserId(actorUserId);
        seasonPlayer.setUpdatedAt(OffsetDateTime.now());
        seasonPlayerRepository.save(seasonPlayer);
    }

    @Transactional
    public void deactivateActiveAssignmentsForPlayerInTeam(Long teamId, Long playerId, Long actorUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        for (SeasonPlayer assignment : seasonPlayerRepository.findAllActiveDetailedByTeamIdAndPlayerId(teamId, playerId)) {
            assignment.setActive(false);
            assignment.setUpdatedByUserId(actorUserId);
            assignment.setUpdatedAt(now);
            seasonPlayerRepository.save(assignment);
        }
    }

    @Transactional
    public void deactivateSeasonPlayersForRemovedTeams(Long seasonId, Set<Long> activeTeamIds, Long actorUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<SeasonTeamRepository.TeamSeasonProjection> existingTeams = seasonTeamRepository.findTeamIdsBySeasonId(seasonId);
        Set<Long> currentTeamIds = existingTeams.stream().map(SeasonTeamRepository.TeamSeasonProjection::getTeamId).collect(java.util.stream.Collectors.toSet());
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

    @Transactional
    public void transferSeasonPlayer(Long seasonId, Long playerId, Long fromTeamId, Long toTeamId, Long actorUserId) {
        Season season = requireSeasonForMutation(toTeamId, seasonId);
        validateSeasonMembership(fromTeamId, seasonId);
        validateSeasonRosterCapacity(season, toTeamId, 1L, playerId);

        SeasonPlayer seasonPlayer = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(seasonId, fromTeamId, playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Игрок уже не находится в заявке указанной команды."));
        Team targetTeam = teamRepository.findById(toTeamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда назначения не найдена."));

        seasonPlayer.setTeam(targetTeam);
        seasonPlayer.setUpdatedByUserId(actorUserId);
        seasonPlayer.setUpdatedAt(OffsetDateTime.now());
        seasonPlayerRepository.save(seasonPlayer);

        reassignPlayerToTeam(seasonPlayer.getPlayer(), targetTeam, actorUserId, false);
    }

    @Transactional(readOnly = true)
    public void validateSeasonRosterCapacity(Season season, Long teamId, Long requestedSize, Long ignoredPlayerId) {
        if (season == null || season.getMaxRosterSize() == null) {
            return;
        }

        long activePlayers = countActiveSeasonPlayers(teamId, season.getId());
        if (ignoredPlayerId != null) {
            SeasonPlayer existing = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(season.getId(), teamId, ignoredPlayerId).orElse(null);
            if (existing != null) {
                return;
            }
        }
        if (activePlayers + requestedSize > season.getMaxRosterSize()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Нельзя превысить максимальный размер заявки сезона: " + season.getMaxRosterSize() + "."
            );
        }
    }

    private void activateSeasonPlayer(Long teamId, Long seasonId, Long playerId, Long actorUserId, OffsetDateTime now) {
        validateSeasonUniqueness(teamId, seasonId, Set.of(playerId));

        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));

        SeasonPlayer existing = seasonPlayerRepository.findBySeason_IdAndTeam_IdAndPlayer_Id(seasonId, teamId, playerId).orElse(null);
        if (existing != null) {
            if (!existing.isActive()) {
                validateSeasonRosterCapacity(season, teamId, 1L, playerId);
            }
            existing.setActive(true);
            existing.setUpdatedByUserId(actorUserId);
            existing.setUpdatedAt(now);
            seasonPlayerRepository.save(existing);
            return;
        }

        validateSeasonRosterCapacity(season, teamId, 1L, playerId);
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

    private void reassignPlayerToTeam(Player player, Team team, Long actorUserId, boolean deactivateSeasonAssignments) {
        LocalDate today = LocalDate.now();
        boolean alreadyInTargetTeam = false;

        playerTeamRepository.findByPlayer_IdAndActiveTrue(player.getId()).forEach(membership -> {
            if (membership.getTeam().getId().equals(team.getId())) {
                return;
            }
            if (deactivateSeasonAssignments) {
                deactivateActiveAssignmentsForPlayerInTeam(membership.getTeam().getId(), player.getId(), actorUserId);
            }
            membership.setActive(false);
            membership.setValidTo(today.minusDays(1));
            playerTeamRepository.save(membership);
        });

        alreadyInTargetTeam = playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(player.getId(), team.getId()).isPresent();
        if (alreadyInTargetTeam) {
            return;
        }

        PlayerTeam newMembership = new PlayerTeam();
        newMembership.setPlayer(player);
        newMembership.setTeam(team);
        newMembership.setValidFrom(today);
        newMembership.setActive(true);
        playerTeamRepository.save(newMembership);
    }

    private Season requireSeasonForMutation(Long teamId, Long seasonId) {
        validateSeasonMembership(teamId, seasonId);
        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Изменять заявку можно только в активном сезоне.");
        }
        return season;
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

    private ActiveSeasonAssignment mapActiveSeasonAssignment(SeasonPlayer assignment) {
        if (assignment == null) {
            return null;
        }
        return new ActiveSeasonAssignment(
            assignment.getSeason().getId(),
            assignment.getSeason().getName(),
            assignment.getTeam().getId(),
            assignment.getTeam().getName()
        );
    }

    public record ActiveSeasonAssignment(Long seasonId, String seasonName, Long teamId, String teamName) {}
}