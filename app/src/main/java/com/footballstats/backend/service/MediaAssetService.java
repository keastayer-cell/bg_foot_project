package com.footballstats.backend.service;

import com.footballstats.backend.domain.MediaAsset;
import com.footballstats.backend.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class MediaAssetService {

    public static final String OWNER_TEAM = "TEAM";
    public static final String OWNER_PLAYER = "PLAYER";
    public static final String OWNER_REFEREE = "REFEREE";
    public static final String KIND_TEAM_LOGO = "TEAM_LOGO";
    public static final String KIND_PLAYER_PHOTO = "PLAYER_PHOTO";
    public static final String KIND_REFEREE_PHOTO = "REFEREE_PHOTO";

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
}