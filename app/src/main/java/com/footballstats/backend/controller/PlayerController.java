package com.footballstats.backend.controller;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.AccessControlService;
import com.footballstats.backend.service.MediaAssetService;
import com.footballstats.backend.service.SeasonPlayerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamRepository teamRepository;
    private final AccessControlService accessControlService;
    private final MediaAssetService mediaAssetService;
    private final SeasonPlayerService seasonPlayerService;

    public PlayerController(
        PlayerRepository playerRepository,
        PlayerTeamRepository playerTeamRepository,
        TeamRepository teamRepository,
        AccessControlService accessControlService,
        MediaAssetService mediaAssetService,
        SeasonPlayerService seasonPlayerService
    ) {
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.teamRepository = teamRepository;
        this.accessControlService = accessControlService;
        this.mediaAssetService = mediaAssetService;
        this.seasonPlayerService = seasonPlayerService;
    }

    // ----------------------------------------------------------------
    // GET /api/players — все активные игроки
    // ----------------------------------------------------------------
    @GetMapping("/players")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<PlayerResponse>> listPlayers(
        @RequestParam(name = "active_flag", defaultValue = "1") int activeFlag,
        @RequestParam(required = false) String name,
        @RequestParam(required = false, name = "team_id") Long teamId,
        @RequestParam(required = false, name = "season_id") Long seasonId,
        @RequestParam(required = false) Integer goals,
        @RequestParam(required = false, name = "yellow_cards") Integer yellowCards,
        @RequestParam(required = false, name = "red_cards") Integer redCards,
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize
    ) {
        Pageable pageable = buildPageable(pagenum, pagesize);

        String normalizedName = normalizeOptional(name);
        String namePattern = normalizedName == null ? null : "%" + normalizedName.toLowerCase(Locale.ROOT) + "%";
        Page<Player> players = playerRepository.searchPlayers(
            activeFlag,
            namePattern,
            teamId,
            seasonId,
            goals,
            yellowCards,
            redCards,
            pageable
        );

        Page<PlayerResponse> result = players.map(p -> {
            List<PlayerTeam> active = playerTeamRepository.findByPlayer_IdAndActiveTrue(p.getId());
            String teamName = active.isEmpty() ? null : active.get(0).getTeam().getName();
            Long currentTeamId = active.isEmpty() ? null : active.get(0).getTeam().getId();
            String photo = mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, p.getId(), MediaAssetService.KIND_PLAYER_PHOTO);
            return toResponse(p, currentTeamId, teamName, photo);
        });
        return ResponseEntity.ok(result);
    }

    // ----------------------------------------------------------------
    // GET /api/players/{playerId} — карточка игрока
    // ----------------------------------------------------------------
    @GetMapping("/players/{playerId}")
    @Transactional(readOnly = true)
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));

        List<PlayerTeam> active = playerTeamRepository.findByPlayer_IdAndActiveTrue(player.getId());
        String teamName = active.isEmpty() ? null : active.get(0).getTeam().getName();
        Long teamId = active.isEmpty() ? null : active.get(0).getTeam().getId();
        String photo = mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, player.getId(), MediaAssetService.KIND_PLAYER_PHOTO);

        return ResponseEntity.ok(toResponse(player, teamId, teamName, photo));
    }

    // ----------------------------------------------------------------
    // GET /api/players/{id}/history — история переходов игрока
    // ----------------------------------------------------------------
    @GetMapping("/players/{id}/history")
    @Transactional(readOnly = true)
    public ResponseEntity<PlayerHistoryResponse> getPlayerHistory(@PathVariable Long id) {
        Player player = playerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));

        List<MembershipRecord> history = playerTeamRepository.findHistoryByPlayerId(id).stream()
            .map(pt -> new MembershipRecord(
                pt.getTeam().getId(),
                pt.getTeam().getName(),
                pt.getValidFrom(),
                pt.getValidTo(),
                pt.isActive()
            ))
            .toList();

        return ResponseEntity.ok(new PlayerHistoryResponse(player.getId(), player.getFullName(), history));
    }

    // ----------------------------------------------------------------
    // POST /api/players — создать игрока (только SUPER_ADMIN)
    // ----------------------------------------------------------------
    @PostMapping("/players")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PlayerResponse> createPlayer(@Valid @RequestBody PlayerUpsertRequest request, Authentication authentication) {
        if (playerRepository.existsByFullNameIgnoreCase(request.fullName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок с таким именем уже существует.");
        }
        Player player = new Player();
        player.setFullName(request.fullName().strip());
        player.setBirthDate(request.birthDate());
        player.setResidence(normalizeOptional(request.residence()));
        Long actorUserId = currentUserId(authentication);
        player.setCreatedByUserId(actorUserId);
        player.setUpdatedByUserId(actorUserId);
        player.setUpdatedAt(OffsetDateTime.now());
        Player saved = playerRepository.save(player);
        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_PLAYER,
            saved.getId(),
            MediaAssetService.KIND_PLAYER_PHOTO,
            request.photoDataUrl(),
            actorUserId
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = playerRepository.save(saved);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toResponse(saved, null, null, photo == null ? null : photo.getDataUrl()));
    }

    @PutMapping("/players/{playerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<PlayerResponse> updatePlayer(
        @PathVariable Long playerId,
        @Valid @RequestBody PlayerUpsertRequest request,
        Authentication authentication
    ) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));

        String normalizedName = request.fullName().strip();
        if (!player.getFullName().equalsIgnoreCase(normalizedName) && playerRepository.existsByFullNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок с таким именем уже существует.");
        }

        player.setFullName(normalizedName);
        player.setBirthDate(request.birthDate());
        player.setResidence(normalizeOptional(request.residence()));
        player.setUpdatedByUserId(currentUserId(authentication));
        player.setUpdatedAt(OffsetDateTime.now());

        Player saved = playerRepository.save(player);

        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_PLAYER,
            saved.getId(),
            MediaAssetService.KIND_PLAYER_PHOTO,
            request.photoDataUrl(),
            currentUserId(authentication)
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = playerRepository.save(saved);
        }

        String photoDataUrl = photo == null
            ? mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, saved.getId(), MediaAssetService.KIND_PLAYER_PHOTO)
            : photo.getDataUrl();

        List<PlayerTeam> active = playerTeamRepository.findByPlayer_IdAndActiveTrue(saved.getId());
        String teamName = active.isEmpty() ? null : active.get(0).getTeam().getName();
        Long teamId = active.isEmpty() ? null : active.get(0).getTeam().getId();

        return ResponseEntity.ok(toResponse(saved, teamId, teamName, photoDataUrl));
    }

    @DeleteMapping("/players/{playerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivatePlayer(@PathVariable Long playerId, Authentication authentication) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));

        player.setActive(false);
        player.setUpdatedByUserId(currentUserId(authentication));
        player.setUpdatedAt(OffsetDateTime.now());
        playerRepository.save(player);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // GET /api/teams/{teamId}/players — текущий состав команды
    // ----------------------------------------------------------------
    @GetMapping("/teams/{teamId}/players")
    public ResponseEntity<List<RosterPlayerResponse>> getRoster(@PathVariable Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена.");
        }
        List<RosterPlayerResponse> roster = playerTeamRepository.findCurrentRosterByTeamId(teamId).stream()
            .map(pt -> new RosterPlayerResponse(
                pt.getPlayer().getId(),
                pt.getPlayer().getFullName(),
                pt.getValidFrom()
            ))
            .toList();
        return ResponseEntity.ok(roster);
    }

    // ----------------------------------------------------------------
    // POST /api/teams/{teamId}/players/{playerId} — добавить игрока в состав
    // SUPER_ADMIN или TEAM_REP с canEditRoster для этой команды
    // ----------------------------------------------------------------
    @PostMapping("/teams/{teamId}/players/{playerId}")
    public ResponseEntity<Void> addPlayerToTeam(
        @PathVariable Long teamId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        checkRosterEditAccess(authentication, teamId);
        Long actorUserId = currentUserId(authentication);

        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));

        if (playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, teamId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок уже в составе этой команды.");
        }

        // Завершаем активную привязку к другой команде, если есть
        playerTeamRepository.findByPlayer_IdAndActiveTrue(playerId).forEach(pt -> {
            seasonPlayerService.deactivateActiveAssignmentsForPlayerInTeam(pt.getTeam().getId(), playerId, actorUserId);
            pt.setActive(false);
            pt.setValidTo(LocalDate.now().minusDays(1));
            playerTeamRepository.save(pt);
        });

        PlayerTeam pt = new PlayerTeam();
        pt.setPlayer(player);
        pt.setTeam(team);
        pt.setValidFrom(LocalDate.now());
        playerTeamRepository.save(pt);

        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/teams/{teamId}/players/{playerId} — убрать игрока из состава
    // ----------------------------------------------------------------
    @DeleteMapping("/teams/{teamId}/players/{playerId}")
    public ResponseEntity<Void> removePlayerFromTeam(
        @PathVariable Long teamId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        checkRosterEditAccess(authentication, teamId);

        PlayerTeam pt = playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден в составе этой команды."));

        pt.setActive(false);
        pt.setValidTo(LocalDate.now());
        playerTeamRepository.save(pt);

        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------
    private void checkRosterEditAccess(Authentication authentication, Long teamId) {
        Long userId = ((AppUserPrincipal) authentication.getPrincipal()).getUserId();
        boolean isSuperAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) return;

        if (!accessControlService.hasTeamPermission(userId, teamId, "ROSTER_EDIT")) {
            throw new AccessDeniedException("Нет прав редактировать состав этой команды.");
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    private String normalizeOptional(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private PlayerResponse toResponse(Player player, Long currentTeamId, String currentTeamName, String photoDataUrl) {
        return new PlayerResponse(
            player.getId(),
            player.getFullName(),
            currentTeamId,
            currentTeamName,
            photoDataUrl,
            player.getBirthDate(),
            player.getResidence(),
            player.getSeasonId(),
            player.getGoals(),
            player.getYellowCards(),
            player.getRedCards(),
            player.isActive(),
            player.getCreatedByUserId(),
            player.getUpdatedByUserId(),
            player.getCreatedAt(),
            player.getUpdatedAt()
        );
    }

    private Pageable buildPageable(int pageNum, int pageSize) {
        int normalizedPage = Math.max(pageNum, 0);
        int normalizedSize = Math.min(Math.max(pageSize, 1), 100);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.unsorted());
    }

    // ----------------------------------------------------------------
    // Records (inline DTOs)
    // ----------------------------------------------------------------
    public record PlayerResponse(
        Long id,
        String fullName,
        Long currentTeamId,
        String currentTeamName,
        String photoDataUrl,
        LocalDate birthDate,
        String residence,
        Long seasonId,
        int goals,
        int yellowCards,
        int redCards,
        boolean active,
        Long createdByUserId,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}
    public record RosterPlayerResponse(Long id, String fullName, LocalDate inTeamSince) {}
    public record MembershipRecord(Long teamId, String teamName, LocalDate validFrom, LocalDate validTo, boolean active) {}
    public record PlayerHistoryResponse(Long id, String fullName, List<MembershipRecord> history) {}
    public record PlayerUpsertRequest(
        @NotBlank(message = "ФИО игрока обязательно.") String fullName,
        LocalDate birthDate,
        String residence,
        String photoDataUrl
    ) {}
}
