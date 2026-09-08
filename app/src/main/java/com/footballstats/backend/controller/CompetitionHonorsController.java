package com.footballstats.backend.controller;

import com.footballstats.backend.service.CompetitionHonorsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CompetitionHonorsController {
    private final CompetitionHonorsService service;
    public CompetitionHonorsController(CompetitionHonorsService service) { this.service = service; }

    @GetMapping("/api/hall-of-fame")
    public ResponseEntity<List<CompetitionHonorsService.HallEntryData>> hallOfFame() {
        return ResponseEntity.ok(service.hallOfFame());
    }

    @GetMapping("/api/seasons/{seasonId}/competitions/{competitionId}/honors")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionHonorsService.EditorData> editor(@PathVariable Long seasonId, @PathVariable Long competitionId) {
        return ResponseEntity.ok(service.editor(seasonId, competitionId));
    }

    @PutMapping("/api/seasons/{seasonId}/competitions/{competitionId}/honors")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionHonorsService.EditorData> save(
        @PathVariable Long seasonId, @PathVariable Long competitionId,
        @RequestBody CompetitionHonorsService.HonorsUpdate update
    ) {
        return ResponseEntity.ok(service.save(seasonId, competitionId, update));
    }
}
