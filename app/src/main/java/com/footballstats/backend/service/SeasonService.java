package com.footballstats.backend.service;

import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.Referee;
import com.footballstats.backend.domain.SeasonReferee;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.SeasonTeam;
import com.footballstats.backend.domain.Team;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.repository.RefereeRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonRefereeRepository;
import com.footballstats.backend.repository.SeasonStandingsConfigRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SeasonService {

    private final RefereeRepository refereeRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonRefereeRepository seasonRefereeRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final TeamRepository teamRepository;
    private final SeasonStandingsConfigRepository seasonStandingsConfigRepository;
    private final SeasonStructureService seasonStructureService;
    private final SeasonPlayerService seasonPlayerService;
    private final SeasonStandingsService seasonStandingsService;
    private final ObjectMapper objectMapper;

    public SeasonService(
        RefereeRepository refereeRepository,
        SeasonRepository seasonRepository,
        SeasonRefereeRepository seasonRefereeRepository,
        SeasonTeamRepository seasonTeamRepository,
        TeamRepository teamRepository,
        SeasonStandingsConfigRepository seasonStandingsConfigRepository,
        SeasonStructureService seasonStructureService,
        SeasonPlayerService seasonPlayerService,
        SeasonStandingsService seasonStandingsService,
        ObjectMapper objectMapper
    ) {
        this.refereeRepository = refereeRepository;
        this.seasonRepository = seasonRepository;
        this.seasonRefereeRepository = seasonRefereeRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.teamRepository = teamRepository;
        this.seasonStandingsConfigRepository = seasonStandingsConfigRepository;
        this.seasonStructureService = seasonStructureService;
        this.seasonPlayerService = seasonPlayerService;
        this.seasonStandingsService = seasonStandingsService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Season> listActiveSeasons() {
        return seasonRepository.findAllByActiveTrueOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Season> listAllSeasons() {
        return seasonRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    @Transactional
    public Season createSeason(
        String rawName,
        Integer roundsCount,
        Boolean playoffEnabled,
        Integer playoffTeamCount,
        LocalDate applicationDeadline,
        List<String> rankingRules,
        List<Long> refereeIds,
        Integer yellowCardsForSuspension,
        Integer redCardsForSuspension,
        Long actorUserId
    ) {
        String normalizedName = normalizeName(rawName);
        validateUniqueName(normalizedName, null);

        Season season = new Season();
        season.setName(normalizedName);
        applyFormat(season, roundsCount, playoffEnabled, playoffTeamCount, false);
        season.setApplicationDeadline(applicationDeadline);
        season.setCreatedByUserId(actorUserId);
        season.setUpdatedByUserId(actorUserId);
        season.setActive(true);
        season.setUpdatedAt(OffsetDateTime.now());
        Season savedSeason = seasonRepository.save(season);
        seasonStandingsService.initializeSeasonStandings(savedSeason.getId(), actorUserId);
        updateStandingsConfig(savedSeason.getId(), rankingRules, yellowCardsForSuspension, redCardsForSuspension, actorUserId);
        replaceSeasonReferees(savedSeason, refereeIds, actorUserId);
        return savedSeason;
    }

    @Transactional
    public Season updateSeason(
        Long seasonId,
        String rawName,
        Integer roundsCount,
        Boolean playoffEnabled,
        Integer playoffTeamCount,
        LocalDate applicationDeadline,
        List<String> rankingRules,
        List<Long> refereeIds,
        Integer yellowCardsForSuspension,
        Integer redCardsForSuspension,
        Long actorUserId
    ) {
        Season season = getExistingSeason(seasonId);
        String normalizedName = normalizeName(rawName);
        validateUniqueName(normalizedName, seasonId);

        int normalizedRoundsCount = normalizeRoundsCount(roundsCount);
        boolean changingRegularStructure = season.getRoundsCount() != normalizedRoundsCount;
        boolean hasRegularMatches = seasonStructureService.hasRegularStageMatches(seasonId);
        if (changingRegularStructure && hasRegularMatches) {
            throw new IllegalArgumentException(
                "Нельзя менять количество кругов после добавления матчей регулярного этапа."
            );
        }

        season.setName(normalizedName);
        applyFormat(season, roundsCount, playoffEnabled, playoffTeamCount, true);
    season.setApplicationDeadline(applicationDeadline);
        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(OffsetDateTime.now());
        Season savedSeason = seasonRepository.save(season);
        updateStandingsConfig(seasonId, rankingRules, yellowCardsForSuspension, redCardsForSuspension, actorUserId);
        replaceSeasonReferees(savedSeason, refereeIds, actorUserId);
        if (!hasRegularMatches) {
            seasonStructureService.syncRegularToursForSeason(savedSeason, actorUserId);
        }
        seasonStandingsService.recalculateSeasonStandings(seasonId, actorUserId);
        return savedSeason;
    }

    @Transactional(readOnly = true)
    public SeasonStandingsConfig getStandingsConfig(Long seasonId) {
        getExistingSeason(seasonId);
        return seasonStandingsConfigRepository.findBySeason_Id(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Конфигурация сезона не найдена."));
    }

    @Transactional
    public Season deactivateSeason(Long seasonId, Long actorUserId) {
        Season season = getExistingSeason(seasonId);
        if (!season.isActive()) {
            return season;
        }

        season.setActive(false);
        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(OffsetDateTime.now());
        return seasonRepository.save(season);
    }

    @Transactional(readOnly = true)
    public List<Team> listSeasonTeams(Long seasonId) {
        getExistingSeason(seasonId);
        return seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(seasonId).stream()
            .map(SeasonTeam::getTeam)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Referee> listSeasonReferees(Long seasonId) {
        getExistingSeason(seasonId);
        return seasonRefereeRepository.findAllBySeasonIdOrderByRefereeFullNameAsc(seasonId).stream()
            .map(SeasonReferee::getReferee)
            .toList();
    }

    @Transactional
    public List<Team> replaceSeasonTeams(Long seasonId, List<Long> teamIds, Long actorUserId) {
        Season season = getExistingSeason(seasonId);
        Set<Long> uniqueTeamIds = new LinkedHashSet<>(teamIds == null ? List.of() : teamIds);
        if (uniqueTeamIds.isEmpty()) {
            throw new IllegalArgumentException("Нужно выбрать хотя бы одну команду для сезона.");
        }
        if (seasonStructureService.hasRegularStageMatches(seasonId)) {
            throw new IllegalArgumentException(
                "Нельзя менять состав команд сезона после добавления матчей регулярного этапа."
            );
        }

        List<Team> teams = uniqueTeamIds.stream()
            .map(this::getExistingTeam)
            .toList();

        validatePlayoffConfigForTeamCount(season.isPlayoffEnabled(), season.getPlayoffTeamCount(), teams.size());

        seasonPlayerService.deactivateSeasonPlayersForRemovedTeams(seasonId, uniqueTeamIds, actorUserId);

        seasonTeamRepository.deleteAllBySeason_Id(seasonId);
        seasonTeamRepository.flush();

        for (Team team : teams) {
            SeasonTeam seasonTeam = new SeasonTeam();
            seasonTeam.setSeason(season);
            seasonTeam.setTeam(team);
            seasonTeam.setCreatedByUserId(actorUserId);
            seasonTeamRepository.save(seasonTeam);
        }

        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(OffsetDateTime.now());
        seasonRepository.save(season);
        seasonStructureService.syncRegularToursForSeason(season, actorUserId);
        seasonStandingsService.recalculateSeasonStandings(seasonId, actorUserId);

        return teams.stream().sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName())).toList();
    }

    @Transactional(readOnly = true)
    public int calculateRegularToursCount(Long seasonId) {
        Season season = getExistingSeason(seasonId);
        return seasonStructureService.calculateRegularToursCountForSeason(seasonId, season.getRoundsCount());
    }

    @Transactional(readOnly = true)
    public Season getSeason(Long seasonId) {
        return getExistingSeason(seasonId);
    }

    private Season getExistingSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
    }

    private Team getExistingTeam(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
    }

    private Referee getExistingReferee(Long refereeId) {
        return refereeRepository.findById(refereeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Судья не найден."));
    }

    private void validateUniqueName(String normalizedName, Long currentSeasonId) {
        seasonRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            if (currentSeasonId == null || !existing.getId().equals(currentSeasonId)) {
                throw new IllegalArgumentException("Сезон с таким названием уже существует.");
            }
        });
    }

    private void applyFormat(Season season, Integer roundsCount, Boolean playoffEnabled, Integer playoffTeamCount, boolean validateCurrentTeamCount) {
        int normalizedRoundsCount = normalizeRoundsCount(roundsCount);
        boolean normalizedPlayoffEnabled = Boolean.TRUE.equals(playoffEnabled);
        Integer normalizedPlayoffTeamCount = normalizePlayoffTeamCount(normalizedPlayoffEnabled, playoffTeamCount);

        if (validateCurrentTeamCount) {
            long teamCount = seasonTeamRepository.countBySeason_Id(season.getId());
            validatePlayoffConfigForTeamCount(normalizedPlayoffEnabled, normalizedPlayoffTeamCount, teamCount);
        }

        season.setRoundsCount(normalizedRoundsCount);
        season.setPlayoffEnabled(normalizedPlayoffEnabled);
        season.setPlayoffTeamCount(normalizedPlayoffTeamCount);
    }

    private void updateStandingsConfig(
        Long seasonId,
        List<String> rankingRules,
        Integer yellowCardsForSuspension,
        Integer redCardsForSuspension,
        Long actorUserId
    ) {
        SeasonStandingsConfig config = getStandingsConfig(seasonId);
        config.setRankingRulesJson(StandingsRankingRules.toJson(rankingRules, objectMapper));
        config.setYellowCardsForSuspension(normalizeSuspensionThreshold(yellowCardsForSuspension, "ЖК"));
        config.setRedCardsForSuspension(normalizeSuspensionThreshold(redCardsForSuspension, "КК"));
        config.setUpdatedByUserId(actorUserId);
        config.setUpdatedAt(OffsetDateTime.now());
        seasonStandingsConfigRepository.save(config);
    }

    private void replaceSeasonReferees(Season season, List<Long> refereeIds, Long actorUserId) {
        Set<Long> uniqueRefereeIds = new LinkedHashSet<>(refereeIds == null ? List.of() : refereeIds);
        seasonRefereeRepository.deleteAllBySeason_Id(season.getId());
        seasonRefereeRepository.flush();

        for (Long refereeId : uniqueRefereeIds) {
            if (refereeId == null) {
                continue;
            }
            SeasonReferee seasonReferee = new SeasonReferee();
            seasonReferee.setSeason(season);
            seasonReferee.setReferee(getExistingReferee(refereeId));
            seasonReferee.setCreatedByUserId(actorUserId);
            seasonRefereeRepository.save(seasonReferee);
        }
    }

    private int normalizeRoundsCount(Integer roundsCount) {
        int normalized = roundsCount == null ? 1 : roundsCount;
        if (normalized < 1) {
            throw new IllegalArgumentException("Количество кругов должно быть не меньше 1.");
        }
        return normalized;
    }

    private Integer normalizePlayoffTeamCount(boolean playoffEnabled, Integer playoffTeamCount) {
        if (!playoffEnabled) {
            return null;
        }
        if (playoffTeamCount == null) {
            throw new IllegalArgumentException("Укажите количество команд для плей-офф.");
        }
        if (playoffTeamCount < 2 || (playoffTeamCount & (playoffTeamCount - 1)) != 0) {
            throw new IllegalArgumentException("Количество команд плей-офф должно быть степенью двойки.");
        }
        return playoffTeamCount;
    }

    private int normalizeSuspensionThreshold(Integer value, String label) {
        int normalized = value == null ? 0 : value;
        if (normalized < 0) {
            throw new IllegalArgumentException("Порог пропуска по " + label + " не может быть отрицательным.");
        }
        return normalized;
    }

    private void validatePlayoffConfigForTeamCount(boolean playoffEnabled, Integer playoffTeamCount, long teamCount) {
        if (!playoffEnabled || playoffTeamCount == null || teamCount == 0) {
            return;
        }
        if (playoffTeamCount > teamCount) {
            throw new IllegalArgumentException("Команд в сезоне меньше, чем требуется для плей-офф.");
        }
    }

    private String normalizeName(String rawName) {
        String normalizedName = String.valueOf(rawName == null ? "" : rawName).trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Название сезона обязательно.");
        }
        return normalizedName;
    }
}
