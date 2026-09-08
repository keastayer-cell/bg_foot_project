package com.footballstats.backend.service;

import com.footballstats.backend.domain.Competition;
import com.footballstats.backend.domain.CompetitionType;
import com.footballstats.backend.domain.MediaAsset;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.repository.CompetitionRepository;
import com.footballstats.backend.repository.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegulationServiceTest {

    @Mock private CompetitionRepository competitionRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private MediaAssetService mediaAssetService;

    private RegulationService service;
    private Season season;
    private Competition championship;
    private Competition cup;

    @BeforeEach
    void setUp() {
        service = new RegulationService(competitionRepository, seasonRepository, mediaAssetService);
        season = new Season();
        ReflectionTestUtils.setField(season, "id", 10L);
        season.setName("Сезон 2026");
        season.setStatus(SeasonStatus.ACTIVE);

        championship = competition(101L, "Чемпионат", CompetitionType.CHAMPIONSHIP);
        cup = competition(102L, "Кубок города", CompetitionType.CUP);
    }

    @Test
    void listsSeasonAndEveryCompetitionAsSeparateTargets() {
        when(seasonRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(season));
        when(competitionRepository.findAllWithSeason()).thenReturn(List.of(championship, cup));

        List<RegulationService.Document> documents = service.list(true);

        assertThat(documents).extracting(RegulationService.Document::targetType)
            .containsExactly("SEASON", "COMPETITION", "COMPETITION");
        assertThat(documents).extracting(RegulationService.Document::competitionName)
            .containsExactly(null, "Чемпионат", "Кубок города");
    }

    @Test
    void savingCupDocumentDoesNotChangeChampionship() {
        String dataUrl = "data:application/pdf;base64,JVBERi0xLjQ=";
        MediaAsset asset = new MediaAsset();
        ReflectionTestUtils.setField(asset, "id", 55L);
        when(competitionRepository.findDetailedById(102L)).thenReturn(Optional.of(cup));
        when(mediaAssetService.decodeDataUrl(dataUrl)).thenReturn(
            new MediaAssetService.DataUrlPayload("application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8))
        );
        when(mediaAssetService.saveAsset("COMPETITION", 102L, "COMPETITION_REGULATION_PDF", dataUrl, 7L))
            .thenReturn(asset);

        RegulationService.Document saved = service.save(102L, dataUrl, 7L);

        assertThat(saved.targetId()).isEqualTo(102L);
        assertThat(saved.available()).isTrue();
        assertThat(cup.getRegulationMediaId()).isEqualTo(55L);
        assertThat(championship.getRegulationMediaId()).isNull();
        verify(competitionRepository).save(cup);
    }

    private Competition competition(Long id, String name, CompetitionType type) {
        Competition competition = new Competition();
        ReflectionTestUtils.setField(competition, "id", id);
        competition.setSeason(season);
        competition.setName(name);
        competition.setType(type);
        return competition;
    }
}
