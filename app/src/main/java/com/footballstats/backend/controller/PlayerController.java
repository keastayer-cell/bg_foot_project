package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.AccessControlService;
import com.footballstats.backend.service.PlayerManagementService;
import com.footballstats.backend.service.PlayerManagementService.PlayerData;
import com.footballstats.backend.service.PlayerManagementService.PlayerHistoryData;
import com.footballstats.backend.service.PlayerManagementService.PlayerSearch;
import com.footballstats.backend.service.PlayerManagementService.PlayerUpsert;
import com.footballstats.backend.service.PlayerManagementService.RosterPlayerData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final PlayerManagementService playerManagementService;
    private final AccessControlService accessControlService;

    public PlayerController(PlayerManagementService playerManagementService, AccessControlService accessControlService) {
        this.playerManagementService = playerManagementService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/players")
    public ResponseEntity<Page<PlayerData>> listPlayers(
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
        return ResponseEntity.ok(playerManagementService.listPlayers(
            new PlayerSearch(activeFlag, name, teamId, seasonId, goals, yellowCards, redCards, pagenum, pagesize)
        ));
    }

    @GetMapping("/players/{playerId}")
    public ResponseEntity<PlayerData> getPlayer(@PathVariable Long playerId) {
        return ResponseEntity.ok(playerManagementService.getPlayer(playerId));
    }

    @GetMapping("/players/{id}/history")
    public ResponseEntity<PlayerHistoryData> getPlayerHistory(@PathVariable Long id) {
        return ResponseEntity.ok(playerManagementService.getHistory(id));
    }

    @PostMapping("/players")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<PlayerData> createPlayer(
        @Valid @RequestBody PlayerUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(playerManagementService.create(request.toCommand(), currentUserId(authentication)));
    }

    @PutMapping("/players/{playerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<PlayerData> updatePlayer(
        @PathVariable Long playerId,
        @Valid @RequestBody PlayerUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(playerManagementService.update(playerId, request.toCommand(), currentUserId(authentication)));
    }

    @DeleteMapping("/players/{playerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<Void> deactivatePlayer(@PathVariable Long playerId, Authentication authentication) {
        playerManagementService.deactivate(playerId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teams/{teamId}/players")
    public ResponseEntity<List<RosterPlayerData>> getRoster(@PathVariable Long teamId) {
        return ResponseEntity.ok(playerManagementService.getRoster(teamId));
    }

    @PostMapping("/teams/{teamId}/players/{playerId}")
    public ResponseEntity<Void> addPlayerToTeam(
        @PathVariable Long teamId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        checkRosterEditAccess(authentication, teamId);
        playerManagementService.addToTeam(teamId, playerId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/teams/{teamId}/players/{playerId}")
    public ResponseEntity<Void> removePlayerFromTeam(
        @PathVariable Long teamId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        checkRosterEditAccess(authentication, teamId);
        playerManagementService.removeFromTeam(teamId, playerId);
        return ResponseEntity.noContent().build();
    }

    private void checkRosterEditAccess(Authentication authentication, Long teamId) {
        Long userId = currentUserId(authentication);
        boolean isSuperAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (!isSuperAdmin && !accessControlService.hasTeamPermission(userId, teamId, "ROSTER_EDIT")) {
            throw new AccessDeniedException("Нет прав редактировать состав этой команды.");
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    public record PlayerUpsertRequest(
        @NotBlank(message = "ФИО игрока обязательно.")
        @Size(max = 255, message = "ФИО игрока не должно превышать 255 символов.")
        String fullName,
        LocalDate birthDate,
        @Size(max = 255, message = "Место жительства не должно превышать 255 символов.")
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl
    ) {
        PlayerUpsert toCommand() {
            return new PlayerUpsert(fullName, birthDate, residence, isGoalkeeper, photoDataUrl);
        }
    }
}
