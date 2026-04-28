package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonApplicationStatus;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.UserTeamScope;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TeamRepService {

    private final UserTeamScopeRepository userTeamScopeRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamRepository teamRepository;
    private final SeasonPlayerService seasonPlayerService;
    private final MediaAssetService mediaAssetService;
    private final SeasonApplicationService seasonApplicationService;

    public TeamRepService(
        UserTeamScopeRepository userTeamScopeRepository,
        PlayerRepository playerRepository,
        PlayerTeamRepository playerTeamRepository,
        TeamRepository teamRepository,
        SeasonPlayerService seasonPlayerService,
        MediaAssetService mediaAssetService,
        SeasonApplicationService seasonApplicationService
    ) {
        this.userTeamScopeRepository = userTeamScopeRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.teamRepository = teamRepository;
        this.seasonPlayerService = seasonPlayerService;
        this.mediaAssetService = mediaAssetService;
        this.seasonApplicationService = seasonApplicationService;
    }

    @Transactional(readOnly = true)
    public List<TeamRepSeasonData> listAvailableSeasons(Long userId) {
        return listAvailableSeasons(new TeamRepActor(userId, false), null);
    }

    @Transactional(readOnly = true)
    public List<TeamRepSeasonData> listAvailableSeasons(TeamRepActor actor, Long requestedTeamId) {
        TeamScopeContext context = requireTeamAccess(actor, requestedTeamId);
        return seasonPlayerService.listAvailableSeasonsForTeam(context.teamId()).stream()
            .map(season -> seasonApplicationService.toSeasonSummary(actor.userId(), context.teamId(), season, context.privilegedAccess()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamRepPlayerData> listTeamPlayers(Long userId) {
        return listTeamPlayers(new TeamRepActor(userId, false), null);
    }

    @Transactional(readOnly = true)
    public List<TeamRepPlayerData> listTeamPlayers(TeamRepActor actor, Long requestedTeamId) {
        TeamScopeContext context = requireTeamAccess(actor, requestedTeamId);
        List<PlayerTeam> rosterMemberships = playerTeamRepository.findCurrentRosterByTeamId(context.teamId());
        Set<Long> rosterPlayerIds = rosterMemberships.stream()
            .map(item -> item.getPlayer().getId())
            .collect(java.util.stream.Collectors.toSet());
        Map<Long, List<TeamRepPlayerSeasonData>> seasonsByPlayerId = new LinkedHashMap<>();

        for (SeasonPlayer seasonPlayer : seasonPlayerService.listActiveSeasonAssignmentsForTeam(context.teamId())) {
            Long playerId = seasonPlayer.getPlayer().getId();
            if (!rosterPlayerIds.contains(playerId)) {
                continue;
            }
            seasonsByPlayerId.computeIfAbsent(playerId, ignored -> new java.util.ArrayList<>())
                .add(new TeamRepPlayerSeasonData(seasonPlayer.getSeason().getId(), seasonPlayer.getSeason().getName()));
        }

        return rosterMemberships.stream()
            .map(PlayerTeam::getPlayer)
            .map(player -> toPlayerData(player, seasonsByPlayerId.getOrDefault(player.getId(), List.of())))
            .toList();
    }

    @Transactional(readOnly = true)
    public TeamRepSeasonPlayersData getSeasonPlayers(Long userId, Long seasonId) {
        return getSeasonPlayers(new TeamRepActor(userId, false), seasonId, null);
    }

    @Transactional(readOnly = true)
    public TeamRepSeasonPlayersData getSeasonPlayers(TeamRepActor actor, Long seasonId, Long requestedTeamId) {
        TeamScopeContext context = requireApplicationAccess(actor, requestedTeamId);
        return seasonApplicationService.getSeasonApplicationView(actor.userId(), seasonId, context.teamId(), context.privilegedAccess());
    }

    @Transactional
    public TeamRepPlayerData createPlayer(Long userId, TeamRepPlayerDraft request) {
        TeamScopeContext context = requireRosterScope(userId);

        String normalizedName = normalizeRequired(request.fullName(), "ФИО игрока обязательно.");
        if (playerRepository.existsByFullNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок с таким именем уже существует.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Player player = new Player();
        player.setFullName(normalizedName);
        player.setBirthDate(request.birthDate());
        player.setResidence(normalizeOptional(request.residence()));
        player.setGoalkeeper(request.isGoalkeeper());
        player.setCreatedByUserId(userId);
        player.setUpdatedByUserId(userId);
        player.setUpdatedAt(now);
        Player saved = playerRepository.save(player);

        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_PLAYER,
            saved.getId(),
            MediaAssetService.KIND_PLAYER_PHOTO,
            request.photoDataUrl(),
            userId
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = playerRepository.save(saved);
        }

        PlayerTeam membership = new PlayerTeam();
        membership.setPlayer(saved);
        membership.setTeam(context.scope().getTeam());
        membership.setValidFrom(LocalDate.now());
        membership.setActive(true);
        playerTeamRepository.save(membership);

        return toPlayerData(saved, List.of());
    }

    @Transactional
    public TeamRepPlayerData updatePlayer(Long userId, Long playerId, TeamRepPlayerDraft request) {
        TeamScopeContext context = requireRosterScope(userId);
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));

        boolean inRoster = playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, context.teamId()).isPresent();
        if (!inRoster) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно редактировать только игроков своей команды.");
        }

        String normalizedName = normalizeRequired(request.fullName(), "ФИО игрока обязательно.");
        if (!player.getFullName().equalsIgnoreCase(normalizedName) && playerRepository.existsByFullNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок с таким именем уже существует.");
        }

        player.setFullName(normalizedName);
        player.setBirthDate(request.birthDate());
        player.setResidence(normalizeOptional(request.residence()));
        player.setGoalkeeper(request.isGoalkeeper());
        player.setUpdatedByUserId(userId);
        player.setUpdatedAt(OffsetDateTime.now());
        Player saved = playerRepository.save(player);

        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_PLAYER,
            saved.getId(),
            MediaAssetService.KIND_PLAYER_PHOTO,
            request.photoDataUrl(),
            userId
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = playerRepository.save(saved);
        }

        List<TeamRepPlayerSeasonData> seasons = seasonPlayerService.listActiveSeasonAssignmentsForPlayer(context.teamId(), saved.getId()).stream()
            .map(item -> new TeamRepPlayerSeasonData(item.getSeason().getId(), item.getSeason().getName()))
            .toList();
        return toPlayerData(saved, seasons);
    }

    @Transactional
    public TeamRepSeasonPlayersData replaceSeasonPlayers(Long userId, Long seasonId, List<Long> playerIds) {
        return replaceSeasonPlayers(new TeamRepActor(userId, false), seasonId, playerIds, null);
    }

    @Transactional
    public TeamRepSeasonPlayersData replaceSeasonPlayers(TeamRepActor actor, Long seasonId, List<Long> playerIds, Long requestedTeamId) {
        TeamRepSeasonPlayersData current = getSeasonPlayers(actor, seasonId, requestedTeamId);
        Set<Long> targetIds = new LinkedHashSet<>(playerIds == null ? List.of() : playerIds);
        Set<Long> currentIds = current.players().stream()
            .filter(TeamRepSeasonPlayerData::selectedForSeason)
            .map(TeamRepSeasonPlayerData::id)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (Long currentId : currentIds) {
            if (!targetIds.contains(currentId)) {
                seasonApplicationService.removePlayer(actor.userId(), seasonId, currentId, current.teamId(), current.teamId() != null && actor.superAdmin());
            }
        }
        Set<Long> toAdd = new LinkedHashSet<>(targetIds);
        toAdd.removeAll(currentIds);
        if (!toAdd.isEmpty()) {
            seasonApplicationService.addPlayers(actor.userId(), seasonId, List.copyOf(toAdd), current.teamId(), current.teamId() != null && actor.superAdmin());
        }
        return getSeasonPlayers(actor, seasonId, requestedTeamId);
    }

    @Transactional
    public TeamRepSeasonPlayersData addSeasonPlayers(Long userId, Long seasonId, List<Long> playerIds) {
        return addSeasonPlayers(new TeamRepActor(userId, false), seasonId, playerIds, null);
    }

    @Transactional
    public TeamRepSeasonPlayersData addSeasonPlayers(TeamRepActor actor, Long seasonId, List<Long> playerIds, Long requestedTeamId) {
        TeamScopeContext context = requireApplicationAccess(actor, requestedTeamId);
        return seasonApplicationService.addPlayers(actor.userId(), seasonId, playerIds, context.teamId(), context.privilegedAccess());
    }

    @Transactional
    public TeamRepSeasonPlayersData addSeasonPlayer(Long userId, Long seasonId, Long playerId) {
        return addSeasonPlayer(new TeamRepActor(userId, false), seasonId, playerId, null);
    }

    @Transactional
    public TeamRepSeasonPlayersData addSeasonPlayer(TeamRepActor actor, Long seasonId, Long playerId, Long requestedTeamId) {
        return addSeasonPlayers(actor, seasonId, List.of(playerId), requestedTeamId);
    }

    @Transactional
    public TeamRepSeasonPlayersData removeSeasonPlayer(Long userId, Long seasonId, Long playerId) {
        return removeSeasonPlayer(new TeamRepActor(userId, false), seasonId, playerId, null);
    }

    @Transactional
    public TeamRepSeasonPlayersData removeSeasonPlayer(TeamRepActor actor, Long seasonId, Long playerId, Long requestedTeamId) {
        TeamScopeContext context = requireApplicationAccess(actor, requestedTeamId);
        return seasonApplicationService.removePlayer(actor.userId(), seasonId, playerId, context.teamId(), context.privilegedAccess());
    }

    @Transactional
    public TeamRepSeasonPlayersData submitSeasonApplication(Long userId, Long seasonId) {
        return submitSeasonApplication(new TeamRepActor(userId, false), seasonId, null);
    }

    @Transactional
    public TeamRepSeasonPlayersData submitSeasonApplication(TeamRepActor actor, Long seasonId, Long requestedTeamId) {
        TeamScopeContext context = requireApplicationAccess(actor, requestedTeamId);
        return seasonApplicationService.submit(actor.userId(), seasonId, context.teamId(), context.privilegedAccess());
    }

    private TeamRepPlayerData toPlayerData(Player player, List<TeamRepPlayerSeasonData> seasons) {
        return new TeamRepPlayerData(
            player.getId(),
            player.getFullName(),
            player.getBirthDate(),
            player.getResidence(),
            player.isGoalkeeper(),
            mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, player.getId(), MediaAssetService.KIND_PLAYER_PHOTO),
            seasons.stream().map(TeamRepPlayerSeasonData::id).toList(),
            seasons,
            player.isActive()
        );
    }

    private TeamScopeContext requireScope(Long userId) {
        UserTeamScope scope = userTeamScopeRepository.findByUser_IdAndActiveTrue(userId).stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Для пользователя не назначена команда."));
        return new TeamScopeContext(scope, scope.getTeam().getId(), scope.getTeam().getName(), scope.getTeam(), false);
    }

    private TeamScopeContext requirePrivilegedScope(Long requestedTeamId) {
        if (requestedTeamId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для режима SUPER_ADMIN нужно указать teamId.");
        }
        Team team = teamRepository.findById(requestedTeamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
        return new TeamScopeContext(null, team.getId(), team.getName(), team, true);
    }

    private TeamScopeContext requireTeamAccess(TeamRepActor actor, Long requestedTeamId) {
        if (actor.superAdmin()) {
            return requirePrivilegedScope(requestedTeamId);
        }
        return requireScope(actor.userId());
    }

    private TeamScopeContext requireRosterScope(Long userId) {
        TeamScopeContext context = requireScope(userId);
        if (!context.scope().isCanEditRoster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав на редактирование состава команды.");
        }
        return context;
    }

    private TeamScopeContext requireApplicationScope(Long userId) {
        TeamScopeContext context = requireScope(userId);
        if (!context.scope().isCanEditApplication()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав на редактирование заявки сезона.");
        }
        return context;
    }

    private TeamScopeContext requireApplicationAccess(TeamRepActor actor, Long requestedTeamId) {
        if (actor.superAdmin()) {
            return requirePrivilegedScope(requestedTeamId);
        }
        return requireApplicationScope(actor.userId());
    }

    private void ensureSeasonApplicationOpenForAdditions(Long teamId, Long seasonId, List<Long> playerIds) {
        Season season = seasonPlayerService.getSeasonForTeam(teamId, seasonId);
        if (isApplicationOpen(season)) {
            return;
        }

        Set<Long> requestedIds = new LinkedHashSet<>(playerIds == null ? List.of() : playerIds.stream().filter(java.util.Objects::nonNull).toList());
        if (requestedIds.isEmpty()) {
            return;
        }

        Set<Long> activeIds = seasonPlayerService.getActivePlayerIds(teamId, seasonId);
        boolean hasNewPlayers = requestedIds.stream().anyMatch(playerId -> !activeIds.contains(playerId));
        if (!hasNewPlayers) {
            return;
        }

        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Срок добавления игроков в заявку сезона истек " + season.getApplicationDeadline() + "."
        );
    }

    private boolean isApplicationOpen(Season season) {
        return season.getStatus() == SeasonStatus.ACTIVE
            && (season.getApplicationDeadline() == null || !LocalDate.now().isAfter(season.getApplicationDeadline()));
    }

    private String normalizeOptional(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private record TeamScopeContext(UserTeamScope scope, Long teamId, String teamName, Team team, boolean privilegedAccess) {}

    public record TeamRepActor(Long userId, boolean superAdmin) {}

    public record TeamRepSeasonData(
        Long id,
        String name,
        LocalDate applicationDeadline,
        SeasonStatus status,
        Integer maxRosterSize,
        LocalDate transferWindowStartDate,
        LocalDate transferWindowEndDate,
        boolean applicationOpen,
        long rosterPlayersCount,
        long selectedPlayersCount,
        SeasonApplicationStatus applicationStatus,
        OffsetDateTime applicationSubmittedAt,
        OffsetDateTime applicationDecisionAt,
        String applicationDecisionComment,
        boolean applicationSubmittable
    ) {}

    public record TeamRepPlayerData(
        Long id,
        String fullName,
        LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl,
        List<Long> seasonIds,
        List<TeamRepPlayerSeasonData> seasons,
        boolean active
    ) {}

    public record TeamRepPlayerSeasonData(Long id, String name) {}

    public record TeamRepSeasonPlayersData(
        Long seasonId,
        String seasonName,
        LocalDate applicationDeadline,
        SeasonStatus status,
        Integer maxRosterSize,
        LocalDate transferWindowStartDate,
        LocalDate transferWindowEndDate,
        boolean applicationOpen,
        Long teamId,
        String teamName,
        List<TeamRepSeasonPlayerData> players,
        List<TeamRepAvailablePlayerData> availablePlayers,
        Long applicationId,
        SeasonApplicationStatus applicationStatus,
        OffsetDateTime applicationSubmittedAt,
        OffsetDateTime applicationDecisionAt,
        String applicationDecisionComment,
        boolean applicationSubmittable
    ) {}

    public record TeamRepSeasonPlayerData(
        Long id,
        String fullName,
        LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl,
        boolean selectedForSeason,
        boolean inCurrentRoster
    ) {}

    public record TeamRepAvailablePlayerData(
        Long id,
        String fullName,
        LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl
    ) {}

    public record TeamRepPlayerDraft(
        String fullName,
        LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl
    ) {}
}