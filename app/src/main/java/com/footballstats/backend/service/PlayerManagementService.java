package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PlayerManagementService {

    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamRepository teamRepository;
    private final MediaAssetService mediaAssetService;
    private final SeasonPlayerService seasonPlayerService;

    public PlayerManagementService(
        PlayerRepository playerRepository,
        PlayerTeamRepository playerTeamRepository,
        TeamRepository teamRepository,
        MediaAssetService mediaAssetService,
        SeasonPlayerService seasonPlayerService
    ) {
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.teamRepository = teamRepository;
        this.mediaAssetService = mediaAssetService;
        this.seasonPlayerService = seasonPlayerService;
    }

    @Transactional(readOnly = true)
    public Page<PlayerData> listPlayers(PlayerSearch search) {
        String normalizedName = normalizeOptional(search.name());
        String namePattern = normalizedName == null ? null : "%" + normalizedName.toLowerCase(Locale.ROOT) + "%";
        Page<Player> players = playerRepository.searchPlayers(
            search.activeFlag(), namePattern, search.teamId(), search.seasonId(), search.goals(),
            search.yellowCards(), search.redCards(),
            PageRequest.of(Math.max(search.pageNum(), 0), Math.min(Math.max(search.pageSize(), 1), 100), Sort.unsorted())
        );
        List<Long> playerIds = players.getContent().stream().map(Player::getId).toList();
        Map<Long, PlayerTeam> activeTeams = new LinkedHashMap<>();
        if (!playerIds.isEmpty()) {
            playerTeamRepository.findActiveByPlayerIds(playerIds)
                .forEach(link -> activeTeams.putIfAbsent(link.getPlayer().getId(), link));
        }
        Map<Long, SeasonPlayerService.ActiveSeasonAssignment> seasonAssignments =
            seasonPlayerService.getLatestActiveSeasonAssignments(playerIds);
        Map<Long, String> photos = mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER, playerIds, MediaAssetService.KIND_PLAYER_PHOTO
        );
        return players.map(player -> toData(player, activeTeams.get(player.getId()), seasonAssignments.get(player.getId()), photos.get(player.getId())));
    }

    @Transactional(readOnly = true)
    public PlayerData getPlayer(Long playerId) {
        Player player = requirePlayer(playerId);
        PlayerTeam activeTeam = playerTeamRepository.findByPlayer_IdAndActiveTrue(playerId).stream().findFirst().orElse(null);
        return toData(
            player,
            activeTeam,
            seasonPlayerService.getLatestActiveSeasonAssignment(playerId),
            mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, playerId, MediaAssetService.KIND_PLAYER_PHOTO)
        );
    }

    @Transactional(readOnly = true)
    public PlayerHistoryData getHistory(Long playerId) {
        Player player = requirePlayer(playerId);
        List<MembershipData> history = playerTeamRepository.findHistoryByPlayerId(playerId).stream()
            .map(link -> new MembershipData(
                link.getTeam().getId(), link.getTeam().getName(), link.getValidFrom(), link.getValidTo(), link.isActive()
            ))
            .toList();
        return new PlayerHistoryData(player.getId(), player.getFullName(), history);
    }

    @Transactional
    public PlayerData create(PlayerUpsert command, Long actorUserId) {
        String fullName = command.fullName().strip();
        if (playerRepository.existsByFullNameIgnoreCase(fullName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок с таким именем уже существует.");
        }
        Player player = new Player();
        apply(player, command, actorUserId);
        player.setCreatedByUserId(actorUserId);
        return saveWithPhoto(player, command.photoDataUrl(), actorUserId);
    }

    @Transactional
    public PlayerData update(Long playerId, PlayerUpsert command, Long actorUserId) {
        Player player = requirePlayer(playerId);
        String fullName = command.fullName().strip();
        if (!player.getFullName().equalsIgnoreCase(fullName) && playerRepository.existsByFullNameIgnoreCase(fullName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок с таким именем уже существует.");
        }
        apply(player, command, actorUserId);
        return saveWithPhoto(player, command.photoDataUrl(), actorUserId);
    }

    @Transactional
    public void deactivate(Long playerId, Long actorUserId) {
        Player player = requirePlayer(playerId);
        player.setActive(false);
        player.setUpdatedByUserId(actorUserId);
        player.setUpdatedAt(OffsetDateTime.now());
        playerRepository.save(player);
    }

    @Transactional(readOnly = true)
    public List<RosterPlayerData> getRoster(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена.");
        }
        return playerTeamRepository.findCurrentRosterByTeamId(teamId).stream()
            .map(link -> new RosterPlayerData(
                link.getPlayer().getId(), link.getPlayer().getFullName(), link.getPlayer().isGoalkeeper(), link.getValidFrom()
            ))
            .toList();
    }

    @Transactional
    public void addToTeam(Long teamId, Long playerId, Long actorUserId) {
        Player player = requirePlayer(playerId);
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена.");
        }
        if (playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, teamId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок уже в составе этой команды.");
        }
        seasonPlayerService.ensurePlayerAssignedToTeam(teamId, player.getId(), actorUserId);
    }

    @Transactional
    public void removeFromTeam(Long teamId, Long playerId) {
        PlayerTeam link = playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден в составе этой команды."));
        link.setActive(false);
        link.setValidTo(LocalDate.now());
        playerTeamRepository.save(link);
    }

    private PlayerData saveWithPhoto(Player player, String photoDataUrl, Long actorUserId) {
        Player saved = playerRepository.save(player);
        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_PLAYER, saved.getId(), MediaAssetService.KIND_PLAYER_PHOTO, photoDataUrl, actorUserId
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = playerRepository.save(saved);
        }
        PlayerTeam activeTeam = playerTeamRepository.findByPlayer_IdAndActiveTrue(saved.getId()).stream().findFirst().orElse(null);
        String resolvedPhoto = photo == null
            ? mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, saved.getId(), MediaAssetService.KIND_PLAYER_PHOTO)
            : photo.getDataUrl();
        return toData(saved, activeTeam, seasonPlayerService.getLatestActiveSeasonAssignment(saved.getId()), resolvedPhoto);
    }

    private void apply(Player player, PlayerUpsert command, Long actorUserId) {
        player.setFullName(command.fullName().strip());
        player.setBirthDate(command.birthDate());
        player.setResidence(normalizeOptional(command.residence()));
        player.setGoalkeeper(command.isGoalkeeper());
        player.setUpdatedByUserId(actorUserId);
        player.setUpdatedAt(OffsetDateTime.now());
    }

    private Player requirePlayer(Long playerId) {
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));
    }

    private PlayerData toData(
        Player player,
        PlayerTeam activeTeam,
        SeasonPlayerService.ActiveSeasonAssignment seasonAssignment,
        String photoDataUrl
    ) {
        return new PlayerData(
            player.getId(), player.getFullName(),
            activeTeam == null ? null : activeTeam.getTeam().getId(),
            activeTeam == null ? null : activeTeam.getTeam().getName(),
            seasonAssignment == null ? null : seasonAssignment.teamId(),
            seasonAssignment == null ? null : seasonAssignment.teamName(),
            seasonAssignment == null ? null : seasonAssignment.seasonId(),
            seasonAssignment == null ? null : seasonAssignment.seasonName(),
            photoDataUrl, player.getBirthDate(), player.getResidence(), player.getSeasonId(), player.isGoalkeeper(),
            player.getGoals(), player.getYellowCards(), player.getRedCards(), player.isActive(),
            player.getCreatedByUserId(), player.getUpdatedByUserId(), player.getCreatedAt(), player.getUpdatedAt()
        );
    }

    private String normalizeOptional(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record PlayerSearch(
        int activeFlag, String name, Long teamId, Long seasonId, Integer goals, Integer yellowCards,
        Integer redCards, int pageNum, int pageSize
    ) {}

    public record PlayerUpsert(String fullName, LocalDate birthDate, String residence, boolean isGoalkeeper, String photoDataUrl) {}

    public record PlayerData(
        Long id, String fullName, Long currentTeamId, String currentTeamName,
        Long activeSeasonTeamId, String activeSeasonTeamName, Long activeSeasonId, String activeSeasonName,
        String photoDataUrl, LocalDate birthDate, String residence, Long seasonId, boolean isGoalkeeper,
        int goals, int yellowCards, int redCards, boolean active, Long createdByUserId, Long updatedByUserId,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    public record RosterPlayerData(Long id, String fullName, boolean isGoalkeeper, LocalDate inTeamSince) {}
    public record MembershipData(Long teamId, String teamName, LocalDate validFrom, LocalDate validTo, boolean active) {}
    public record PlayerHistoryData(Long id, String fullName, List<MembershipData> history) {}
}
