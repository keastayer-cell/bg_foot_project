package com.footballstats.backend.service;

import com.footballstats.backend.domain.Referee;
import com.footballstats.backend.repository.RefereeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class RefereeManagementService {

    private final RefereeRepository refereeRepository;
    private final MediaAssetService mediaAssetService;

    public RefereeManagementService(RefereeRepository refereeRepository, MediaAssetService mediaAssetService) {
        this.refereeRepository = refereeRepository;
        this.mediaAssetService = mediaAssetService;
    }

    @Transactional(readOnly = true)
    public List<RefereeData> list(int activeFlag) {
        List<Referee> referees = activeFlag == 0
            ? refereeRepository.findAllByOrderByFullNameAsc()
            : refereeRepository.findAllByActiveTrueOrderByFullNameAsc();
        Map<Long, String> photos = mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_REFEREE,
            referees.stream().map(Referee::getId).toList(),
            MediaAssetService.KIND_REFEREE_PHOTO
        );
        return referees.stream().map(referee -> toData(referee, photos.get(referee.getId()))).toList();
    }

    @Transactional
    public RefereeData create(RefereeUpsert command, Long actorUserId) {
        String fullName = normalizeRequired(command.fullName());
        if (refereeRepository.existsByFullNameIgnoreCase(fullName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Судья с таким ФИО уже существует.");
        }
        Referee referee = new Referee();
        referee.setCreatedByUserId(actorUserId);
        apply(referee, command, actorUserId);
        return saveWithPhoto(referee, command.photoDataUrl(), actorUserId);
    }

    @Transactional
    public RefereeData update(Long refereeId, RefereeUpsert command, Long actorUserId) {
        Referee referee = requireReferee(refereeId);
        String fullName = normalizeRequired(command.fullName());
        refereeRepository.findByFullNameIgnoreCase(fullName).ifPresent(existing -> {
            if (!existing.getId().equals(refereeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Судья с таким ФИО уже существует.");
            }
        });
        apply(referee, command, actorUserId);
        return saveWithPhoto(referee, command.photoDataUrl(), actorUserId);
    }

    @Transactional
    public void deactivate(Long refereeId, Long actorUserId) {
        Referee referee = requireReferee(refereeId);
        referee.setActive(false);
        referee.setUpdatedByUserId(actorUserId);
        referee.setUpdatedAt(OffsetDateTime.now());
        refereeRepository.save(referee);
    }

    private RefereeData saveWithPhoto(Referee referee, String photoDataUrl, Long actorUserId) {
        Referee saved = refereeRepository.save(referee);
        var photo = mediaAssetService.saveAsset(
            MediaAssetService.OWNER_REFEREE, saved.getId(), MediaAssetService.KIND_REFEREE_PHOTO, photoDataUrl, actorUserId
        );
        if (photo != null) {
            saved.setPhotoMediaId(photo.getId());
            saved = refereeRepository.save(saved);
        }
        String resolvedPhoto = photo == null
            ? mediaAssetService.loadDataUrl(MediaAssetService.OWNER_REFEREE, saved.getId(), MediaAssetService.KIND_REFEREE_PHOTO)
            : photo.getDataUrl();
        return toData(saved, resolvedPhoto);
    }

    private void apply(Referee referee, RefereeUpsert command, Long actorUserId) {
        referee.setFullName(normalizeRequired(command.fullName()));
        referee.setCity(normalizeOptional(command.city()));
        referee.setBirthDate(command.birthDate());
        referee.setUpdatedByUserId(actorUserId);
        referee.setUpdatedAt(OffsetDateTime.now());
    }

    private Referee requireReferee(Long refereeId) {
        return refereeRepository.findById(refereeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Судья не найден."));
    }

    private RefereeData toData(Referee referee, String photoDataUrl) {
        return new RefereeData(
            referee.getId(), referee.getFullName(), referee.getCity(), referee.getBirthDate(), photoDataUrl, referee.isActive()
        );
    }

    private String normalizeRequired(String value) {
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

    public record RefereeUpsert(String fullName, String city, LocalDate birthDate, String photoDataUrl) {}
    public record RefereeData(Long id, String fullName, String city, LocalDate birthDate, String photoDataUrl, boolean active) {}
}
