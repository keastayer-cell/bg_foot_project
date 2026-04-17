package com.footballstats.backend.controller;

import com.footballstats.backend.domain.Referee;
import com.footballstats.backend.repository.RefereeRepository;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.MediaAssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/referees")
public class RefereeController {

    private final RefereeRepository refereeRepository;
    private final MediaAssetService mediaAssetService;

    public RefereeController(RefereeRepository refereeRepository, MediaAssetService mediaAssetService) {
        this.refereeRepository = refereeRepository;
        this.mediaAssetService = mediaAssetService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RefereeResponse>> listReferees(@RequestParam(name = "active_flag", defaultValue = "1") int activeFlag) {
        List<Referee> referees = activeFlag == 0
            ? refereeRepository.findAllByOrderByFullNameAsc()
            : refereeRepository.findAllByActiveTrueOrderByFullNameAsc();
        return ResponseEntity.ok(referees.stream().map(this::toResponse).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<RefereeResponse> createReferee(@Valid @RequestBody RefereeUpsertRequest request, Authentication authentication) {
        String fullName = normalizeRequiredName(request.fullName());
        if (refereeRepository.existsByFullNameIgnoreCase(fullName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Судья с таким ФИО уже существует.");
        }

        Long actorUserId = currentUserId(authentication);
        Referee referee = new Referee();
        referee.setFullName(fullName);
        referee.setCity(normalizeOptional(request.city()));
        referee.setBirthDate(request.birthDate());
        referee.setCreatedByUserId(actorUserId);
        referee.setUpdatedByUserId(actorUserId);
        referee.setUpdatedAt(OffsetDateTime.now());
        Referee saved = refereeRepository.save(referee);

        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_REFEREE,
            saved.getId(),
            MediaAssetService.KIND_REFEREE_PHOTO,
            request.photoDataUrl(),
            actorUserId
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = refereeRepository.save(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{refereeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @Transactional
    public ResponseEntity<RefereeResponse> updateReferee(
        @PathVariable Long refereeId,
        @Valid @RequestBody RefereeUpsertRequest request,
        Authentication authentication
    ) {
        Referee referee = refereeRepository.findById(refereeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Судья не найден."));

        String fullName = normalizeRequiredName(request.fullName());
        refereeRepository.findByFullNameIgnoreCase(fullName).ifPresent(existing -> {
            if (!existing.getId().equals(refereeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Судья с таким ФИО уже существует.");
            }
        });

        referee.setFullName(fullName);
        referee.setCity(normalizeOptional(request.city()));
        referee.setBirthDate(request.birthDate());
        referee.setUpdatedByUserId(currentUserId(authentication));
        referee.setUpdatedAt(OffsetDateTime.now());
        Referee saved = refereeRepository.save(referee);

        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_REFEREE,
            saved.getId(),
            MediaAssetService.KIND_REFEREE_PHOTO,
            request.photoDataUrl(),
            currentUserId(authentication)
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = refereeRepository.save(saved);
        }

        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{refereeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<Void> deactivateReferee(@PathVariable Long refereeId, Authentication authentication) {
        Referee referee = refereeRepository.findById(refereeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Судья не найден."));
        referee.setActive(false);
        referee.setUpdatedByUserId(currentUserId(authentication));
        referee.setUpdatedAt(OffsetDateTime.now());
        refereeRepository.save(referee);
        return ResponseEntity.noContent().build();
    }

    private RefereeResponse toResponse(Referee referee) {
        return new RefereeResponse(
            referee.getId(),
            referee.getFullName(),
            referee.getCity(),
            referee.getBirthDate(),
            mediaAssetService.loadDataUrl(MediaAssetService.OWNER_REFEREE, referee.getId(), MediaAssetService.KIND_REFEREE_PHOTO),
            referee.isActive()
        );
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    private String normalizeRequiredName(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ФИО судьи обязательно.");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record RefereeResponse(
        Long id,
        String fullName,
        String city,
        LocalDate birthDate,
        String photoDataUrl,
        boolean active
    ) {}

    public record RefereeUpsertRequest(
        @NotBlank(message = "ФИО судьи обязательно.") String fullName,
        String city,
        LocalDate birthDate,
        String photoDataUrl
    ) {}
}