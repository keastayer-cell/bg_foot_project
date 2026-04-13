package com.footballstats.backend.controller;

import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.dto.auth.UserResponse;
import com.footballstats.backend.dto.auth.AssignTeamScopeRequest;
import com.footballstats.backend.dto.auth.UserAccessResponse;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.AccessControlService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/access")
public class AdminAccessController {

    private final AccessControlService accessControlService;

    public AdminAccessController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @PostMapping("/users/{userId}/roles/{roleCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> assignRole(
        @PathVariable Long userId,
        @PathVariable RoleCode roleCode,
        Authentication authentication
    ) {
        accessControlService.assignRole(currentUserId(authentication), userId, roleCode);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/roles/{roleCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> revokeRole(@PathVariable Long userId, @PathVariable RoleCode roleCode) {
        accessControlService.revokeRole(userId, roleCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/team-scopes")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> assignTeamScope(
        @PathVariable Long userId,
        @Valid @RequestBody AssignTeamScopeRequest request,
        Authentication authentication
    ) {
        accessControlService.assignTeamScope(currentUserId(authentication), userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/team-scopes/{teamId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> revokeTeamScope(@PathVariable Long userId, @PathVariable Long teamId) {
        accessControlService.revokeTeamScope(userId, teamId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserAccessResponse> getUserAccess(@PathVariable Long userId) {
        return ResponseEntity.ok(accessControlService.getUserAccess(userId));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<UserResponse>> listUsers(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String role,
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(pagenum, 0),
            Math.min(Math.max(pagesize, 1), 100),
            Sort.by(Sort.Direction.ASC, "email")
        );

        Page<UserResponse> page = accessControlService.searchUsers(
            toLikePattern(name),
            toLikePattern(email),
            role,
            pageable
        ).map(user -> new UserResponse(
            user.getId(),
            false,
            user.getEmail(),
            user.getName(),
            accessControlService.getRoleCodes(user.getId())
        ));

        return ResponseEntity.ok(page);
    }

    @GetMapping("/me")
    public ResponseEntity<UserAccessResponse> getMyAccess(Authentication authentication) {
        AppUserPrincipal principal = currentPrincipal(authentication);
        Long userId = principal.getUserId();

        if (userId != null && userId > 0) {
            return ResponseEntity.ok(accessControlService.getUserAccess(userId));
        }

        List<String> roles = principal.getAuthorities().stream()
            .map(authority -> authority.getAuthority().replace("ROLE_", ""))
            .sorted()
            .toList();

        return ResponseEntity.ok(new UserAccessResponse(
            0L,
            principal.getUsername(),
            principal.getName(),
            roles,
            accessControlService.buildRoleAnnotations(roles),
            List.of()
        ));
    }

    private Long currentUserId(Authentication authentication) {
        return currentPrincipal(authentication).getUserId();
    }

    private AppUserPrincipal currentPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal;
        }
        throw new IllegalArgumentException("Не удалось определить пользователя из токена.");
    }

    private String toLikePattern(String raw) {
        String normalized = String.valueOf(raw == null ? "" : raw).trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }
}
