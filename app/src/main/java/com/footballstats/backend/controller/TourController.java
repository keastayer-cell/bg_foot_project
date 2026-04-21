package com.footballstats.backend.controller;

import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.TourService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/tours")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @GetMapping
    public ResponseEntity<List<TourResponse>> listTours(
        @RequestParam(name = "season_id") Long seasonId,
        @RequestParam(name = "active_flag", defaultValue = "1") Integer activeFlag,
        @RequestParam(name = "published_flag", required = false) Integer publishedFlag
    ) {
        boolean includeInactive = Integer.valueOf(0).equals(activeFlag);
        Boolean publishedOnly = publishedFlag == null ? null : Integer.valueOf(1).equals(publishedFlag);
        return ResponseEntity.ok(tourService.listTours(seasonId, includeInactive, publishedOnly).stream().map(this::toResponse).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<TourResponse> createTour(
        @Valid @RequestBody TourCreateRequest request,
        Authentication authentication
    ) {
        Tour tour = tourService.createTour(request.seasonId(), request.name(), currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tour));
    }

    @GetMapping("/{tourId}/matches")
    public ResponseEntity<List<TourMatchResponse>> listMatches(
        @PathVariable Long tourId,
        @RequestParam(name = "active_flag", defaultValue = "1") Integer activeFlag
    ) {
        boolean includeInactive = Integer.valueOf(0).equals(activeFlag);
        return ResponseEntity.ok(tourService.listMatches(tourId, includeInactive).stream().map(this::toMatchResponse).toList());
    }

    @PostMapping("/{tourId}/matches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<TourMatchResponse> createMatch(
        @PathVariable Long tourId,
        @Valid @RequestBody TourMatchCreateRequest request,
        Authentication authentication
    ) {
        TourMatch match = tourService.createMatch(
            tourId,
            request.homeTeamId(),
            request.awayTeamId(),
            request.kickoffAt(),
            currentUserId(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toMatchResponse(match));
    }

    @DeleteMapping("/{tourId}/matches/{matchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<Void> deleteMatch(
        @PathVariable Long tourId,
        @PathVariable Long matchId,
        Authentication authentication
    ) {
        tourService.deleteMatch(tourId, matchId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{tourId}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<TourResponse> publishTour(@PathVariable Long tourId, Authentication authentication) {
        return ResponseEntity.ok(toResponse(tourService.publishTour(tourId, currentUserId(authentication))));
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    private TourResponse toResponse(Tour tour) {
        return new TourResponse(
            tour.getId(),
            tour.getSeason().getId(),
            tour.getSeason().getName(),
            tour.getName(),
            tour.getStageType(),
            tour.getRoundNumber(),
            tour.getSortOrder(),
            tour.isPublished(),
            tour.isActive(),
            tour.getCreatedAt(),
            tour.getUpdatedAt()
        );
    }

    private TourMatchResponse toMatchResponse(TourMatch match) {
        Integer homeScore = match.getProtocol() == null ? null : match.getProtocol().getHomeScore();
        Integer awayScore = match.getProtocol() == null ? null : match.getProtocol().getAwayScore();
        MatchProtocolStatus protocolStatus = match.getProtocol() == null || match.getProtocol().getStatus() == null
            ? MatchProtocolStatus.SCHEDULED
            : match.getProtocol().getStatus();
        return new TourMatchResponse(
            match.getId(),
            match.getTour().getId(),
            match.getHomeTeam().getId(),
            match.getHomeTeam().getName(),
            match.getAwayTeam().getId(),
            match.getAwayTeam().getName(),
            match.getKickoffAt(),
            match.isActive(),
            protocolStatus,
            homeScore,
            awayScore
        );
    }

    public record TourResponse(
        Long id,
        Long seasonId,
        String seasonName,
        String name,
        String stageType,
        Integer roundNumber,
        Integer sortOrder,
        boolean published,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    public record TourMatchResponse(
        Long id,
        Long tourId,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        OffsetDateTime kickoffAt,
        boolean active,
        MatchProtocolStatus protocolStatus,
        Integer homeScore,
        Integer awayScore
    ) {}

    public record TourCreateRequest(
        @NotNull(message = "seasonId обязателен.") Long seasonId,
        @NotBlank(message = "Название тура обязательно.") String name
    ) {}

    public record TourMatchCreateRequest(
        @NotNull(message = "homeTeamId обязателен.") Long homeTeamId,
        @NotNull(message = "awayTeamId обязателен.") Long awayTeamId,
        @NotNull(message = "kickoffAt обязателен.") OffsetDateTime kickoffAt
    ) {}
}