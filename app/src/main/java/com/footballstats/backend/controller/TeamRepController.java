package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.TeamRepService;
import com.footballstats.backend.service.TeamRepTransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/team-rep")
public class TeamRepController {

    private final TeamRepService teamRepService;
    private final TeamRepTransferService teamRepTransferService;

    public TeamRepController(TeamRepService teamRepService, TeamRepTransferService teamRepTransferService) {
        this.teamRepService = teamRepService;
        this.teamRepTransferService = teamRepTransferService;
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @GetMapping("/seasons")
    public ResponseEntity<List<TeamRepService.TeamRepSeasonData>> listSeasons(
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.listAvailableSeasons(currentApplicationActor(authentication), teamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @GetMapping("/players")
    public ResponseEntity<List<TeamRepService.TeamRepPlayerData>> listPlayers(
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.listTeamPlayers(currentApplicationActor(authentication), teamId));
    }

    @PreAuthorize("hasRole('TEAM_REP')")
    @PostMapping("/players")
    public ResponseEntity<TeamRepService.TeamRepPlayerData> createPlayer(
        @Valid @RequestBody TeamRepPlayerUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamRepService.createPlayer(
            currentUserId(authentication),
            new TeamRepService.TeamRepPlayerDraft(request.fullName(), request.birthDate(), request.residence(), request.isGoalkeeper(), request.photoDataUrl())
        ));
    }

    @PreAuthorize("hasRole('TEAM_REP')")
    @PutMapping("/players/{playerId}")
    public ResponseEntity<TeamRepService.TeamRepPlayerData> updatePlayer(
        @PathVariable Long playerId,
        @Valid @RequestBody TeamRepPlayerUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.updatePlayer(
            currentUserId(authentication),
            playerId,
            new TeamRepService.TeamRepPlayerDraft(request.fullName(), request.birthDate(), request.residence(), request.isGoalkeeper(), request.photoDataUrl())
        ));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @GetMapping("/seasons/{seasonId}/players")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> getSeasonPlayers(
        @PathVariable Long seasonId,
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.getSeasonPlayers(currentApplicationActor(authentication), seasonId, teamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @PutMapping("/seasons/{seasonId}/players")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> replaceSeasonPlayers(
        @PathVariable Long seasonId,
        @Valid @RequestBody TeamRepSeasonPlayersUpsertRequest request,
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.replaceSeasonPlayers(currentApplicationActor(authentication), seasonId, request.playerIds(), teamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @PostMapping("/seasons/{seasonId}/players")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> addSeasonPlayers(
        @PathVariable Long seasonId,
        @Valid @RequestBody TeamRepSeasonPlayersUpsertRequest request,
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.addSeasonPlayers(currentApplicationActor(authentication), seasonId, request.playerIds(), teamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @PostMapping("/seasons/{seasonId}/players/{playerId}")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> addSeasonPlayer(
        @PathVariable Long seasonId,
        @PathVariable Long playerId,
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.addSeasonPlayer(currentApplicationActor(authentication), seasonId, playerId, teamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @DeleteMapping("/seasons/{seasonId}/players/{playerId}")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> removeSeasonPlayer(
        @PathVariable Long seasonId,
        @PathVariable Long playerId,
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.removeSeasonPlayer(currentApplicationActor(authentication), seasonId, playerId, teamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN')")
    @PostMapping("/seasons/{seasonId}/submit")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> submitSeasonApplication(
        @PathVariable Long seasonId,
        @RequestParam(required = false) Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.submitSeasonApplication(currentApplicationActor(authentication), seasonId, teamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN','REFEREE')")
    @GetMapping("/seasons/{seasonId}/transfers")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> getSeasonTransfers(
        @PathVariable Long seasonId,
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.getSeasonTransfers(currentTransferActor(authentication), seasonId, pagenum, pagesize));
    }

    @PreAuthorize("hasRole('TEAM_REP')")
    @GetMapping("/transfers/incoming-pending")
    public ResponseEntity<TeamRepTransferService.IncomingTransferNotificationsData> getIncomingPendingTransfers(
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.getIncomingPendingTransfers(currentUserId(authentication), pagenum, pagesize));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN','REFEREE')")
    @GetMapping("/seasons/{seasonId}/transfer-candidates/{fromTeamId}")
    public ResponseEntity<List<TeamRepTransferService.TeamRepTransferCandidateData>> listTransferCandidates(
        @PathVariable Long seasonId,
        @PathVariable Long fromTeamId,
        @RequestParam(required = false) Long toTeamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.listTransferCandidates(currentTransferActor(authentication), seasonId, fromTeamId, toTeamId));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN','REFEREE')")
    @PostMapping("/seasons/{seasonId}/transfers")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> createTransferRequest(
        @PathVariable Long seasonId,
        @Valid @RequestBody TeamRepTransferRequestCreateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamRepTransferService.createTransferRequest(
            currentTransferActor(authentication),
            seasonId,
            request.fromTeamId(),
            request.toTeamId(),
            request.playerId(),
            request.requestComment()
        ));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN','REFEREE')")
    @PostMapping("/transfers/{requestId}/approve")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> approveTransferRequest(
        @PathVariable Long requestId,
        @RequestBody(required = false) TeamRepTransferDecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.approveTransferRequest(
            currentTransferActor(authentication),
            requestId,
            request == null ? null : request.decisionComment()
        ));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN','REFEREE')")
    @PostMapping("/transfers/{requestId}/reject")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> rejectTransferRequest(
        @PathVariable Long requestId,
        @RequestBody(required = false) TeamRepTransferDecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.rejectTransferRequest(
            currentTransferActor(authentication),
            requestId,
            request == null ? null : request.decisionComment()
        ));
    }

    @PreAuthorize("hasAnyRole('TEAM_REP','SUPER_ADMIN','REFEREE')")
    @PostMapping("/transfers/{requestId}/revoke")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> revokeTransferRequest(
        @PathVariable Long requestId,
        @RequestBody(required = false) TeamRepTransferDecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.revokeTransferRequest(
            currentTransferActor(authentication),
            requestId,
            request == null ? null : request.decisionComment()
        ));
    }

    private TeamRepTransferService.TransferActor currentTransferActor(Authentication authentication) {
        boolean teamRep = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_TEAM_REP".equals(authority.getAuthority()));
        boolean superAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
        boolean referee = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_REFEREE".equals(authority.getAuthority()));
        return new TeamRepTransferService.TransferActor(currentUserId(authentication), teamRep, superAdmin, referee);
    }

    private TeamRepService.TeamRepActor currentApplicationActor(Authentication authentication) {
        boolean superAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
        return new TeamRepService.TeamRepActor(currentUserId(authentication), superAdmin);
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    public record TeamRepPlayerUpsertRequest(
        @NotBlank(message = "ФИО игрока обязательно.") @jakarta.validation.constraints.Size(max = 255) String fullName,
        LocalDate birthDate,
        @jakarta.validation.constraints.Size(max = 255) String residence,
        boolean isGoalkeeper,
        String photoDataUrl
    ) {}

    public record TeamRepSeasonPlayersUpsertRequest(
        @jakarta.validation.constraints.NotNull(message = "Список игроков обязателен.") List<@jakarta.validation.constraints.NotNull Long> playerIds
    ) {}

    public record TeamRepTransferRequestCreateRequest(
        Long fromTeamId,
        @jakarta.validation.constraints.NotNull(message = "Команда назначения обязательна.") Long toTeamId,
        @jakarta.validation.constraints.NotNull(message = "Игрок обязателен.") Long playerId,
        @jakarta.validation.constraints.Size(max = 2000) String requestComment
    ) {}

    public record TeamRepTransferDecisionRequest(String decisionComment) {}
}
