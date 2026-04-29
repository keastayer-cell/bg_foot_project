package com.footballstats.backend.service;

import com.footballstats.backend.domain.LeagueOfficial;
import com.footballstats.backend.domain.LeagueVenue;
import com.footballstats.backend.domain.MediaAsset;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.repository.LeagueOfficialRepository;
import com.footballstats.backend.repository.LeagueVenueRepository;
import com.footballstats.backend.repository.SeasonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LeagueService {

    private final LeagueOfficialRepository leagueOfficialRepository;
    private final LeagueVenueRepository leagueVenueRepository;
    private final SeasonRepository seasonRepository;
    private final MediaAssetService mediaAssetService;

    public LeagueService(
        LeagueOfficialRepository leagueOfficialRepository,
        LeagueVenueRepository leagueVenueRepository,
        SeasonRepository seasonRepository,
        MediaAssetService mediaAssetService
    ) {
        this.leagueOfficialRepository = leagueOfficialRepository;
        this.leagueVenueRepository = leagueVenueRepository;
        this.seasonRepository = seasonRepository;
        this.mediaAssetService = mediaAssetService;
    }

    @Transactional(readOnly = true)
    public LeagueOverviewData getPublicOverview() {
        return new LeagueOverviewData(
            leagueOfficialRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toOfficialData)
                .toList(),
            leagueVenueRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toVenueData)
                .toList(),
            seasonRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .filter(season -> season.getRegulationMediaId() != null)
                .map(this::toSeasonDocumentData)
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<LeagueOfficialData> listOfficials(boolean includeInactive) {
        return (includeInactive
            ? leagueOfficialRepository.findAllByOrderBySortOrderAscIdAsc()
            : leagueOfficialRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()).stream()
            .map(this::toOfficialData)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LeagueVenueData> listVenues(boolean includeInactive) {
        return (includeInactive
            ? leagueVenueRepository.findAllByOrderBySortOrderAscIdAsc()
            : leagueVenueRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()).stream()
            .map(this::toVenueData)
            .toList();
    }

    @Transactional
    public LeagueOfficialData createOfficial(LeagueOfficialUpsertData data, Long actorUserId) {
        LeagueOfficial official = new LeagueOfficial();
        applyOfficial(official, data, actorUserId, true);
        LeagueOfficial saved = persistOfficial(official);
        saveOfficialPhoto(saved, data.photoDataUrl(), actorUserId);
        return toOfficialData(saved);
    }

    @Transactional
    public LeagueOfficialData updateOfficial(Long officialId, LeagueOfficialUpsertData data, Long actorUserId) {
        LeagueOfficial official = getOfficial(officialId);
        applyOfficial(official, data, actorUserId, false);
        LeagueOfficial saved = persistOfficial(official);
        saveOfficialPhoto(saved, data.photoDataUrl(), actorUserId);
        return toOfficialData(saved);
    }

    @Transactional
    public void deactivateOfficial(Long officialId, Long actorUserId) {
        LeagueOfficial official = getOfficial(officialId);
        official.setActive(false);
        official.setUpdatedByUserId(actorUserId);
        official.setUpdatedAt(OffsetDateTime.now());
        leagueOfficialRepository.save(official);
    }

    @Transactional
    public LeagueVenueData createVenue(LeagueVenueUpsertData data, Long actorUserId) {
        LeagueVenue venue = new LeagueVenue();
        applyVenue(venue, data, actorUserId, true);
        LeagueVenue saved = persistVenue(venue);
        saveVenuePhoto(saved, data.photoDataUrl(), actorUserId);
        return toVenueData(saved);
    }

    @Transactional
    public LeagueVenueData updateVenue(Long venueId, LeagueVenueUpsertData data, Long actorUserId) {
        LeagueVenue venue = getVenue(venueId);
        applyVenue(venue, data, actorUserId, false);
        LeagueVenue saved = persistVenue(venue);
        saveVenuePhoto(saved, data.photoDataUrl(), actorUserId);
        return toVenueData(saved);
    }

    @Transactional
    public void deactivateVenue(Long venueId, Long actorUserId) {
        LeagueVenue venue = getVenue(venueId);
        venue.setActive(false);
        venue.setUpdatedByUserId(actorUserId);
        venue.setUpdatedAt(OffsetDateTime.now());
        leagueVenueRepository.save(venue);
    }

    @Transactional
    public SeasonDocumentData updateSeasonRegulation(Long seasonId, String documentDataUrl, Long actorUserId) {
        Season season = getSeason(seasonId);
        String normalizedDataUrl = normalizeOptional(documentDataUrl);
        if (normalizedDataUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF-файл положения сезона обязателен.");
        }

        MediaAssetService.DataUrlPayload payload = mediaAssetService.decodeDataUrl(normalizedDataUrl);
        String mimeType = payload.mimeType() == null ? "" : payload.mimeType().toLowerCase();
        if (!MediaType.APPLICATION_PDF_VALUE.equals(mimeType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно загрузить PDF-файл положения сезона.");
        }

        MediaAsset asset = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_SEASON,
            season.getId(),
            MediaAssetService.KIND_SEASON_REGULATION_PDF,
            normalizedDataUrl,
            actorUserId
        );
        if (asset == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось сохранить PDF-файл положения сезона.");
        }

        season.setRegulationMediaId(asset.getId());
        season.setRegulationUpdatedAt(OffsetDateTime.now());
        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(OffsetDateTime.now());
        return toSeasonDocumentData(seasonRepository.save(season));
    }

    @Transactional
    public SeasonDocumentData removeSeasonRegulation(Long seasonId, Long actorUserId) {
        Season season = getSeason(seasonId);
        season.setRegulationMediaId(null);
        season.setRegulationUpdatedAt(null);
        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(OffsetDateTime.now());
        return toSeasonDocumentData(seasonRepository.save(season));
    }

    @Transactional(readOnly = true)
    public RegulationPdfPayload getSeasonRegulationPdf(Long seasonId) {
        Season season = getSeason(seasonId);
        if (season.getRegulationMediaId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Положение сезона не загружено.");
        }

        MediaAsset asset = mediaAssetService.loadLatestAsset(
            MediaAssetService.OWNER_SEASON,
            season.getId(),
            MediaAssetService.KIND_SEASON_REGULATION_PDF
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Положение сезона не найдено."));
        MediaAssetService.DataUrlPayload payload = mediaAssetService.decodeDataUrl(asset.getDataUrl());
        return new RegulationPdfPayload(buildRegulationFileName(season.getName()), payload.mimeType(), payload.bytes());
    }

    private LeagueOfficial getOfficial(Long officialId) {
        return leagueOfficialRepository.findById(java.util.Objects.requireNonNull(officialId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Представитель руководства не найден."));
    }

    private LeagueVenue getVenue(Long venueId) {
        return leagueVenueRepository.findById(java.util.Objects.requireNonNull(venueId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Место проведения не найдено."));
    }

    private Season getSeason(Long seasonId) {
        return seasonRepository.findById(java.util.Objects.requireNonNull(seasonId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
    }

    @SuppressWarnings("null")
    private LeagueOfficial persistOfficial(LeagueOfficial official) {
        LeagueOfficial saved = leagueOfficialRepository.save(official);
        return saved;
    }

    @SuppressWarnings("null")
    private LeagueVenue persistVenue(LeagueVenue venue) {
        LeagueVenue saved = leagueVenueRepository.save(venue);
        return saved;
    }

    private void applyOfficial(LeagueOfficial official, LeagueOfficialUpsertData data, Long actorUserId, boolean create) {
        official.setFullName(normalizeRequired(data.fullName(), "ФИО представителя обязательно."));
        official.setPositionTitle(normalizeRequired(data.positionTitle(), "Должность обязательна."));
        official.setBio(normalizeOptional(data.bio()));
        official.setSortOrder(normalizeSortOrder(data.sortOrder()));
        official.setUpdatedByUserId(actorUserId);
        official.setUpdatedAt(OffsetDateTime.now());
        if (create) {
            official.setCreatedByUserId(actorUserId);
            official.setActive(true);
        }
    }

    private void applyVenue(LeagueVenue venue, LeagueVenueUpsertData data, Long actorUserId, boolean create) {
        venue.setName(normalizeRequired(data.name(), "Название площадки обязательно."));
        venue.setShortLabel(normalizeOptional(data.shortLabel()));
        venue.setAddress(normalizeRequired(data.address(), "Адрес площадки обязателен."));
        venue.setDescription(normalizeOptional(data.description()));
        venue.setSortOrder(normalizeSortOrder(data.sortOrder()));
        venue.setUpdatedByUserId(actorUserId);
        venue.setUpdatedAt(OffsetDateTime.now());
        if (create) {
            venue.setCreatedByUserId(actorUserId);
            venue.setActive(true);
        }
    }

    private void saveOfficialPhoto(LeagueOfficial official, String photoDataUrl, Long actorUserId) {
        MediaAsset photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_LEAGUE_OFFICIAL,
            official.getId(),
            MediaAssetService.KIND_LEAGUE_OFFICIAL_PHOTO,
            photoDataUrl,
            actorUserId
        );
        if (photo != null) {
            official.setPhotoMediaId(photo.getId());
            leagueOfficialRepository.save(official);
        }
    }

    private void saveVenuePhoto(LeagueVenue venue, String photoDataUrl, Long actorUserId) {
        MediaAsset photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_LEAGUE_VENUE,
            venue.getId(),
            MediaAssetService.KIND_LEAGUE_VENUE_PHOTO,
            photoDataUrl,
            actorUserId
        );
        if (photo != null) {
            venue.setPhotoMediaId(photo.getId());
            leagueVenueRepository.save(venue);
        }
    }

    private LeagueOfficialData toOfficialData(LeagueOfficial official) {
        return new LeagueOfficialData(
            official.getId(),
            official.getFullName(),
            official.getPositionTitle(),
            official.getBio(),
            mediaAssetService.loadDataUrl(
                MediaAssetService.OWNER_LEAGUE_OFFICIAL,
                official.getId(),
                MediaAssetService.KIND_LEAGUE_OFFICIAL_PHOTO
            ),
            official.getSortOrder(),
            official.isActive(),
            official.getCreatedAt(),
            official.getUpdatedAt()
        );
    }

    private LeagueVenueData toVenueData(LeagueVenue venue) {
        return new LeagueVenueData(
            venue.getId(),
            venue.getName(),
            venue.getShortLabel(),
            venue.getAddress(),
            venue.getDescription(),
            mediaAssetService.loadDataUrl(
                MediaAssetService.OWNER_LEAGUE_VENUE,
                venue.getId(),
                MediaAssetService.KIND_LEAGUE_VENUE_PHOTO
            ),
            venue.getSortOrder(),
            venue.isActive(),
            venue.getCreatedAt(),
            venue.getUpdatedAt()
        );
    }

    public SeasonDocumentData toSeasonDocumentData(Season season) {
        return new SeasonDocumentData(
            season.getId(),
            season.getName(),
            season.getStatus().name(),
            season.isActive(),
            season.getRegulationMediaId() != null,
            season.getRegulationUpdatedAt(),
            season.getRegulationMediaId() == null ? null : "/api/seasons/" + season.getId() + "/regulation/pdf"
        );
    }

    private String buildRegulationFileName(String seasonName) {
        String normalized = String.valueOf(seasonName == null ? "season" : seasonName)
            .trim()
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "season";
        }
        return "regulation_" + normalized + ".pdf";
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 100 : sortOrder;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record LeagueOverviewData(
        List<LeagueOfficialData> officials,
        List<LeagueVenueData> venues,
        List<SeasonDocumentData> seasonDocuments
    ) {}

    public record LeagueOfficialData(
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

    public record LeagueVenueData(
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

    public record SeasonDocumentData(
        Long seasonId,
        String seasonName,
        String seasonStatus,
        boolean seasonActive,
        boolean regulationAvailable,
        OffsetDateTime regulationUpdatedAt,
        String regulationDownloadUrl
    ) {}

    public record LeagueOfficialUpsertData(String fullName, String positionTitle, String bio, String photoDataUrl, Integer sortOrder) {}

    public record LeagueVenueUpsertData(String name, String shortLabel, String address, String description, String photoDataUrl, Integer sortOrder) {}

    public record RegulationPdfPayload(String fileName, String mimeType, byte[] bytes) {}
}