package com.footballstats.backend.controller;

import com.footballstats.backend.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<List<AdminDashboardService.DashboardTask>> getDashboard(Authentication authentication, @org.springframework.web.bind.annotation.RequestParam Long seasonId, @org.springframework.web.bind.annotation.RequestParam Long competitionId) {
        return ResponseEntity.ok(service.getTasks(authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN")), seasonId, competitionId));
    }
}
