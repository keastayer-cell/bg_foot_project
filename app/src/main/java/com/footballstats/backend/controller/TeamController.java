package com.footballstats.backend.controller;

import com.footballstats.backend.domain.Team;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.TeamService;
import com.footballstats.backend.service.TeamService.TeamUpsertData;
import com.footballstats.backend.service.MediaAssetService;
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

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final MediaAssetService mediaAssetService;

    public TeamController(TeamService teamService, MediaAssetService mediaAssetService) {
        this.teamService = teamService;
        this.mediaAssetService = mediaAssetService;
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> listTeams(
        @RequestParam(name = "active_flag", defaultValue = "1") Integer activeFlag,
        @RequestParam(name = "season_id", required = false) Long seasonId
    ) {
        boolean includeInactive = Integer.valueOf(0).equals(activeFlag);
        List<TeamResponse> teams = teamService.listTeams(includeInactive, seasonId).stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(teams);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TeamResponse> createTeam(
        @Valid @RequestBody TeamUpsertRequest request,
        Authentication authentication
    ) {
        Team team = teamService.createTeam(toData(request), currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(team));
    }

    @PutMapping("/{teamId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TeamResponse> updateTeam(
        @PathVariable Long teamId,
        @Valid @RequestBody TeamUpsertRequest request,
        Authentication authentication
    ) {
        Team team = teamService.updateTeam(teamId, toData(request), currentUserId(authentication));
        return ResponseEntity.ok(toResponse(team));
    }

    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TeamResponse> deactivateTeam(@PathVariable Long teamId, Authentication authentication) {
        Team team = teamService.deactivateTeam(teamId, currentUserId(authentication));
        return ResponseEntity.ok(toResponse(team));
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    private TeamUpsertData toData(TeamUpsertRequest request) {
        return new TeamUpsertData(request.name(), request.shortName(), request.city(), request.logoDataUrl());
    }

    private TeamResponse toResponse(Team team) {
        String logoDataUrl = mediaAssetService.loadDataUrl(
            MediaAssetService.OWNER_TEAM,
            team.getId(),
            MediaAssetService.KIND_TEAM_LOGO
        );
        return new TeamResponse(
            team.getId(),
            team.getName(),
            team.getShortName(),
            team.getCity(),
            logoDataUrl,
            team.isActive(),
            team.getCreatedByUserId(),
            team.getUpdatedByUserId(),
            team.getCreatedAt(),
            team.getUpdatedAt()
        );
    }

    public record TeamResponse(
        Long id,
        String name,
        String shortName,
        String city,
        String logoDataUrl,
        boolean active,
        Long createdByUserId,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    public record TeamUpsertRequest(
        @NotBlank(message = "Название команды обязательно.") String name,
        String shortName,
        String city,
        String logoDataUrl
    ) {}
}
