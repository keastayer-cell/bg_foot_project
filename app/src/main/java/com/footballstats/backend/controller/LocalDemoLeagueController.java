package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.LocalDemoLeagueService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo/league")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "demo.tools.enabled", havingValue = "true")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class LocalDemoLeagueController {

    private final LocalDemoLeagueService service;

    public LocalDemoLeagueController(LocalDemoLeagueService service) {
        this.service = service;
    }

    @GetMapping
    public LocalDemoLeagueService.DemoLeagueStatus getStatus() {
        return service.getStatus();
    }

    @PostMapping
    public LocalDemoLeagueService.DemoLeagueStatus createBase(Authentication authentication) {
        return service.createBase(currentUserId(authentication));
    }

    @PostMapping("/schedule")
    public LocalDemoLeagueService.DemoLeagueStatus createSchedule(Authentication authentication) {
        return service.createSchedule(currentUserId(authentication));
    }

    @PostMapping("/results")
    public LocalDemoLeagueService.DemoLeagueStatus addResults(Authentication authentication) {
        return service.addResults(currentUserId(authentication));
    }

    @PostMapping("/transfers")
    public LocalDemoLeagueService.DemoLeagueStatus prepareTransfers(Authentication authentication) {
        return service.prepareTransfers(currentUserId(authentication));
    }

    @PostMapping("/playoffs")
    public LocalDemoLeagueService.DemoLeagueStatus preparePlayoffs(Authentication authentication) {
        return service.preparePlayoffs(currentUserId(authentication));
    }

    @DeleteMapping
    public LocalDemoLeagueService.DemoLeagueStatus reset() {
        return service.reset();
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("Не удалось определить текущего пользователя.");
        }
        return principal.getUserId();
    }
}
