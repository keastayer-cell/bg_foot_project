package com.footballstats.backend.controller;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.LeagueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@RestController
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping("/api/league/overview")
    public ResponseEntity<LeagueOverviewResponse> getPublicOverview() {
        LeagueService.LeagueOverviewData overview = leagueService.getPublicOverview();
        return ResponseEntity.ok(new LeagueOverviewResponse(
            overview.officials().stream().map(this::toOfficialResponse).toList(),
            overview.venues().stream().map(this::toVenueResponse).toList(),
            overview.seasonDocuments().stream().map(this::toSeasonDocumentResponse).toList()
        ));
    }

    @GetMapping("/api/admin/league/officials")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<List<LeagueOfficialResponse>> listOfficials(
        @RequestParam(name = "active_flag", defaultValue = "1") Integer activeFlag
    ) {
        boolean includeInactive = Integer.valueOf(0).equals(activeFlag);
        return ResponseEntity.ok(leagueService.listOfficials(includeInactive).stream().map(this::toOfficialResponse).toList());
    }

    @PostMapping("/api/admin/league/officials")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<LeagueOfficialResponse> createOfficial(
        @Valid @RequestBody LeagueOfficialUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toOfficialResponse(leagueService.createOfficial(
            new LeagueService.LeagueOfficialUpsertData(
                request.fullName(),
                request.positionTitle(),
                request.bio(),
                request.photoDataUrl(),
                request.sortOrder()
            ),
            currentUserId(authentication)
        )));
    }

    @PutMapping("/api/admin/league/officials/{officialId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<LeagueOfficialResponse> updateOfficial(
        @PathVariable Long officialId,
        @Valid @RequestBody LeagueOfficialUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toOfficialResponse(leagueService.updateOfficial(
            officialId,
            new LeagueService.LeagueOfficialUpsertData(
                request.fullName(),
                request.positionTitle(),
                request.bio(),
                request.photoDataUrl(),
                request.sortOrder()
            ),
            currentUserId(authentication)
        )));
    }

    @DeleteMapping("/api/admin/league/officials/{officialId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<Void> deactivateOfficial(@PathVariable Long officialId, Authentication authentication) {
        leagueService.deactivateOfficial(officialId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/league/venues")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<List<LeagueVenueResponse>> listVenues(
        @RequestParam(name = "active_flag", defaultValue = "1") Integer activeFlag
    ) {
        boolean includeInactive = Integer.valueOf(0).equals(activeFlag);
        return ResponseEntity.ok(leagueService.listVenues(includeInactive).stream().map(this::toVenueResponse).toList());
    }

    @PostMapping("/api/admin/league/venues")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<LeagueVenueResponse> createVenue(
        @Valid @RequestBody LeagueVenueUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toVenueResponse(leagueService.createVenue(
            new LeagueService.LeagueVenueUpsertData(
                request.name(),
                request.shortLabel(),
                request.address(),
                request.description(),
                request.photoDataUrl(),
                request.sortOrder()
            ),
            currentUserId(authentication)
        )));
    }

    @PutMapping("/api/admin/league/venues/{venueId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<LeagueVenueResponse> updateVenue(
        @PathVariable Long venueId,
        @Valid @RequestBody LeagueVenueUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toVenueResponse(leagueService.updateVenue(
            venueId,
            new LeagueService.LeagueVenueUpsertData(
                request.name(),
                request.shortLabel(),
                request.address(),
                request.description(),
                request.photoDataUrl(),
                request.sortOrder()
            ),
            currentUserId(authentication)
        )));
    }

    @DeleteMapping("/api/admin/league/venues/{venueId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<Void> deactivateVenue(@PathVariable Long venueId, Authentication authentication) {
        leagueService.deactivateVenue(venueId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/league/seasons/{seasonId}/regulation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<SeasonDocumentResponse> updateSeasonRegulation(
        @PathVariable Long seasonId,
        @Valid @RequestBody SeasonRegulationUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toSeasonDocumentResponse(
            leagueService.updateSeasonRegulation(seasonId, request.documentDataUrl(), currentUserId(authentication))
        ));
    }

    @DeleteMapping("/api/admin/league/seasons/{seasonId}/regulation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<SeasonDocumentResponse> deleteSeasonRegulation(
        @PathVariable Long seasonId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toSeasonDocumentResponse(leagueService.removeSeasonRegulation(seasonId, currentUserId(authentication))));
    }

    @GetMapping("/api/seasons/{seasonId}/regulation/pdf")
    public ResponseEntity<byte[]> downloadSeasonRegulation(@PathVariable Long seasonId) {
        LeagueService.RegulationPdfPayload pdf = leagueService.getSeasonRegulationPdf(seasonId);
        MediaType mediaType = MediaType.APPLICATION_PDF;
        String mimeType = pdf.mimeType();
        if (mimeType != null && !mimeType.isBlank()) {
            mediaType = MediaType.parseMediaType(Objects.requireNonNull(mimeType));
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(pdf.fileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20"))
            .contentType(Objects.requireNonNull(mediaType))
            .contentLength(pdf.bytes().length)
            .body(pdf.bytes());
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

    private LeagueOfficialResponse toOfficialResponse(LeagueService.LeagueOfficialData data) {
        return new LeagueOfficialResponse(
            data.id(),
            data.fullName(),
            data.positionTitle(),
            data.bio(),
            data.photoDataUrl(),
            data.sortOrder(),
            data.active(),
            data.createdAt(),
            data.updatedAt()
        );
    }

    private LeagueVenueResponse toVenueResponse(LeagueService.LeagueVenueData data) {
        return new LeagueVenueResponse(
            data.id(),
            data.name(),
            data.shortLabel(),
            data.address(),
            data.description(),
            data.photoDataUrl(),
            data.sortOrder(),
            data.active(),
            data.createdAt(),
            data.updatedAt()
        );
    }

    private SeasonDocumentResponse toSeasonDocumentResponse(LeagueService.SeasonDocumentData data) {
        return new SeasonDocumentResponse(
            data.seasonId(),
            data.seasonName(),
            data.seasonStatus(),
            data.seasonActive(),
            data.regulationAvailable(),
            data.regulationUpdatedAt(),
            data.regulationDownloadUrl()
        );
    }

    public record LeagueOverviewResponse(
        List<LeagueOfficialResponse> officials,
        List<LeagueVenueResponse> venues,
        List<SeasonDocumentResponse> seasonDocuments
    ) {}

    public record LeagueOfficialResponse(
        Long id,
        String fullName,
        String positionTitle,
        String bio,
        String photoDataUrl,
        Integer sortOrder,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    public record LeagueVenueResponse(
        Long id,
        String name,
        String shortLabel,
        String address,
        String description,
        String photoDataUrl,
        Integer sortOrder,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    public record SeasonDocumentResponse(
        Long seasonId,
        String seasonName,
        String seasonStatus,
        boolean seasonActive,
        boolean regulationAvailable,
        OffsetDateTime regulationUpdatedAt,
        String regulationDownloadUrl
    ) {}

    public record LeagueOfficialUpsertRequest(
        @NotBlank(message = "ФИО обязательно.") @jakarta.validation.constraints.Size(max = 255) String fullName,
        @NotBlank(message = "Должность обязательна.") @jakarta.validation.constraints.Size(max = 255) String positionTitle,
        @jakarta.validation.constraints.Size(max = 4000) String bio,
        String photoDataUrl,
        Integer sortOrder
    ) {}

    public record LeagueVenueUpsertRequest(
        @NotBlank(message = "Название площадки обязательно.") @jakarta.validation.constraints.Size(max = 255) String name,
        @jakarta.validation.constraints.Size(max = 100) String shortLabel,
        @NotBlank(message = "Адрес площадки обязателен.") @jakarta.validation.constraints.Size(max = 500) String address,
        @jakarta.validation.constraints.Size(max = 4000) String description,
        String photoDataUrl,
        Integer sortOrder
    ) {}

    public record SeasonRegulationUpsertRequest(
        @NotBlank(message = "PDF-файл положения сезона обязателен.") String documentDataUrl
    ) {}
}
