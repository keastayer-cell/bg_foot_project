package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonPlayoffBracket;
import com.footballstats.backend.domain.SeasonPlayoffConfig;
import com.footballstats.backend.domain.SeasonPlayoffTie;
import com.footballstats.backend.domain.SeasonStandingsRow;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.SeasonPlayoffBracketRepository;
import com.footballstats.backend.repository.SeasonPlayoffConfigRepository;
import com.footballstats.backend.repository.SeasonPlayoffTieRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonStandingsRowRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import com.footballstats.backend.repository.TourRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SeasonPlayoffService {

    public static final String PLAYOFF_STAGE = "PLAYOFF";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_PLANNED = "PLANNED";
    private static final String SOURCE_WINNER = "WINNER";
    private static final String SOURCE_LOSER = "LOSER";

    private final SeasonRepository seasonRepository;
    private final SeasonPlayoffConfigRepository seasonPlayoffConfigRepository;
    private final SeasonPlayoffBracketRepository seasonPlayoffBracketRepository;
    private final SeasonPlayoffTieRepository seasonPlayoffTieRepository;
    private final SeasonStandingsService seasonStandingsService;
    private final SeasonStandingsRowRepository seasonStandingsRowRepository;
    private final TourRepository tourRepository;
    private final TourMatchRepository tourMatchRepository;

    public SeasonPlayoffService(
        SeasonRepository seasonRepository,
        SeasonPlayoffConfigRepository seasonPlayoffConfigRepository,
        SeasonPlayoffBracketRepository seasonPlayoffBracketRepository,
        SeasonPlayoffTieRepository seasonPlayoffTieRepository,
        SeasonStandingsService seasonStandingsService,
        SeasonStandingsRowRepository seasonStandingsRowRepository,
        TourRepository tourRepository,
        TourMatchRepository tourMatchRepository
    ) {
        this.seasonRepository = seasonRepository;
        this.seasonPlayoffConfigRepository = seasonPlayoffConfigRepository;
        this.seasonPlayoffBracketRepository = seasonPlayoffBracketRepository;
        this.seasonPlayoffTieRepository = seasonPlayoffTieRepository;
        this.seasonStandingsService = seasonStandingsService;
        this.seasonStandingsRowRepository = seasonStandingsRowRepository;
        this.tourRepository = tourRepository;
        this.tourMatchRepository = tourMatchRepository;
    }

    @Transactional
    public SeasonPlayoffConfig syncSeasonPlayoffConfig(Long seasonId, boolean enabled, Integer teamCount, boolean thirdPlaceEnabled, Long actorUserId) {
        Season season = getExistingSeason(seasonId);
        SeasonPlayoffConfig config = seasonPlayoffConfigRepository.findBySeason_Id(seasonId)
            .orElseGet(SeasonPlayoffConfig::new);
        OffsetDateTime now = OffsetDateTime.now();

        if (config.getSeason() == null) {
            config.setSeason(season);
            config.setCreatedByUserId(actorUserId);
            config.setCreatedAt(now);
        }

        if (enabled && teamCount != null && teamCount < 4 && thirdPlaceEnabled) {
            throw new IllegalArgumentException("Матч за 3 место доступен только для плей-офф минимум на 4 команды.");
        }

        config.setEnabled(enabled);
        config.setTeamCount(enabled ? teamCount : null);
        config.setThirdPlaceEnabled(enabled && teamCount != null && teamCount >= 4 && thirdPlaceEnabled);
        config.setUpdatedByUserId(actorUserId);
        config.setUpdatedAt(now);
        return seasonPlayoffConfigRepository.save(config);
    }

    @Transactional(readOnly = true)
    public SeasonPlayoffConfigData getSeasonPlayoffConfig(Long seasonId) {
        Season season = getExistingSeason(seasonId);
        Optional<SeasonPlayoffConfig> existingConfig = seasonPlayoffConfigRepository.findBySeason_Id(seasonId);
        if (existingConfig.isEmpty()) {
            return new SeasonPlayoffConfigData(
                season.isPlayoffEnabled(),
                season.getPlayoffTeamCount(),
                false,
                1,
                1,
                1,
                1,
                1
            );
        }

        SeasonPlayoffConfig config = existingConfig.get();
        return new SeasonPlayoffConfigData(
            config.isEnabled(),
            config.getTeamCount(),
            config.isThirdPlaceEnabled(),
            config.getRoundOf16Legs(),
            config.getQuarterfinalLegs(),
            config.getSemifinalLegs(),
            config.getFinalLegs(),
            config.getThirdPlaceLegs()
        );
    }

    @Transactional(readOnly = true)
    public SeasonPlayoffBracketData getSeasonPlayoffBracket(Long seasonId) {
        SeasonPlayoffConfigData config = getSeasonPlayoffConfig(seasonId);
        SeasonPlayoffBracket bracket = seasonPlayoffBracketRepository.findBySeason_Id(seasonId).orElse(null);
        List<SeasonPlayoffTie> ties = seasonPlayoffTieRepository.findAllDetailedBySeasonId(seasonId);
        return new SeasonPlayoffBracketData(config, bracket, ties);
    }

    @Transactional
    public Season completeRegularSeason(Long seasonId, Long actorUserId) {
        Season season = getExistingSeason(seasonId);
        List<Tour> regularTours = tourRepository.findAllBySeason_IdAndStageTypeOrderBySortOrderAscIdAsc(seasonId, SeasonStructureService.REGULAR_STAGE).stream()
            .filter(Tour::isActive)
            .toList();
        if (regularTours.isEmpty()) {
            throw new IllegalArgumentException("В сезоне нет активных туров регулярного этапа.");
        }

        List<TourMatch> regularMatches = tourMatchRepository.findAllActiveDetailedBySeasonId(seasonId).stream()
            .filter(match -> SeasonStructureService.REGULAR_STAGE.equalsIgnoreCase(match.getTour().getStageType()))
            .toList();

        if (regularMatches.isEmpty()) {
            throw new IllegalArgumentException("Нельзя завершить регулярный этап без сыгранных матчей.");
        }

        Map<Long, Integer> matchesPerTour = new LinkedHashMap<>();
        for (Tour tour : regularTours) {
            matchesPerTour.put(tour.getId(), 0);
        }
        for (TourMatch match : regularMatches) {
            matchesPerTour.computeIfPresent(match.getTour().getId(), (ignored, count) -> count + 1);
            MatchProtocol protocol = match.getProtocol();
            if (protocol == null || protocol.getStatus() != MatchProtocolStatus.VERIFIED) {
                throw new IllegalArgumentException(
                    "Нельзя завершить регулярный этап, пока все матчи регулярки не подтверждены. Проверьте матч: "
                        + match.getHomeTeam().getName() + " - " + match.getAwayTeam().getName() + "."
                );
            }
        }
        for (Tour tour : regularTours) {
            if (matchesPerTour.getOrDefault(tour.getId(), 0) <= 0) {
                tour.setActive(false);
                tour.setPublished(false);
                tour.setUpdatedByUserId(actorUserId);
                tour.setUpdatedAt(OffsetDateTime.now());
                tourRepository.save(tour);
                continue;
            }
            if (!tour.isPublished()) {
                tour.setPublished(true);
                tour.setUpdatedByUserId(actorUserId);
                tour.setUpdatedAt(OffsetDateTime.now());
                tourRepository.save(tour);
            }
        }

        seasonStandingsService.recalculateSeasonStandings(seasonId, actorUserId);

        SeasonPlayoffConfig config = seasonPlayoffConfigRepository.findBySeason_Id(seasonId)
            .orElseGet(() -> syncSeasonPlayoffConfig(seasonId, season.isPlayoffEnabled(), season.getPlayoffTeamCount(), false, actorUserId));

        if (!config.isEnabled() || config.getTeamCount() == null) {
            // Завершение чемпионата не закрывает сезон-контейнер: параллельные Кубки могут продолжаться.
            season.setStatus(SeasonStatus.ACTIVE);
            season.setUpdatedByUserId(actorUserId);
            season.setUpdatedAt(OffsetDateTime.now());
            return seasonRepository.save(season);
        }

        if (!tourRepository.findAllBySeason_IdAndStageTypeOrderBySortOrderAscIdAsc(seasonId, PLAYOFF_STAGE).isEmpty()) {
            throw new IllegalArgumentException("Плей-офф уже сформирован для этого сезона.");
        }
        SeasonPlayoffBracket existingBracket = seasonPlayoffBracketRepository.findBySeason_Id(seasonId).orElse(null);
        if (existingBracket != null && existingBracket.isRegularSeasonCompleted()) {
            throw new IllegalArgumentException("Сетка плей-офф уже сформирована для этого сезона.");
        }

        List<SeasonStandingsRow> standingsRows = seasonStandingsRowRepository.findAllDetailedBySeasonId(seasonId);
        if (standingsRows.size() < config.getTeamCount()) {
            throw new IllegalArgumentException("Недостаточно команд в итоговой таблице для формирования плей-офф.");
        }

        SeasonPlayoffBracket bracket = existingBracket == null ? new SeasonPlayoffBracket() : existingBracket;
        OffsetDateTime now = OffsetDateTime.now();
        if (bracket.getSeason() == null) {
            bracket.setSeason(season);
            bracket.setCreatedByUserId(actorUserId);
            bracket.setCreatedAt(now);
        }
        bracket.setStatus(STATUS_READY);
        bracket.setRegularSeasonCompleted(true);
        bracket.setGeneratedAt(now);
        bracket.setBasedOnStandingsCalculatedAt(seasonStandingsService.getSeasonStandings(seasonId).config().getLastCalculatedAt());
        bracket.setUpdatedByUserId(actorUserId);
        bracket.setUpdatedAt(now);
        SeasonPlayoffBracket savedBracket = seasonPlayoffBracketRepository.save(bracket);

        if (savedBracket.getId() != null && seasonPlayoffTieRepository.existsByBracket_Season_Id(seasonId)) {
            seasonPlayoffTieRepository.deleteAllByBracket_Id(savedBracket.getId());
        }

        generateTies(savedBracket, config, standingsRows, now);
        int lastRegularSortOrder = regularTours.stream()
            .filter(tour -> matchesPerTour.getOrDefault(tour.getId(), 0) > 0)
            .mapToInt(Tour::getSortOrder)
            .max()
            .orElse(0);
        syncPlayoffTours(season, config, actorUserId, lastRegularSortOrder);

        season.setStatus(SeasonStatus.ACTIVE);
        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(now);
        return seasonRepository.save(season);
    }

    private void generateTies(
        SeasonPlayoffBracket bracket,
        SeasonPlayoffConfig config,
        List<SeasonStandingsRow> standingsRows,
        OffsetDateTime now
    ) {
        int teamCount = config.getTeamCount() == null ? 0 : config.getTeamCount();
        List<RoundPlan> plans = roundPlans(teamCount, config.isThirdPlaceEnabled());
        Map<String, List<SeasonPlayoffTie>> tiesByRound = new LinkedHashMap<>();

        if (!plans.isEmpty()) {
            RoundPlan firstRound = plans.get(0);
            List<SeasonPlayoffTie> firstRoundTies = new ArrayList<>();
            for (int slotIndex = 0; slotIndex < firstRound.tieCount(); slotIndex += 1) {
                int homeSeed = slotIndex + 1;
                int awaySeed = teamCount - slotIndex;
                SeasonPlayoffTie tie = new SeasonPlayoffTie();
                tie.setBracket(bracket);
                tie.setRoundCode(firstRound.roundCode());
                tie.setRoundOrder(firstRound.roundOrder());
                tie.setSlotOrder(slotIndex + 1);
                tie.setLegCount(legCountForRound(config, firstRound.roundCode()));
                tie.setTitle(defaultTieTitle(firstRound.roundCode(), slotIndex + 1, firstRound.tieCount()));
                tie.setHomeSeed(homeSeed);
                tie.setAwaySeed(awaySeed);
                tie.setHomeTeam(standingsRows.get(homeSeed - 1).getTeam());
                tie.setAwayTeam(standingsRows.get(awaySeed - 1).getTeam());
                tie.setStatus(STATUS_PLANNED);
                tie.setCreatedAt(now);
                tie.setUpdatedAt(now);
                firstRoundTies.add(seasonPlayoffTieRepository.save(tie));
            }
            tiesByRound.put(firstRound.roundCode(), firstRoundTies);
        }

        for (int planIndex = 1; planIndex < plans.size(); planIndex += 1) {
            RoundPlan plan = plans.get(planIndex);
            if ("THIRD_PLACE".equals(plan.roundCode())) {
                List<SeasonPlayoffTie> semifinalTies = tiesByRound.getOrDefault("SEMIFINAL", List.of());
                if (semifinalTies.size() < 2) {
                    continue;
                }
                SeasonPlayoffTie tie = new SeasonPlayoffTie();
                tie.setBracket(bracket);
                tie.setRoundCode(plan.roundCode());
                tie.setRoundOrder(plan.roundOrder());
                tie.setSlotOrder(1);
                tie.setLegCount(legCountForRound(config, plan.roundCode()));
                tie.setTitle(defaultTieTitle(plan.roundCode(), 1, 1));
                tie.setHomeSourceTie(semifinalTies.get(0));
                tie.setHomeSourceResult(SOURCE_LOSER);
                tie.setAwaySourceTie(semifinalTies.get(1));
                tie.setAwaySourceResult(SOURCE_LOSER);
                tie.setStatus(STATUS_PLANNED);
                tie.setCreatedAt(now);
                tie.setUpdatedAt(now);
                tiesByRound.put(plan.roundCode(), List.of(seasonPlayoffTieRepository.save(tie)));
                continue;
            }

            RoundPlan previousPlan = findPreviousCompetitiveRound(plans, planIndex);
            List<SeasonPlayoffTie> previousRoundTies = previousPlan == null ? List.of() : tiesByRound.getOrDefault(previousPlan.roundCode(), List.of());
            if (previousRoundTies.isEmpty()) {
                continue;
            }

            List<SeasonPlayoffTie> currentRoundTies = new ArrayList<>();
            for (int slotIndex = 0; slotIndex < plan.tieCount(); slotIndex += 1) {
                int sourceIndex = slotIndex * 2;
                SeasonPlayoffTie tie = new SeasonPlayoffTie();
                tie.setBracket(bracket);
                tie.setRoundCode(plan.roundCode());
                tie.setRoundOrder(plan.roundOrder());
                tie.setSlotOrder(slotIndex + 1);
                tie.setLegCount(legCountForRound(config, plan.roundCode()));
                tie.setTitle(defaultTieTitle(plan.roundCode(), slotIndex + 1, plan.tieCount()));
                tie.setHomeSourceTie(previousRoundTies.get(sourceIndex));
                tie.setHomeSourceResult(SOURCE_WINNER);
                tie.setAwaySourceTie(previousRoundTies.get(sourceIndex + 1));
                tie.setAwaySourceResult(SOURCE_WINNER);
                tie.setStatus(STATUS_PLANNED);
                tie.setCreatedAt(now);
                tie.setUpdatedAt(now);
                currentRoundTies.add(seasonPlayoffTieRepository.save(tie));
            }
            tiesByRound.put(plan.roundCode(), currentRoundTies);
        }
    }

    private void syncPlayoffTours(Season season, SeasonPlayoffConfig config, Long actorUserId, int regularSortOrderMax) {
        List<RoundPlan> plans = roundPlans(config.getTeamCount() == null ? 0 : config.getTeamCount(), config.isThirdPlaceEnabled());
        OffsetDateTime now = OffsetDateTime.now();

        for (int index = 0; index < plans.size(); index += 1) {
            RoundPlan plan = plans.get(index);
            Tour tour = tourRepository.findBySeason_IdAndNameIgnoreCase(season.getId(), tourNameForRound(plan.roundCode()))
                .orElseGet(Tour::new);

            if (tour.getSeason() == null) {
                tour.setSeason(season);
                tour.setCreatedByUserId(actorUserId);
                tour.setCreatedAt(now);
            }
            tour.setName(tourNameForRound(plan.roundCode()));
            tour.setStageType(PLAYOFF_STAGE);
            tour.setRoundNumber(null);
            tour.setSortOrder(regularSortOrderMax + index + 1);
            tour.setPublished(false);
            tour.setActive(true);
            tour.setUpdatedByUserId(actorUserId);
            tour.setUpdatedAt(now);
            tourRepository.save(tour);
        }
    }

    private int legCountForRound(SeasonPlayoffConfig config, String roundCode) {
        return switch (roundCode) {
            case "ROUND_OF_16" -> config.getRoundOf16Legs();
            case "QUARTERFINAL" -> config.getQuarterfinalLegs();
            case "SEMIFINAL" -> config.getSemifinalLegs();
            case "FINAL" -> config.getFinalLegs();
            case "THIRD_PLACE" -> config.getThirdPlaceLegs();
            default -> 1;
        };
    }

    private List<RoundPlan> roundPlans(int teamCount, boolean thirdPlaceEnabled) {
        List<RoundPlan> plans = new ArrayList<>();
        if (teamCount >= 16) {
            plans.add(new RoundPlan("ROUND_OF_16", 1, 8));
        }
        if (teamCount >= 8) {
            plans.add(new RoundPlan("QUARTERFINAL", plans.size() + 1, 4));
        }
        if (teamCount >= 4) {
            plans.add(new RoundPlan("SEMIFINAL", plans.size() + 1, 2));
        }
        if (teamCount >= 2) {
            plans.add(new RoundPlan("FINAL", plans.size() + 1, 1));
        }
        if (thirdPlaceEnabled && teamCount >= 4) {
            plans.add(new RoundPlan("THIRD_PLACE", plans.size() + 1, 1));
        }
        return plans;
    }

    private RoundPlan findPreviousCompetitiveRound(List<RoundPlan> plans, int currentIndex) {
        for (int index = currentIndex - 1; index >= 0; index -= 1) {
            RoundPlan plan = plans.get(index);
            if (!"THIRD_PLACE".equals(plan.roundCode())) {
                return plan;
            }
        }
        return null;
    }

    private String defaultTieTitle(String roundCode, int slotOrder, int tieCount) {
        String label = tourNameForRound(roundCode);
        if (tieCount <= 1) {
            return label;
        }
        return label + " · пара " + slotOrder;
    }

    private String tourNameForRound(String roundCode) {
        return switch (roundCode) {
            case "ROUND_OF_16" -> "1/8 финала";
            case "QUARTERFINAL" -> "1/4 финала";
            case "SEMIFINAL" -> "1/2 финала";
            case "FINAL" -> "Финал";
            case "THIRD_PLACE" -> "Матч за 3 место";
            default -> "Плей-офф";
        };
    }

    private Season getExistingSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
    }

    public record SeasonPlayoffConfigData(
        boolean enabled,
        Integer teamCount,
        boolean thirdPlaceEnabled,
        int roundOf16Legs,
        int quarterfinalLegs,
        int semifinalLegs,
        int finalLegs,
        int thirdPlaceLegs
    ) {}

    public record SeasonPlayoffBracketData(
        SeasonPlayoffConfigData config,
        SeasonPlayoffBracket bracket,
        List<SeasonPlayoffTie> ties
    ) {}

    private record RoundPlan(String roundCode, int roundOrder, int tieCount) {}
}
