package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.SeasonApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/season-applications")
public class SeasonApplicationController {

    private final SeasonApplicationService seasonApplicationService;

    public SeasonApplicationController(SeasonApplicationService seasonApplicationService) {
        this.seasonApplicationService = seasonApplicationService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @GetMapping
    public ResponseEntity<SeasonApplicationService.ReviewQueueData> getQueue(
        @RequestParam(required = false) Long seasonId
    ) {
        return ResponseEntity.ok(seasonApplicationService.getReviewQueue(seasonId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @GetMapping("/{applicationId}")
    public ResponseEntity<SeasonApplicationService.ReviewDetailsData> getDetails(@PathVariable Long applicationId) {
        return ResponseEntity.ok(seasonApplicationService.getReviewDetails(applicationId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<SeasonApplicationService.ReviewDetailsData> approve(
        @PathVariable Long applicationId,
        @RequestBody(required = false) DecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(seasonApplicationService.approve(currentUserId(authentication), applicationId, request == null ? null : request.decisionComment()));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @PostMapping("/{applicationId}/return")
    public ResponseEntity<SeasonApplicationService.ReviewDetailsData> returnToTeam(
        @PathVariable Long applicationId,
        @RequestBody(required = false) DecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(seasonApplicationService.returnToTeam(currentUserId(authentication), applicationId, request == null ? null : request.decisionComment()));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<SeasonApplicationService.ReviewDetailsData> reject(
        @PathVariable Long applicationId,
        @RequestBody(required = false) DecisionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(seasonApplicationService.reject(currentUserId(authentication), applicationId, request == null ? null : request.decisionComment()));
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    public record DecisionRequest(String decisionComment) {}
}