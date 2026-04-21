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
@PreAuthorize("hasRole('TEAM_REP')")
public class TeamRepController {

    private final TeamRepService teamRepService;
    private final TeamRepTransferService teamRepTransferService;

    public TeamRepController(TeamRepService teamRepService, TeamRepTransferService teamRepTransferService) {
        this.teamRepService = teamRepService;
        this.teamRepTransferService = teamRepTransferService;
    }

    @GetMapping("/seasons")
    public ResponseEntity<List<TeamRepService.TeamRepSeasonData>> listSeasons(Authentication authentication) {
        return ResponseEntity.ok(teamRepService.listAvailableSeasons(currentUserId(authentication)));
    }

    @GetMapping("/players")
    public ResponseEntity<List<TeamRepService.TeamRepPlayerData>> listPlayers(Authentication authentication) {
        return ResponseEntity.ok(teamRepService.listTeamPlayers(currentUserId(authentication)));
    }

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

    @GetMapping("/seasons/{seasonId}/players")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> getSeasonPlayers(
        @PathVariable Long seasonId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.getSeasonPlayers(currentUserId(authentication), seasonId));
    }

    @PutMapping("/seasons/{seasonId}/players")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> replaceSeasonPlayers(
        @PathVariable Long seasonId,
        @Valid @RequestBody TeamRepSeasonPlayersUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.replaceSeasonPlayers(currentUserId(authentication), seasonId, request.playerIds()));
    }

    @PostMapping("/seasons/{seasonId}/players")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> addSeasonPlayers(
        @PathVariable Long seasonId,
        @Valid @RequestBody TeamRepSeasonPlayersUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.addSeasonPlayers(currentUserId(authentication), seasonId, request.playerIds()));
    }

    @PostMapping("/seasons/{seasonId}/players/{playerId}")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> addSeasonPlayer(
        @PathVariable Long seasonId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.addSeasonPlayer(currentUserId(authentication), seasonId, playerId));
    }

    @DeleteMapping("/seasons/{seasonId}/players/{playerId}")
    public ResponseEntity<TeamRepService.TeamRepSeasonPlayersData> removeSeasonPlayer(
        @PathVariable Long seasonId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepService.removeSeasonPlayer(currentUserId(authentication), seasonId, playerId));
    }

    @GetMapping("/seasons/{seasonId}/transfers")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> getSeasonTransfers(
        @PathVariable Long seasonId,
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.getSeasonTransfers(currentUserId(authentication), seasonId, pagenum, pagesize));
    }

    @GetMapping("/transfers/incoming-pending")
    public ResponseEntity<TeamRepTransferService.IncomingTransferNotificationsData> getIncomingPendingTransfers(
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.getIncomingPendingTransfers(currentUserId(authentication), pagenum, pagesize));
    }

    @GetMapping("/seasons/{seasonId}/transfer-candidates/{fromTeamId}")
    public ResponseEntity<List<TeamRepTransferService.TeamRepTransferCandidateData>> listTransferCandidates(
        @PathVariable Long seasonId,
        @PathVariable Long fromTeamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.listTransferCandidates(currentUserId(authentication), seasonId, fromTeamId));
    }

    @PostMapping("/seasons/{seasonId}/transfers")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> createTransferRequest(
        @PathVariable Long seasonId,
        @Valid @RequestBody TeamRepTransferRequestCreateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamRepTransferService.createTransferRequest(
            currentUserId(authentication),
            seasonId,
            request.fromTeamId(),
            request.playerId(),
            request.requestComment()
        ));
    }

    @PostMapping("/transfers/{requestId}/approve")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> approveTransferRequest(
        @PathVariable Long requestId,
        @RequestBody(required = false) TeamRepTransferDecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.approveTransferRequest(
            currentUserId(authentication),
            requestId,
            request == null ? null : request.decisionComment()
        ));
    }

    @PostMapping("/transfers/{requestId}/reject")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> rejectTransferRequest(
        @PathVariable Long requestId,
        @RequestBody(required = false) TeamRepTransferDecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.rejectTransferRequest(
            currentUserId(authentication),
            requestId,
            request == null ? null : request.decisionComment()
        ));
    }

    @PostMapping("/transfers/{requestId}/revoke")
    public ResponseEntity<TeamRepTransferService.TeamRepTransferOverviewData> revokeTransferRequest(
        @PathVariable Long requestId,
        @RequestBody(required = false) TeamRepTransferDecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(teamRepTransferService.revokeTransferRequest(
            currentUserId(authentication),
            requestId,
            request == null ? null : request.decisionComment()
        ));
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    public record TeamRepPlayerUpsertRequest(
        @NotBlank(message = "ФИО игрока обязательно.") String fullName,
        LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl
    ) {}

    public record TeamRepSeasonPlayersUpsertRequest(List<Long> playerIds) {}

    public record TeamRepTransferRequestCreateRequest(Long fromTeamId, Long playerId, String requestComment) {}

    public record TeamRepTransferDecisionRequest(String decisionComment) {}
}