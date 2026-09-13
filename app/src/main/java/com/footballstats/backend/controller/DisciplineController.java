package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.DisciplineCenterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class DisciplineController {
    private final DisciplineCenterService service;
    public DisciplineController(DisciplineCenterService service) { this.service = service; }

    @GetMapping("/api/discipline")
    public ResponseEntity<Map<String, Object>> get(@RequestParam Long competitionId) {
        return ResponseEntity.ok(service.getCenter(competitionId));
    }

    @PostMapping("/api/admin/discipline/adjustments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<Map<String, Object>> adjust(@Valid @RequestBody AdjustmentRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.adjust(request.competitionId(), request.playerId(), request.teamId(),
            request.oldRemainingMatches(), request.newRemainingMatches(), request.reason(), currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal ? principal.getUserId() : null;
    }

    public record AdjustmentRequest(@NotNull Long competitionId, @NotNull Long playerId, @NotNull Long teamId,
        @Min(0) int oldRemainingMatches, @Min(0) int newRemainingMatches, @NotBlank String reason) {}
}
