package com.footballstats.backend.service;

import com.footballstats.backend.domain.MediaAsset;
import com.footballstats.backend.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MediaAssetService {

    public static final String OWNER_TEAM = "TEAM";
    public static final String OWNER_PLAYER = "PLAYER";
    public static final String OWNER_REFEREE = "REFEREE";
    public static final String OWNER_SEASON = "SEASON";
    public static final String OWNER_LEAGUE_OFFICIAL = "LEAGUE_OFFICIAL";
    public static final String OWNER_LEAGUE_VENUE = "LEAGUE_VENUE";
    public static final String KIND_TEAM_LOGO = "TEAM_LOGO";
    public static final String KIND_PLAYER_PHOTO = "PLAYER_PHOTO";
    public static final String KIND_REFEREE_PHOTO = "REFEREE_PHOTO";
    public static final String KIND_SEASON_REGULATION_PDF = "SEASON_REGULATION_PDF";
    public static final String KIND_LEAGUE_OFFICIAL_PHOTO = "LEAGUE_OFFICIAL_PHOTO";
    public static final String KIND_LEAGUE_VENUE_PHOTO = "LEAGUE_VENUE_PHOTO";

    private final MediaAssetRepository mediaAssetRepository;

    public MediaAssetService(MediaAssetRepository mediaAssetRepository) {
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Transactional
    public MediaAsset saveAsset(String ownerType, Long ownerId, String mediaKind, String dataUrl, Long actorUserId) {
        String normalized = normalizeDataUrl(dataUrl);
        if (normalized == null) {
            return null;
        }

        MediaAsset asset = new MediaAsset();
        asset.setOwnerType(normalizeToken(ownerType));
        asset.setOwnerId(ownerId);
        asset.setMediaKind(normalizeToken(mediaKind));
        asset.setDataUrl(normalized);
        asset.setMimeType(extractMimeType(normalized));
        asset.setCreatedByUserId(actorUserId);
        asset.setActive(true);
        return mediaAssetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public String loadDataUrl(String ownerType, Long ownerId, String mediaKind) {
        if (ownerId == null) {
            return null;
        }
        return mediaAssetRepository
            .findFirstByOwnerTypeAndOwnerIdAndMediaKindAndActiveTrueOrderByIdDesc(
                normalizeToken(ownerType),
                ownerId,
                normalizeToken(mediaKind)
            )
            .map(MediaAsset::getDataUrl)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<MediaAsset> loadLatestAsset(String ownerType, Long ownerId, String mediaKind) {
        if (ownerId == null) {
            return Optional.empty();
        }
        return mediaAssetRepository.findFirstByOwnerTypeAndOwnerIdAndMediaKindAndActiveTrueOrderByIdDesc(
            normalizeToken(ownerType),
            ownerId,
            normalizeToken(mediaKind)
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, String> loadDataUrls(String ownerType, Collection<Long> ownerIds, String mediaKind) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedOwnerIds = ownerIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (normalizedOwnerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> latestByOwner = new LinkedHashMap<>();
        mediaAssetRepository.findActiveByOwners(
            normalizeToken(ownerType),
            normalizeToken(mediaKind),
            normalizedOwnerIds
        ).forEach(asset -> latestByOwner.putIfAbsent(asset.getOwnerId(), asset.getDataUrl()));
        return latestByOwner;
    }

    public DataUrlPayload decodeDataUrl(String dataUrl) {
        String normalized = normalizeDataUrl(dataUrl);
        if (normalized == null || !normalized.startsWith("data:")) {
            throw new IllegalArgumentException("Некорректный data URL.");
        }

        int commaIndex = normalized.indexOf(',');
        if (commaIndex < 0) {
            throw new IllegalArgumentException("Некорректный data URL.");
        }

        String meta = normalized.substring(5, commaIndex);
        String payload = normalized.substring(commaIndex + 1);
        String mimeType = extractMimeType(normalized);
        boolean base64 = meta.contains(";base64");
        byte[] bytes = base64
            ? Base64.getDecoder().decode(payload)
            : payload.getBytes(StandardCharsets.UTF_8);
        return new DataUrlPayload(mimeType, bytes);
    }

    private String normalizeToken(String value) {
        return String.valueOf(value == null ? "" : value).trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDataUrl(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String extractMimeType(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            return null;
        }
        int semicolon = dataUrl.indexOf(';');
        if (semicolon <= 5) {
            return null;
        }
        return dataUrl.substring(5, semicolon).trim();
    }

    public record DataUrlPayload(String mimeType, byte[] bytes) {}
}
