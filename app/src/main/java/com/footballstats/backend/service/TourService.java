package com.footballstats.backend.service;

import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.MatchProtocolRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import com.footballstats.backend.repository.TourRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TourService {

    private final TourRepository tourRepository;
    private final TourMatchRepository tourMatchRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final MatchProtocolRepository matchProtocolRepository;
    private final SeasonStandingsService seasonStandingsService;

    public TourService(
        TourRepository tourRepository,
        TourMatchRepository tourMatchRepository,
        SeasonRepository seasonRepository,
        TeamRepository teamRepository,
        SeasonTeamRepository seasonTeamRepository,
        MatchProtocolRepository matchProtocolRepository,
        SeasonStandingsService seasonStandingsService
    ) {
        this.tourRepository = tourRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.matchProtocolRepository = matchProtocolRepository;
        this.seasonStandingsService = seasonStandingsService;
    }

    @Transactional(readOnly = true)
    public List<Tour> listTours(Long seasonId, boolean includeInactive, Boolean publishedOnly) {
        getExistingSeason(seasonId);
        if (Boolean.TRUE.equals(publishedOnly) && !includeInactive) {
            return tourRepository.findAllPublishedDetailedBySeasonId(seasonId);
        }
        return includeInactive
            ? tourRepository.findAllDetailedBySeasonId(seasonId)
            : tourRepository.findAllActiveDetailedBySeasonId(seasonId);
    }

    @Transactional
    public Tour createTour(Long seasonId, String rawName, Long actorUserId) {
        throw new IllegalArgumentException(
            "Ручное создание туров отключено. Настройте сезон, состав команд и количество кругов."
        );
    }

    @Transactional(readOnly = true)
    public List<TourMatch> listMatches(Long tourId, boolean includeInactive) {
        getExistingTour(tourId);
        return includeInactive
            ? tourMatchRepository.findAllDetailedByTourId(tourId)
            : tourMatchRepository.findAllActiveDetailedByTourId(tourId);
    }

    @Transactional
    public Tour publishTour(Long tourId, Long actorUserId) {
        Tour tour = getExistingTour(tourId);
        if (tour.isPublished()) {
            return getExistingDetailedTour(tourId);
        }
        if (!tourMatchRepository.existsByTour_IdAndActiveTrue(tourId)) {
            throw new IllegalArgumentException("Нельзя публиковать пустой тур без матчей.");
        }
        tour.setPublished(true);
        tour.setUpdatedByUserId(actorUserId);
        tour.setUpdatedAt(OffsetDateTime.now());
        tourRepository.save(tour);
        seasonStandingsService.recalculateSeasonStandings(tour.getSeason().getId(), actorUserId);
        return getExistingDetailedTour(tourId);
    }

    @Transactional(readOnly = true)
    public SeasonOverviewData getPublishedSeasonOverview(Long seasonId) {
        Season season = getExistingSeason(seasonId);
        List<Team> teams = seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(seasonId).stream()
            .map(seasonTeam -> seasonTeam.getTeam())
            .filter(Team::isActive)
            .toList();

        List<Tour> tours = tourRepository.findAllPublishedDetailedBySeasonId(seasonId);
        List<TourMatch> matches = tourMatchRepository.findAllActiveDetailedByPublishedSeasonId(seasonId);

        Map<Long, List<TourMatch>> matchesByTourId = new LinkedHashMap<>();
        for (Tour tour : tours) {
            matchesByTourId.put(tour.getId(), new java.util.ArrayList<>());
        }
        for (TourMatch match : matches) {
            matchesByTourId.computeIfAbsent(match.getTour().getId(), ignored -> new java.util.ArrayList<>()).add(match);
        }

        return new SeasonOverviewData(season, teams, tours, matchesByTourId);
    }

    @Transactional
    public TourMatch createMatch(Long tourId, Long homeTeamId, Long awayTeamId, OffsetDateTime kickoffAt, Long actorUserId) {
        Tour tour = getExistingTour(tourId);
        if (homeTeamId == null || awayTeamId == null) {
            throw new IllegalArgumentException("Нужно выбрать обе команды.");
        }
        if (homeTeamId.equals(awayTeamId)) {
            throw new IllegalArgumentException("Команды матча должны быть разными.");
        }
        if (kickoffAt == null) {
            throw new IllegalArgumentException("Время матча обязательно.");
        }

        Long seasonId = tour.getSeason().getId();
        if (!seasonTeamRepository.existsBySeason_IdAndTeam_Id(seasonId, homeTeamId)) {
            throw new IllegalArgumentException("Домашняя команда не относится к выбранному сезону.");
        }
        if (!seasonTeamRepository.existsBySeason_IdAndTeam_Id(seasonId, awayTeamId)) {
            throw new IllegalArgumentException("Гостевая команда не относится к выбранному сезону.");
        }

        Team homeTeam = getExistingTeam(homeTeamId);
        Team awayTeam = getExistingTeam(awayTeamId);

        TourMatch match = new TourMatch();
        match.setTour(tour);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setKickoffAt(kickoffAt);
        match.setCreatedByUserId(actorUserId);
        match.setUpdatedByUserId(actorUserId);
        match.setUpdatedAt(OffsetDateTime.now());
        match.setActive(true);
        TourMatch savedMatch = tourMatchRepository.save(match);
        if (matchProtocolRepository.findByMatch_Id(savedMatch.getId()).isEmpty()) {
            com.footballstats.backend.domain.MatchProtocol protocol = new com.footballstats.backend.domain.MatchProtocol();
            protocol.setMatch(savedMatch);
            protocol.setCreatedByUserId(actorUserId);
            protocol.setUpdatedByUserId(actorUserId);
            protocol.setCreatedAt(OffsetDateTime.now());
            protocol.setUpdatedAt(OffsetDateTime.now());
            matchProtocolRepository.save(protocol);
        }
        return savedMatch;
    }

    @Transactional
    public void deleteMatch(Long tourId, Long matchId, Long actorUserId) {
        Tour tour = getExistingTour(tourId);
        TourMatch match = tourMatchRepository.findByIdAndTour_Id(matchId, tourId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Матч тура не найден."));
        Long seasonId = tour.getSeason().getId();

        tourMatchRepository.delete(match);

        if (tour.isPublished() && tourMatchRepository.countByTour_IdAndActiveTrue(tourId) == 0) {
            tour.setPublished(false);
            tour.setUpdatedByUserId(actorUserId);
            tour.setUpdatedAt(OffsetDateTime.now());
            tourRepository.save(tour);
        }

        seasonStandingsService.recalculateSeasonStandings(seasonId, actorUserId);
    }

    private Season getExistingSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
    }

    private Tour getExistingTour(Long tourId) {
        return tourRepository.findById(tourId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тур не найден."));
    }

    private Tour getExistingDetailedTour(Long tourId) {
        return tourRepository.findDetailedById(tourId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тур не найден."));
    }

    private Team getExistingTeam(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
    }

    public record SeasonOverviewData(
        Season season,
        List<Team> teams,
        List<Tour> tours,
        Map<Long, List<TourMatch>> matchesByTourId
    ) {}

}