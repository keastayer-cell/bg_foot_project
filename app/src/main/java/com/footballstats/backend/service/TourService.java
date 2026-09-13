package com.footballstats.backend.service;

import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.domain.MatchScheduleStatus;
import com.footballstats.backend.domain.LeagueVenue;
import com.footballstats.backend.repository.LeagueVenueRepository;
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
    private final SiteNotificationService siteNotificationService;
    private final LeagueVenueRepository leagueVenueRepository;

    public TourService(
        TourRepository tourRepository,
        TourMatchRepository tourMatchRepository,
        SeasonRepository seasonRepository,
        TeamRepository teamRepository,
        SeasonTeamRepository seasonTeamRepository,
        MatchProtocolRepository matchProtocolRepository,
        SeasonStandingsService seasonStandingsService,
        SiteNotificationService siteNotificationService,
        LeagueVenueRepository leagueVenueRepository
    ) {
        this.tourRepository = tourRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.matchProtocolRepository = matchProtocolRepository;
        this.seasonStandingsService = seasonStandingsService;
        this.siteNotificationService = siteNotificationService;
        this.leagueVenueRepository = leagueVenueRepository;
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

    @Transactional(readOnly = true)
    public List<TourMatch> listSeasonMatches(Long seasonId, boolean includeInactive) {
        getExistingSeason(seasonId);
        return includeInactive
            ? tourMatchRepository.findAllDetailedByAnyCompetitionSeasonId(seasonId)
            : tourMatchRepository.findAllActiveDetailedByAnyCompetitionSeasonId(seasonId);
    }

    @Transactional
    @com.footballstats.backend.audit.AuditedAction(entity="TOUR",idParam="tourId",action="TOUR_PUBLISHED",actorParam="actorUserId")
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
        siteNotificationService.notifyTourPublished(tour, actorUserId);
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

        validateHeadToHeadLimit(tour, homeTeam, awayTeam);

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
    @com.footballstats.backend.audit.AuditedAction(entity="MATCH",idParam="matchId",action="DELETED",actorParam="actorUserId")
    public void deleteMatch(Long tourId, Long matchId, Long actorUserId) {
        Tour tour = getExistingTour(tourId);
        TourMatch match = tourMatchRepository.findByIdAndTour_Id(matchId, tourId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Матч тура не найден."));
        Long seasonId = tour.getSeason().getId();

        MatchProtocol protocol = matchProtocolRepository.findByMatch_Id(matchId).orElse(null);
        MatchProtocolStatus protocolStatus = protocol == null || protocol.getStatus() == null
            ? MatchProtocolStatus.SCHEDULED
            : protocol.getStatus();
        if (protocolStatus != MatchProtocolStatus.SCHEDULED) {
            throw new IllegalArgumentException(
                "Нельзя удалить матч, если по нему уже поданы составы или заполнен протокол. Сначала верните матч в исходное состояние."
            );
        }

        tourMatchRepository.delete(match);

        if (tour.isPublished() && tourMatchRepository.countByTour_IdAndActiveTrue(tourId) == 0) {
            tour.setPublished(false);
            tour.setUpdatedByUserId(actorUserId);
            tour.setUpdatedAt(OffsetDateTime.now());
            tourRepository.save(tour);
        }

        seasonStandingsService.recalculateSeasonStandings(seasonId, actorUserId);
    }

    @Transactional
    @com.footballstats.backend.audit.AuditedAction(entity="MATCH",idParam="matchId",action="SCHEDULE_UPDATED",actorParam="actorUserId")
    public TourMatch updateMatchSchedule(Long tourId, Long matchId, MatchScheduleStatus status,
        OffsetDateTime kickoffAt, Long venueId, String rawReason, Long actorUserId) {
        if (status == null) {
            throw new IllegalArgumentException("Укажите статус расписания.");
        }
        if (status == MatchScheduleStatus.COMPLETED || status == MatchScheduleStatus.TECHNICAL_RESULT) {
            throw new IllegalArgumentException("Итоговый статус назначается подтверждением протокола.");
        }
        if (kickoffAt == null) {
            throw new IllegalArgumentException("Укажите дату и время матча.");
        }
        TourMatch match = tourMatchRepository.findDetailedById(matchId)
            .filter(item -> item.getTour().getId().equals(tourId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Матч тура не найден."));
        if (match.getProtocol() != null && match.getProtocol().getStatus() == MatchProtocolStatus.VERIFIED) {
            throw new IllegalArgumentException("Расписание подтверждённого матча менять нельзя. Сначала откройте протокол.");
        }
        OffsetDateTime previousKickoff = match.getKickoffAt();
        MatchScheduleStatus previousStatus = match.getScheduleStatus();
        Long previousVenueId = match.getVenue() == null ? null : match.getVenue().getId();
        boolean kickoffChanged = !previousKickoff.equals(kickoffAt);
        if (kickoffChanged && status == MatchScheduleStatus.SCHEDULED) {
            throw new IllegalArgumentException("При изменении даты или времени выберите статус «Перенесён».");
        }
        String reason = rawReason == null ? "" : rawReason.trim();
        if ((status == MatchScheduleStatus.RESCHEDULED || status == MatchScheduleStatus.CANCELLED) && reason.length() < 5) {
            throw new IllegalArgumentException("Для переноса или отмены укажите причину.");
        }
        if (status == MatchScheduleStatus.RESCHEDULED && match.getOriginalKickoffAt() == null) {
            match.setOriginalKickoffAt(previousKickoff);
        }
        if (status == MatchScheduleStatus.SCHEDULED) match.setOriginalKickoffAt(null);
        LeagueVenue venue = venueId == null ? null : leagueVenueRepository.findById(venueId)
            .orElseThrow(() -> new IllegalArgumentException("Площадка не найдена."));
        match.setKickoffAt(kickoffAt);
        match.setVenue(venue);
        match.setScheduleStatus(status);
        match.setScheduleChangeReason(reason.isBlank() ? null : reason);
        match.setUpdatedByUserId(actorUserId);
        match.setUpdatedAt(OffsetDateTime.now());
        TourMatch saved = tourMatchRepository.save(match);
        boolean venueChanged = !java.util.Objects.equals(previousVenueId, venueId);
        if (saved.getTour().isPublished()
            && (previousStatus != status || kickoffChanged || venueChanged)) {
            siteNotificationService.notifyMatchScheduleChanged(saved, previousKickoff, actorUserId);
        }
        return saved;
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

    private void validateHeadToHeadLimit(Tour tour, Team homeTeam, Team awayTeam) {
        if (!SeasonStructureService.REGULAR_STAGE.equalsIgnoreCase(tour.getStageType())) {
            return;
        }

        Season season = tour.getSeason();
        int allowedMeetings = Math.max(season.getRoundsCount(), 1);
        long existingMeetings = tourMatchRepository.countActiveHeadToHeadMatchesInSeasonStage(
            season.getId(),
            SeasonStructureService.REGULAR_STAGE,
            homeTeam.getId(),
            awayTeam.getId()
        );

        if (existingMeetings >= allowedMeetings) {
            throw new IllegalArgumentException(
                "Нельзя добавить матч: команды "
                    + homeTeam.getName()
                    + " и "
                    + awayTeam.getName()
                    + " уже сыграют между собой максимальное число раз для сезона ("
                    + allowedMeetings
                    + " круг(а))."
            );
        }
    }

    public record SeasonOverviewData(
        Season season,
        List<Team> teams,
        List<Tour> tours,
        Map<Long, List<TourMatch>> matchesByTourId
    ) {}

}
