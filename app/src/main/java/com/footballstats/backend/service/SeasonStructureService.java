package com.footballstats.backend.service;

import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import com.footballstats.backend.repository.TourRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SeasonStructureService {

    public static final String REGULAR_STAGE = "REGULAR";

    private final TourRepository tourRepository;
    private final TourMatchRepository tourMatchRepository;
    private final SeasonTeamRepository seasonTeamRepository;

    public SeasonStructureService(
        TourRepository tourRepository,
        TourMatchRepository tourMatchRepository,
        SeasonTeamRepository seasonTeamRepository
    ) {
        this.tourRepository = tourRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.seasonTeamRepository = seasonTeamRepository;
    }

    @Transactional(readOnly = true)
    public int calculateRegularToursCount(long teamCount, int roundsCount) {
        if (teamCount < 2 || roundsCount < 1) {
            return 0;
        }
        long singleRoundTours = teamCount % 2 == 0 ? teamCount - 1 : teamCount;
        return Math.toIntExact(singleRoundTours * roundsCount);
    }

    @Transactional(readOnly = true)
    public boolean hasRegularStageMatches(Long seasonId) {
        return tourMatchRepository.existsByTour_Season_IdAndTour_StageTypeAndActiveTrue(seasonId, REGULAR_STAGE);
    }

    @Transactional(readOnly = true)
    public int calculateRegularToursCountForSeason(Long seasonId, int roundsCount) {
        return calculateRegularToursCount(seasonTeamRepository.countBySeason_Id(seasonId), roundsCount);
    }

    @Transactional
    public void syncRegularToursForSeason(Season season, Long actorUserId) {
        int desiredCount = calculateRegularToursCountForSeason(season.getId(), season.getRoundsCount());
        List<Tour> existingRegularTours = tourRepository.findAllBySeason_IdAndStageTypeOrderBySortOrderAscIdAsc(
            season.getId(),
            REGULAR_STAGE
        );
        boolean hasRegularMatches = hasRegularStageMatches(season.getId());

        if (hasRegularMatches) {
            validateExistingRegularStructure(existingRegularTours, desiredCount);
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (int index = 0; index < desiredCount; index += 1) {
            int roundNumber = index + 1;
            Tour tour = index < existingRegularTours.size() ? existingRegularTours.get(index) : new Tour();
            if (tour.getSeason() == null) {
                tour.setSeason(season);
                tour.setCreatedByUserId(actorUserId);
            }
            tour.setName(roundNumber + " тур");
            tour.setStageType(REGULAR_STAGE);
            tour.setRoundNumber(roundNumber);
            tour.setSortOrder(roundNumber);
            tour.setActive(true);
            tour.setUpdatedByUserId(actorUserId);
            tour.setUpdatedAt(now);
            tourRepository.save(tour);
        }

        if (hasRegularMatches) {
            return;
        }

        for (int index = desiredCount; index < existingRegularTours.size(); index += 1) {
            Tour tour = existingRegularTours.get(index);
            if (!tour.isActive()) {
                continue;
            }
            tour.setActive(false);
            tour.setUpdatedByUserId(actorUserId);
            tour.setUpdatedAt(now);
            tourRepository.save(tour);
        }
    }

    private void validateExistingRegularStructure(List<Tour> existingRegularTours, int desiredCount) {
        long activeCount = existingRegularTours.stream().filter(Tour::isActive).count();
        if (activeCount != desiredCount) {
            throw new IllegalArgumentException(
                "Нельзя менять структуру регулярного этапа после добавления матчей. Сначала очистите матчи регулярного этапа."
            );
        }
    }
}