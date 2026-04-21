package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.UserTeamScope;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
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
    private final SeasonPlayerService seasonPlayerService;
    private final MediaAssetService mediaAssetService;

    public TeamRepService(
        UserTeamScopeRepository userTeamScopeRepository,
        PlayerRepository playerRepository,
        PlayerTeamRepository playerTeamRepository,
        SeasonPlayerService seasonPlayerService,
        MediaAssetService mediaAssetService
    ) {
        this.userTeamScopeRepository = userTeamScopeRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.seasonPlayerService = seasonPlayerService;
        this.mediaAssetService = mediaAssetService;
    }

    @Transactional(readOnly = true)
    public List<TeamRepSeasonData> listAvailableSeasons(Long userId) {
        TeamScopeContext context = requireScope(userId);
        long rosterCount = playerTeamRepository.findCurrentRosterByTeamId(context.teamId()).size();
        return seasonPlayerService.listAvailableSeasonsForTeam(context.teamId()).stream()
            .map(season -> new TeamRepSeasonData(
                season.getId(),
                season.getName(),
                season.getApplicationDeadline(),
                season.getStatus(),
                season.getMaxRosterSize(),
                season.getTransferWindowStartDate(),
                season.getTransferWindowEndDate(),
                isApplicationOpen(season),
                rosterCount,
                seasonPlayerService.countActiveSeasonPlayers(context.teamId(), season.getId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamRepPlayerData> listTeamPlayers(Long userId) {
        TeamScopeContext context = requireScope(userId);
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
        TeamScopeContext context = requireApplicationScope(userId);
        Set<Long> selectedPlayerIds = seasonPlayerService.getActivePlayerIds(context.teamId(), seasonId);
        Season season = seasonPlayerService.listAvailableSeasonsForTeam(context.teamId()).stream()
            .filter(item -> item.getId().equals(seasonId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон команды не найден."));

        Map<Long, TeamRepSeasonPlayerData> playersById = new LinkedHashMap<>();

        for (Player player : playerTeamRepository.findCurrentRosterByTeamId(context.teamId()).stream().map(PlayerTeam::getPlayer).toList()) {
            playersById.put(player.getId(), new TeamRepSeasonPlayerData(
                player.getId(),
                player.getFullName(),
                player.getBirthDate(),
                player.getResidence(),
                player.isGoalkeeper(),
                mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, player.getId(), MediaAssetService.KIND_PLAYER_PHOTO),
                selectedPlayerIds.contains(player.getId()),
                true
            ));
        }

        for (SeasonPlayer seasonPlayer : seasonPlayerService.listActiveSeasonPlayers(context.teamId(), seasonId)) {
            Player player = seasonPlayer.getPlayer();
            playersById.put(player.getId(), new TeamRepSeasonPlayerData(
                player.getId(),
                player.getFullName(),
                player.getBirthDate(),
                player.getResidence(),
                player.isGoalkeeper(),
                mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, player.getId(), MediaAssetService.KIND_PLAYER_PHOTO),
                true,
                playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(player.getId(), context.teamId()).isPresent()
            ));
        }

        List<TeamRepSeasonPlayerData> players = playersById.values().stream()
            .sorted(Comparator.comparing(TeamRepSeasonPlayerData::fullName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        List<TeamRepAvailablePlayerData> availablePlayers = seasonPlayerService.listAvailablePlayersForSeason(context.teamId(), seasonId).stream()
            .map(player -> new TeamRepAvailablePlayerData(
                player.getId(),
                player.getFullName(),
                player.getBirthDate(),
                player.getResidence(),
                player.isGoalkeeper(),
                mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, player.getId(), MediaAssetService.KIND_PLAYER_PHOTO)
            ))
            .toList();

        return new TeamRepSeasonPlayersData(
            season.getId(),
            season.getName(),
            season.getApplicationDeadline(),
            season.getStatus(),
            season.getMaxRosterSize(),
            season.getTransferWindowStartDate(),
            season.getTransferWindowEndDate(),
            isApplicationOpen(season),
            context.teamId(),
            context.teamName(),
            players,
            availablePlayers
        );
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
        TeamScopeContext context = requireApplicationScope(userId);
        ensureSeasonApplicationOpenForAdditions(context.teamId(), seasonId, playerIds);
        seasonPlayerService.replaceSeasonPlayers(context.teamId(), seasonId, playerIds, userId);
        return getSeasonPlayers(userId, seasonId);
    }

    @Transactional
    public TeamRepSeasonPlayersData addSeasonPlayers(Long userId, Long seasonId, List<Long> playerIds) {
        TeamScopeContext context = requireApplicationScope(userId);
        ensureSeasonApplicationOpenForAdditions(context.teamId(), seasonId, playerIds);
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>(playerIds == null ? List.of() : playerIds.stream().filter(java.util.Objects::nonNull).toList());
        for (Long playerId : ids) {
            boolean alreadyInRoster = playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, context.teamId()).isPresent();
            if (alreadyInRoster) {
                seasonPlayerService.addSeasonPlayer(context.teamId(), seasonId, playerId, userId);
            } else {
                seasonPlayerService.attachAvailablePlayerToTeamAndSeason(context.teamId(), seasonId, playerId, userId);
            }
        }
        return getSeasonPlayers(userId, seasonId);
    }

    @Transactional
    public TeamRepSeasonPlayersData addSeasonPlayer(Long userId, Long seasonId, Long playerId) {
        TeamScopeContext context = requireApplicationScope(userId);
        ensureSeasonApplicationOpenForAdditions(context.teamId(), seasonId, List.of(playerId));
        boolean alreadyInRoster = playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, context.teamId()).isPresent();
        if (alreadyInRoster) {
            seasonPlayerService.addSeasonPlayer(context.teamId(), seasonId, playerId, userId);
        } else {
            seasonPlayerService.attachAvailablePlayerToTeamAndSeason(context.teamId(), seasonId, playerId, userId);
        }
        return getSeasonPlayers(userId, seasonId);
    }

    @Transactional
    public TeamRepSeasonPlayersData removeSeasonPlayer(Long userId, Long seasonId, Long playerId) {
        TeamScopeContext context = requireApplicationScope(userId);
        seasonPlayerService.removeSeasonPlayer(context.teamId(), seasonId, playerId, userId);
        return getSeasonPlayers(userId, seasonId);
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
        return new TeamScopeContext(scope, scope.getTeam().getId(), scope.getTeam().getName());
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

    private record TeamScopeContext(UserTeamScope scope, Long teamId, String teamName) {}

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
        long selectedPlayersCount
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
        List<TeamRepAvailablePlayerData> availablePlayers
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