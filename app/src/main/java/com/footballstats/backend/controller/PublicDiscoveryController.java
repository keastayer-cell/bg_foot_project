package com.footballstats.backend.controller;

import com.footballstats.backend.service.PublicDiscoveryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
public class PublicDiscoveryController {
    private final PublicDiscoveryService service;

    public PublicDiscoveryController(PublicDiscoveryService service) {
        this.service = service;
    }

    @GetMapping("/api/search")
    public ResponseEntity<Map<String, Object>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(service.search(q, limit));
    }

    @GetMapping("/api/calendar")
    public ResponseEntity<Map<String, Object>> calendar(
        @RequestParam(required = false) Long seasonId,
        @RequestParam(required = false) Long competitionId,
        @RequestParam(required = false) Long teamId,
        @RequestParam(required = false) Long venueId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(service.calendar(seasonId, competitionId, teamId, venueId, from, to));
    }

    @GetMapping(value = "/api/calendar.ics", produces = "text/calendar;charset=UTF-8")
    public ResponseEntity<byte[]> calendarIcs(
        @RequestParam(required = false) Long matchId,
        @RequestParam(required = false) Long teamId,
        @RequestParam(required = false) Long competitionId
    ) {
        byte[] content = service.exportIcs(matchId, teamId, competitionId).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bgfoot-calendar.ics")
            .body(content);
    }
}
