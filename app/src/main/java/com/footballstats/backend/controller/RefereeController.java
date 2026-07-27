package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.RefereeManagementService;
import com.footballstats.backend.service.RefereeManagementService.RefereeData;
import com.footballstats.backend.service.RefereeManagementService.RefereeUpsert;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/referees")
public class RefereeController {

    private final RefereeManagementService refereeManagementService;

    public RefereeController(RefereeManagementService refereeManagementService) {
        this.refereeManagementService = refereeManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<List<RefereeData>> listReferees(
        @RequestParam(name = "active_flag", defaultValue = "1") int activeFlag
    ) {
        return ResponseEntity.ok(refereeManagementService.list(activeFlag));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<RefereeData> createReferee(
        @Valid @RequestBody RefereeUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(refereeManagementService.create(request.toCommand(), currentUserId(authentication)));
    }

    @PutMapping("/{refereeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<RefereeData> updateReferee(
        @PathVariable Long refereeId,
        @Valid @RequestBody RefereeUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(refereeManagementService.update(refereeId, request.toCommand(), currentUserId(authentication)));
    }

    @DeleteMapping("/{refereeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<Void> deactivateReferee(@PathVariable Long refereeId, Authentication authentication) {
        refereeManagementService.deactivate(refereeId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    public record RefereeUpsertRequest(
        @NotBlank(message = "ФИО судьи обязательно.")
        @Size(max = 255, message = "ФИО судьи не должно превышать 255 символов.")
        String fullName,
        @Size(max = 255, message = "Название города не должно превышать 255 символов.")
        String city,
        LocalDate birthDate,
        String photoDataUrl
    ) {
        RefereeUpsert toCommand() {
            return new RefereeUpsert(fullName, city, birthDate, photoDataUrl);
        }
    }
}
