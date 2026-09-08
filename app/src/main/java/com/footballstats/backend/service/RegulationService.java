package com.footballstats.backend.service;

import com.footballstats.backend.domain.Competition;
import com.footballstats.backend.domain.MediaAsset;
import com.footballstats.backend.repository.CompetitionRepository;
import com.footballstats.backend.repository.SeasonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegulationService {
    private static final String OWNER = "COMPETITION";
    private static final String KIND = "COMPETITION_REGULATION_PDF";
    private final CompetitionRepository competitions;
    private final SeasonRepository seasons;
    private final MediaAssetService media;

    public RegulationService(CompetitionRepository competitions, SeasonRepository seasons, MediaAssetService media) {
        this.competitions = competitions;
        this.seasons = seasons;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public List<Document> list(boolean includeMissing) {
        List<Document> documents = new ArrayList<>();
        seasons.findAllByOrderByCreatedAtDescIdDesc().forEach(season -> {
            if (includeMissing || season.getRegulationMediaId() != null) {
                documents.add(new Document("SEASON", season.getId(), season.getId(), season.getName(),
                    season.getStatus().name(), null, null, season.getRegulationMediaId() != null,
                    season.getRegulationUpdatedAt(), season.getRegulationMediaId() == null ? null
                    : "/api/seasons/" + season.getId() + "/regulation/pdf"));
            }
        });
        competitions.findAllWithSeason().forEach(competition -> {
            if (includeMissing || competition.getRegulationMediaId() != null) documents.add(toDocument(competition));
        });
        return documents;
    }

    @Transactional
    public Document save(Long competitionId, String dataUrl, Long actorId) {
        Competition competition = get(competitionId);
        MediaAssetService.DataUrlPayload payload;
        try {
            payload = media.decodeDataUrl(dataUrl);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный PDF-файл.");
        }
        if (!"application/pdf".equalsIgnoreCase(payload.mimeType()) || payload.bytes().length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно загрузить PDF-файл регламента.");
        }
        MediaAsset asset = media.saveAsset(OWNER, competitionId, KIND, dataUrl, actorId);
        if (asset == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось сохранить PDF.");
        competition.setRegulationMediaId(asset.getId());
        competition.setRegulationUpdatedAt(OffsetDateTime.now());
        touch(competition, actorId);
        return toDocument(competition);
    }

    @Transactional
    public Document remove(Long competitionId, Long actorId) {
        Competition competition = get(competitionId);
        competition.setRegulationMediaId(null);
        competition.setRegulationUpdatedAt(null);
        touch(competition, actorId);
        return toDocument(competition);
    }

    @Transactional(readOnly = true)
    public LeagueService.RegulationPdfPayload download(Long competitionId) {
        Competition competition = get(competitionId);
        if (competition.getRegulationMediaId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Регламент соревнования не опубликован.");
        }
        MediaAsset asset = media.loadLatestAsset(OWNER, competitionId, KIND)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF не найден."));
        var payload = media.decodeDataUrl(asset.getDataUrl());
        String name = (competition.getSeason().getName() + "_" + competition.getName())
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "_");
        return new LeagueService.RegulationPdfPayload("regulation_" + name + ".pdf", payload.mimeType(), payload.bytes());
    }

    private Competition get(Long id) {
        return competitions.findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено."));
    }

    private void touch(Competition competition, Long actorId) {
        competition.setUpdatedByUserId(actorId);
        competition.setUpdatedAt(OffsetDateTime.now());
        competitions.save(competition);
    }

    private Document toDocument(Competition c) {
        return new Document("COMPETITION", c.getId(), c.getSeason().getId(), c.getSeason().getName(),
            c.getSeason().getStatus().name(), c.getName(), c.getType().name(), c.getRegulationMediaId() != null,
            c.getRegulationUpdatedAt(), c.getRegulationMediaId() == null ? null
            : "/api/competitions/" + c.getId() + "/regulation/pdf");
    }

    public record Document(String targetType, Long targetId, Long seasonId, String seasonName,
        String seasonStatus, String competitionName, String competitionType, boolean available,
        OffsetDateTime updatedAt, String downloadUrl) {}
}
