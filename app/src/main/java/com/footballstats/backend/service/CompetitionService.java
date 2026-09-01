package com.footballstats.backend.service;

import com.footballstats.backend.domain.*;
import com.footballstats.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CompetitionService {
    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository competitionTeamRepository;
    private final CompetitionRosterPlayerRepository rosterRepository;
    private final CupTieRepository cupTieRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final SeasonPlayerRepository seasonPlayerRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TourRepository tourRepository;
    private final TourMatchRepository tourMatchRepository;
    private final MatchProtocolRepository matchProtocolRepository;
    private final CupTieMatchRepository cupTieMatchRepository;

    public CompetitionService(
        CompetitionRepository competitionRepository,
        CompetitionTeamRepository competitionTeamRepository,
        CompetitionRosterPlayerRepository rosterRepository,
        CupTieRepository cupTieRepository,
        SeasonRepository seasonRepository,
        SeasonTeamRepository seasonTeamRepository,
        SeasonPlayerRepository seasonPlayerRepository,
        TeamRepository teamRepository,
        PlayerRepository playerRepository,
        TourRepository tourRepository,
        TourMatchRepository tourMatchRepository,
        MatchProtocolRepository matchProtocolRepository,
        CupTieMatchRepository cupTieMatchRepository
    ) {
        this.competitionRepository = competitionRepository;
        this.competitionTeamRepository = competitionTeamRepository;
        this.rosterRepository = rosterRepository;
        this.cupTieRepository = cupTieRepository;
        this.seasonRepository = seasonRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.seasonPlayerRepository = seasonPlayerRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.tourRepository = tourRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.matchProtocolRepository = matchProtocolRepository;
        this.cupTieMatchRepository = cupTieMatchRepository;
    }

    @Transactional
    public Competition ensureChampionship(Long seasonId, Long actorUserId) {
        return competitionRepository.findBySeason_IdAndTypeAndActiveTrue(seasonId, CompetitionType.CHAMPIONSHIP)
            .orElseGet(() -> {
                Season season = getSeason(seasonId);
                Competition competition = new Competition();
                competition.setSeason(season);
                competition.setName("Чемпионат");
                competition.setType(CompetitionType.CHAMPIONSHIP);
                competition.setStatus(season.getStatus() == SeasonStatus.ACTIVE ? "ACTIVE" : "DRAFT");
                competition.setRosterMode(CompetitionRosterMode.SEASON_SHARED);
                competition.setMaxRosterSize(season.getMaxRosterSize());
                competition.setPlayersOnField(season.getPlayersOnField());
                competition.setCreatedByUserId(actorUserId);
                competition.setUpdatedByUserId(actorUserId);
                Competition saved = competitionRepository.save(competition);
                replaceTeams(saved, seasonTeamRepository.findTeamIdsBySeasonId(seasonId).stream().map(SeasonTeamRepository.TeamSeasonProjection::getTeamId).toList(), actorUserId);
                return saved;
            });
    }

    @Transactional
    public void syncChampionshipTeams(Long seasonId, Long actorUserId) {
        Competition championship = ensureChampionship(seasonId, actorUserId);
        List<Long> teamIds = seasonTeamRepository.findTeamIdsBySeasonId(seasonId).stream()
            .map(SeasonTeamRepository.TeamSeasonProjection::getTeamId)
            .toList();
        replaceTeams(championship, teamIds, actorUserId);
    }

    @Transactional
    public List<CompetitionData> list(Long seasonId) {
        getSeason(seasonId);
        return competitionRepository.findAllActiveDetailedBySeasonId(seasonId).stream().map(this::toData).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasChampionship(Long seasonId) {
        return competitionRepository.findBySeason_IdAndTypeAndActiveTrue(seasonId, CompetitionType.CHAMPIONSHIP).isPresent();
    }

    @Transactional
    public CompetitionData createChampionship(Long seasonId, String rawName, Long actorUserId) {
        if (hasChampionship(seasonId)) {
            throw new IllegalArgumentException("В этом сезоне уже есть чемпионат.");
        }
        Season season = getSeason(seasonId);
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Укажите название чемпионата.");
        Competition competition = new Competition();
        competition.setSeason(season);
        competition.setName(name);
        competition.setType(CompetitionType.CHAMPIONSHIP);
        competition.setStatus(season.getStatus() == SeasonStatus.ACTIVE ? "ACTIVE" : "DRAFT");
        competition.setRosterMode(CompetitionRosterMode.SEASON_SHARED);
        competition.setMaxRosterSize(season.getMaxRosterSize());
        competition.setPlayersOnField(season.getPlayersOnField());
        competition.setCreatedByUserId(actorUserId);
        competition.setUpdatedByUserId(actorUserId);
        Competition saved = competitionRepository.save(competition);
        replaceTeams(saved, seasonTeamRepository.findTeamIdsBySeasonId(seasonId).stream()
            .map(SeasonTeamRepository.TeamSeasonProjection::getTeamId).toList(), actorUserId);
        return toData(saved);
    }

    @Transactional
    public CompetitionData renameChampionship(Long seasonId, Long competitionId, String rawName, Long actorUserId) {
        Competition competition = competitionRepository.findDetailedById(competitionId)
            .filter(item -> item.isActive() && item.getSeason().getId().equals(seasonId) && item.getType() == CompetitionType.CHAMPIONSHIP)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Чемпионат не найден."));
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Укажите название чемпионата.");
        competition.setName(name);
        competition.setUpdatedByUserId(actorUserId);
        competition.setUpdatedAt(OffsetDateTime.now());
        return toData(competitionRepository.save(competition));
    }

    @Transactional
    public void markChampionshipFinished(Long seasonId, Long actorUserId) {
        competitionRepository.findBySeason_IdAndTypeAndActiveTrue(seasonId, CompetitionType.CHAMPIONSHIP).ifPresent(competition -> {
            competition.setStatus("FINISHED");
            competition.setUpdatedByUserId(actorUserId);
            competition.setUpdatedAt(OffsetDateTime.now());
            competitionRepository.save(competition);
        });
    }

    @Transactional
    public CompetitionData createCup(Long seasonId, CompetitionSettings settings, Long actorUserId) {
        Season season = getSeason(seasonId);
        Competition competition = new Competition();
        competition.setSeason(season);
        competition.setType(CompetitionType.CUP);
        competition.setCreatedByUserId(actorUserId);
        applySettings(competition, settings, true);
        Competition saved = competitionRepository.save(competition);
        replaceTeams(saved, settings.teamIds(), actorUserId);
        return toData(saved);
    }

    @Transactional
    public CompetitionData updateCup(Long seasonId, Long competitionId, CompetitionSettings settings, Long actorUserId) {
        Competition competition = getCup(seasonId, competitionId);
        if ("CONFIRMED".equals(competition.getDrawStatus())) {
            throw new IllegalArgumentException("Нельзя менять основные правила после утверждения кубковой сетки.");
        }
        applySettings(competition, settings, false);
        competition.setUpdatedByUserId(actorUserId);
        competition.setUpdatedAt(OffsetDateTime.now());
        Competition saved = competitionRepository.save(competition);
        replaceTeams(saved, settings.teamIds(), actorUserId);
        resetDraw(saved);
        return toData(saved);
    }

    @Transactional
    public CompetitionData deactivate(Long seasonId, Long competitionId, Long actorUserId) {
        Competition competition = getCup(seasonId, competitionId);
        competition.setActive(false);
        competition.setUpdatedByUserId(actorUserId);
        competition.setUpdatedAt(OffsetDateTime.now());
        return toData(competitionRepository.save(competition));
    }

    @Transactional
    public CompetitionData draw(Long seasonId, Long competitionId, List<Long> orderedTeamIds, Long actorUserId) {
        Competition competition = getCup(seasonId, competitionId);
        if ("CONFIRMED".equals(competition.getDrawStatus())) {
            throw new IllegalArgumentException("Сетка уже утверждена.");
        }
        List<Team> participants = competitionTeamRepository.findAllDetailedByCompetitionId(competitionId).stream()
            .map(CompetitionTeam::getTeam)
            .toList();
        if (participants.size() < 2) {
            throw new IllegalArgumentException("Для жеребьевки нужны минимум две команды.");
        }

        List<Team> drawOrder = resolveDrawOrder(participants, orderedTeamIds);
        cupTieRepository.deleteAllByCompetition_Id(competitionId);
        cupTieRepository.flush();
        generateBracket(competition, drawOrder);
        competition.setDrawStatus("DRAFT");
        competition.setUpdatedByUserId(actorUserId);
        competition.setUpdatedAt(OffsetDateTime.now());
        return toData(competitionRepository.save(competition));
    }

    @Transactional
    public CompetitionData confirmDraw(Long seasonId, Long competitionId, Long actorUserId) {
        Competition competition = getCup(seasonId, competitionId);
        if (cupTieRepository.findAllDetailedByCompetitionId(competitionId).isEmpty()) {
            throw new IllegalArgumentException("Сначала проведите жеребьевку.");
        }
        competition.setDrawStatus("CONFIRMED");
        competition.setStatus("ACTIVE");
        competition.setUpdatedByUserId(actorUserId);
        competition.setUpdatedAt(OffsetDateTime.now());
        return toData(competitionRepository.save(competition));
    }

    @Transactional
    public CompetitionData scheduleTie(Long seasonId, Long competitionId, Long tieId, List<OffsetDateTime> kickoffDates, Long actorUserId) {
        Competition competition = getCup(seasonId, competitionId);
        if (!"CONFIRMED".equals(competition.getDrawStatus())) {
            throw new IllegalArgumentException("Сначала утвердите кубковую сетку.");
        }
        CupTie tie = cupTieRepository.findById(tieId)
            .filter(item -> item.getCompetition().getId().equals(competitionId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пара Кубка не найдена."));
        if (tie.getHomeTeam() == null || tie.getAwayTeam() == null) {
            throw new IllegalArgumentException("Участники этой пары ещё не определены.");
        }
        if (!cupTieMatchRepository.findAllDetailedByTieId(tieId).isEmpty()) {
            throw new IllegalArgumentException("Матчи этой пары уже созданы.");
        }
        List<OffsetDateTime> dates = kickoffDates == null ? List.of() : kickoffDates.stream().filter(Objects::nonNull).toList();
        if (dates.size() != tie.getLegCount()) {
            throw new IllegalArgumentException("Нужно указать даты всех матчей пары: " + tie.getLegCount() + ".");
        }
        Tour round = tourRepository.findByCompetition_IdAndNameIgnoreCase(competitionId, roundLabel(tie.getRoundCode()))
            .orElseGet(() -> {
                Tour created = new Tour();
                created.setSeason(competition.getSeason());
                created.setCompetition(competition);
                created.setName(roundLabel(tie.getRoundCode()));
                created.setStageType("CUP");
                created.setSortOrder(tie.getRoundOrder());
                created.setPublished(true);
                created.setActive(true);
                created.setCreatedByUserId(actorUserId);
                created.setUpdatedByUserId(actorUserId);
                return tourRepository.save(created);
            });
        for (int index = 0; index < dates.size(); index++) {
            boolean reverse = index % 2 == 1;
            TourMatch match = new TourMatch();
            match.setTour(round);
            match.setHomeTeam(reverse ? tie.getAwayTeam() : tie.getHomeTeam());
            match.setAwayTeam(reverse ? tie.getHomeTeam() : tie.getAwayTeam());
            match.setKickoffAt(dates.get(index));
            match.setCreatedByUserId(actorUserId);
            match.setUpdatedByUserId(actorUserId);
            match.setUpdatedAt(OffsetDateTime.now());
            TourMatch savedMatch = tourMatchRepository.save(match);
            MatchProtocol protocol = new MatchProtocol();
            protocol.setMatch(savedMatch);
            protocol.setCreatedByUserId(actorUserId);
            protocol.setUpdatedByUserId(actorUserId);
            matchProtocolRepository.save(protocol);
            CupTieMatch link = new CupTieMatch();
            link.setTie(tie);
            link.setMatch(savedMatch);
            link.setLegNumber(index + 1);
            cupTieMatchRepository.save(link);
        }
        tie.setStatus("IN_PROGRESS");
        tie.setUpdatedAt(OffsetDateTime.now());
        cupTieRepository.save(tie);
        return toData(competition);
    }

    @Transactional
    public void refreshCupAfterMatch(Long matchId) {
        CupTieMatch link = cupTieMatchRepository.findDetailedByMatchId(matchId).orElse(null);
        if (link == null) return;
        CupTie tie = link.getTie();
        List<CupTieMatch> matches = cupTieMatchRepository.findAllDetailedByTieId(tie.getId());
        if (matches.size() != tie.getLegCount() || matches.stream().anyMatch(item -> item.getMatch().getProtocol() == null || item.getMatch().getProtocol().getStatus() != MatchProtocolStatus.VERIFIED)) return;
        int homeTotal = 0;
        int awayTotal = 0;
        for (CupTieMatch item : matches) {
            TourMatch match = item.getMatch();
            MatchProtocol protocol = match.getProtocol();
            boolean originalOrder = match.getHomeTeam().getId().equals(tie.getHomeTeam().getId());
            homeTotal += originalOrder ? protocol.getHomeScore() : protocol.getAwayScore();
            awayTotal += originalOrder ? protocol.getAwayScore() : protocol.getHomeScore();
        }
        tie.setAggregateHomeScore(homeTotal);
        tie.setAggregateAwayScore(awayTotal);
        if (homeTotal != awayTotal) finishTie(tie, homeTotal > awayTotal ? tie.getHomeTeam() : tie.getAwayTeam());
        cupTieRepository.save(tie);
    }

    @Transactional
    public CompetitionData chooseTieWinner(Long seasonId, Long competitionId, Long tieId, Integer homePenaltyScore, Integer awayPenaltyScore) {
        Competition competition = getCup(seasonId, competitionId);
        if (!competition.isPenaltiesEnabled()) {
            throw new IllegalArgumentException("Ручной выбор победителя доступен только при включенной серии пенальти.");
        }
        CupTie tie = cupTieRepository.findById(tieId).filter(item -> item.getCompetition().getId().equals(competitionId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пара Кубка не найдена."));
        refreshCupAfterMatch(cupTieMatchRepository.findAllDetailedByTieId(tieId).stream().reduce((first, second) -> second).map(item -> item.getMatch().getId()).orElse(-1L));
        if (tie.getAggregateHomeScore() == null || !tie.getAggregateHomeScore().equals(tie.getAggregateAwayScore())) {
            throw new IllegalArgumentException("Ручной выбор победителя доступен только при равном общем счёте.");
        }
        if (homePenaltyScore == null || awayPenaltyScore == null || homePenaltyScore < 0 || awayPenaltyScore < 0 || homePenaltyScore.equals(awayPenaltyScore)) {
            throw new IllegalArgumentException("Укажите неравный неотрицательный счёт серии пенальти.");
        }
        tie.setHomePenaltyScore(homePenaltyScore);
        tie.setAwayPenaltyScore(awayPenaltyScore);
        Team winner = homePenaltyScore > awayPenaltyScore ? tie.getHomeTeam() : tie.getAwayTeam();
        finishTie(tie, winner);
        cupTieRepository.save(tie);
        return toData(competition);
    }

    private void finishTie(CupTie tie, Team winner) {
        Team loser = winner.getId().equals(tie.getHomeTeam().getId()) ? tie.getAwayTeam() : tie.getHomeTeam();
        tie.setWinnerTeam(winner);
        tie.setStatus("FINISHED");
        tie.setUpdatedAt(OffsetDateTime.now());
        cupTieRepository.save(tie);
        for (CupTie next : cupTieRepository.findAllDetailedByCompetitionId(tie.getCompetition().getId())) {
            boolean changed = false;
            if (next.getHomeSourceTie() != null && next.getHomeSourceTie().getId().equals(tie.getId())) {
                next.setHomeTeam("LOSER".equals(next.getHomeSourceResult()) ? loser : winner);
                changed = true;
            }
            if (next.getAwaySourceTie() != null && next.getAwaySourceTie().getId().equals(tie.getId())) {
                next.setAwayTeam("LOSER".equals(next.getAwaySourceResult()) ? loser : winner);
                changed = true;
            }
            if (changed) {
                if (next.getHomeTeam() != null && next.getAwayTeam() != null) next.setStatus("READY");
                next.setUpdatedAt(OffsetDateTime.now());
                cupTieRepository.save(next);
            }
        }
        if ("FINAL".equals(tie.getRoundCode())) {
            Competition competition = tie.getCompetition();
            competition.setStatus("FINISHED");
            competition.setUpdatedAt(OffsetDateTime.now());
            competitionRepository.save(competition);
        }
    }

    @Transactional(readOnly = true)
    public List<RosterPlayerData> roster(Long seasonId, Long competitionId) {
        Competition competition = getCup(seasonId, competitionId);
        Set<Long> participantIds = competitionTeamRepository.findAllDetailedByCompetitionId(competitionId).stream()
            .map(item -> item.getTeam().getId()).collect(java.util.stream.Collectors.toSet());
        if (competition.getRosterMode() == CompetitionRosterMode.SEASON_SHARED) {
            return participantIds.stream()
                .flatMap(teamId -> seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, teamId).stream())
                .map(item -> new RosterPlayerData(item.getPlayer().getId(), item.getPlayer().getFullName(), item.getTeam().getId(), item.getTeam().getName()))
                .sorted(Comparator.comparing(RosterPlayerData::teamName).thenComparing(RosterPlayerData::playerName))
                .toList();
        }
        Map<Long, SeasonPlayer> currentByPlayer = new HashMap<>();
        for (Long teamId : participantIds) {
            for (SeasonPlayer player : seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, teamId)) {
                currentByPlayer.put(player.getPlayer().getId(), player);
            }
        }
        return rosterRepository.findAllActiveDetailedByCompetitionId(competitionId).stream()
            .filter(item -> currentByPlayer.containsKey(item.getPlayer().getId()))
            .filter(item -> currentByPlayer.get(item.getPlayer().getId()).getTeam().getId().equals(item.getTeam().getId()))
            .map(item -> new RosterPlayerData(item.getPlayer().getId(), item.getPlayer().getFullName(), item.getTeam().getId(), item.getTeam().getName()))
            .toList();
    }

    @Transactional
    public List<RosterPlayerData> addRosterPlayers(Long seasonId, Long competitionId, Long teamId, List<Long> playerIds, Long actorUserId) {
        Competition competition = getCup(seasonId, competitionId);
        if (competition.getRosterMode() != CompetitionRosterMode.OWN) {
            throw new IllegalArgumentException("У этого Кубка используется общая заявка сезона.");
        }
        if (!competitionTeamRepository.existsByCompetition_IdAndTeam_Id(competitionId, teamId)) {
            throw new IllegalArgumentException("Команда не участвует в этом Кубке.");
        }
        List<Long> uniqueIds = playerIds == null ? List.of() : playerIds.stream().filter(Objects::nonNull).distinct().toList();
        Set<Long> existingIds = rosterRepository.findAllActiveDetailedByCompetitionId(competitionId).stream()
            .filter(item -> item.getTeam().getId().equals(teamId))
            .map(item -> item.getPlayer().getId())
            .collect(java.util.stream.Collectors.toSet());
        long additions = uniqueIds.stream().filter(id -> !existingIds.contains(id)).count();
        Integer limit = competition.getMaxRosterSize();
        if (limit != null && existingIds.size() + additions > limit) {
            throw new IllegalArgumentException("Будет превышен лимит заявки Кубка: " + limit + ".");
        }
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
        for (Long playerId : uniqueIds) {
            if (!seasonPlayerRepository.existsBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(seasonId, teamId, playerId)) {
                throw new IllegalArgumentException("Игрок должен быть заявлен за эту команду в сезоне.");
            }
            Player player = playerRepository.findById(playerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));
            CompetitionRosterPlayer rosterPlayer = rosterRepository.findByCompetition_IdAndPlayer_Id(competitionId, playerId).orElseGet(CompetitionRosterPlayer::new);
            if (rosterPlayer.getCompetition() == null) {
                rosterPlayer.setCompetition(competition);
                rosterPlayer.setPlayer(player);
                rosterPlayer.setCreatedByUserId(actorUserId);
            }
            rosterPlayer.setTeam(team);
            rosterPlayer.setActive(true);
            rosterPlayer.setUpdatedByUserId(actorUserId);
            rosterPlayer.setUpdatedAt(OffsetDateTime.now());
            rosterRepository.save(rosterPlayer);
        }
        return roster(seasonId, competitionId);
    }

    @Transactional(readOnly = true)
    public List<RosterPlayerData> rosterCandidates(Long seasonId, Long competitionId, Long teamId) {
        Competition competition = getCup(seasonId, competitionId);
        if (competition.getRosterMode() != CompetitionRosterMode.OWN) {
            throw new IllegalArgumentException("У этого Кубка используется общая заявка сезона.");
        }
        if (!competitionTeamRepository.existsByCompetition_IdAndTeam_Id(competitionId, teamId)) {
            throw new IllegalArgumentException("Команда не участвует в этом Кубке.");
        }
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
        return seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, teamId).stream()
            .map(item -> new RosterPlayerData(item.getPlayer().getId(), item.getPlayer().getFullName(), teamId, team.getName()))
            .sorted(Comparator.comparing(RosterPlayerData::playerName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Transactional
    public List<RosterPlayerData> removeRosterPlayer(Long seasonId, Long competitionId, Long teamId, Long playerId, Long actorUserId) {
        Competition competition = getCup(seasonId, competitionId);
        if (competition.getRosterMode() != CompetitionRosterMode.OWN) {
            throw new IllegalArgumentException("У этого Кубка используется общая заявка сезона.");
        }
        CompetitionRosterPlayer item = rosterRepository.findByCompetition_IdAndPlayer_Id(competitionId, playerId)
            .filter(value -> value.isActive() && value.getTeam().getId().equals(teamId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден в заявке Кубка."));
        item.setActive(false);
        item.setUpdatedByUserId(actorUserId);
        item.setUpdatedAt(OffsetDateTime.now());
        rosterRepository.save(item);
        return roster(seasonId, competitionId);
    }

    @Transactional(readOnly = true)
    public List<Player> eligiblePlayers(Long seasonId, Long competitionId, Long teamId) {
        Competition competition = getCup(seasonId, competitionId);
        if (!competitionTeamRepository.existsByCompetition_IdAndTeam_Id(competitionId, teamId)) return List.of();
        if (competition.getRosterMode() == CompetitionRosterMode.SEASON_SHARED) {
            return seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, teamId).stream().map(SeasonPlayer::getPlayer).toList();
        }
        Set<Long> currentIds = seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, teamId).stream()
            .map(item -> item.getPlayer().getId()).collect(java.util.stream.Collectors.toSet());
        return rosterRepository.findAllActiveDetailedByCompetitionId(competitionId).stream()
            .filter(item -> item.getTeam().getId().equals(teamId) && currentIds.contains(item.getPlayer().getId()))
            .map(CompetitionRosterPlayer::getPlayer).toList();
    }

    @Transactional(readOnly = true)
    public Competition getCompetition(Long competitionId) {
        return competitionRepository.findDetailedById(competitionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено."));
    }

    private void applySettings(Competition competition, CompetitionSettings settings, boolean creating) {
        String name = settings.name() == null ? "" : settings.name().trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Укажите название Кубка.");
        if (creating && competitionRepository.existsBySeason_IdAndNameIgnoreCaseAndActiveTrue(competition.getSeason().getId(), name)) {
            throw new IllegalArgumentException("Соревнование с таким названием уже существует в сезоне.");
        }
        int playersOnField = positive(settings.playersOnField(), 11, "Количество игроков на поле");
        Integer matchRoster = settings.matchRosterSize();
        if (matchRoster != null && matchRoster < playersOnField) throw new IllegalArgumentException("Состав на матч не может быть меньше стартового состава.");
        competition.setName(name);
        competition.setRosterMode(settings.rosterMode() == null ? CompetitionRosterMode.SEASON_SHARED : settings.rosterMode());
        competition.setMaxRosterSize(settings.maxRosterSize());
        competition.setMatchRosterSize(matchRoster);
        competition.setPlayersOnField(playersOnField);
        competition.setRegularTieLegs(legs(settings.regularTieLegs()));
        competition.setFinalLegs(legs(settings.finalLegs()));
        competition.setThirdPlaceEnabled(settings.thirdPlaceEnabled());
        competition.setThirdPlaceLegs(legs(settings.thirdPlaceLegs()));
        competition.setExtraTimeEnabled(settings.extraTimeEnabled());
        competition.setExtraTimeMinutes(settings.extraTimeEnabled() ? positive(settings.extraTimeMinutes(), 30, "Продолжительность дополнительного времени") : 0);
        competition.setPenaltiesEnabled(settings.penaltiesEnabled());
        if (!settings.extraTimeEnabled() && !settings.penaltiesEnabled()) {
            throw new IllegalArgumentException("Для Кубка нужно включить дополнительное время или серию пенальти.");
        }
        competition.setYellowCardsForSuspension(Math.max(0, settings.yellowCardsForSuspension()));
        competition.setYellowSuspensionMatches(positive(settings.yellowSuspensionMatches(), 1, "Дисквалификация за ЖК"));
        competition.setRedSuspensionMatches(positive(settings.redSuspensionMatches(), 1, "Дисквалификация за КК"));
    }

    private void replaceTeams(Competition competition, List<Long> teamIds, Long actorUserId) {
        List<Long> ids = teamIds == null ? List.of() : teamIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long teamId : ids) {
            if (!seasonTeamRepository.existsBySeason_IdAndTeam_Id(competition.getSeason().getId(), teamId)) {
                throw new IllegalArgumentException("Все участники соревнования должны быть добавлены в сезон.");
            }
        }
        competitionTeamRepository.deleteAllByCompetition_Id(competition.getId());
        competitionTeamRepository.flush();
        int seed = 1;
        for (Long teamId : ids) {
            Team team = teamRepository.findById(teamId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
            CompetitionTeam item = new CompetitionTeam();
            item.setCompetition(competition);
            item.setTeam(team);
            item.setSeedNumber(seed++);
            item.setCreatedByUserId(actorUserId);
            competitionTeamRepository.save(item);
        }
    }

    private List<Team> resolveDrawOrder(List<Team> participants, List<Long> orderedIds) {
        Map<Long, Team> byId = participants.stream().collect(java.util.stream.Collectors.toMap(Team::getId, item -> item));
        if (orderedIds != null && !orderedIds.isEmpty()) {
            List<Long> unique = orderedIds.stream().distinct().toList();
            if (unique.size() != participants.size() || !byId.keySet().equals(new HashSet<>(unique))) {
                throw new IllegalArgumentException("Ручная расстановка должна содержать все команды Кубка ровно по одному разу.");
            }
            return unique.stream().map(byId::get).toList();
        }
        List<Team> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    private void generateBracket(Competition competition, List<Team> teams) {
        int bracketSize = 1;
        while (bracketSize < teams.size()) bracketSize *= 2;
        int rounds = Integer.numberOfTrailingZeros(bracketSize);
        int byes = bracketSize - teams.size();
        List<CupTie> previous = new ArrayList<>();
        int cursor = 0;
        int firstTieCount = bracketSize / 2;
        for (int slot = 1; slot <= firstTieCount; slot++) {
            Team home = teams.get(cursor++);
            Team away = slot <= byes ? null : teams.get(cursor++);
            CupTie tie = baseTie(competition, roundCode(bracketSize, 1), 1, slot, legCount(competition, 1, rounds));
            tie.setHomeTeam(home);
            tie.setAwayTeam(away);
            if (away == null) {
                tie.setWinnerTeam(home);
                tie.setStatus("BYE");
            } else {
                tie.setStatus("READY");
            }
            previous.add(cupTieRepository.save(tie));
        }
        for (int round = 2; round <= rounds; round++) {
            List<CupTie> current = new ArrayList<>();
            for (int slot = 0; slot < previous.size() / 2; slot++) {
                CupTie left = previous.get(slot * 2);
                CupTie right = previous.get(slot * 2 + 1);
                CupTie tie = baseTie(competition, roundCode(bracketSize, round), round, slot + 1, legCount(competition, round, rounds));
                tie.setHomeSourceTie(left);
                tie.setAwaySourceTie(right);
                tie.setHomeSourceResult("WINNER");
                tie.setAwaySourceResult("WINNER");
                if (left.getWinnerTeam() != null) tie.setHomeTeam(left.getWinnerTeam());
                if (right.getWinnerTeam() != null) tie.setAwayTeam(right.getWinnerTeam());
                tie.setStatus(tie.getHomeTeam() != null && tie.getAwayTeam() != null ? "READY" : "PLANNED");
                current.add(cupTieRepository.save(tie));
            }
            previous = current;
        }
        if (competition.isThirdPlaceEnabled() && rounds >= 2) {
            List<CupTie> semifinals = cupTieRepository.findAllDetailedByCompetitionId(competition.getId()).stream()
                .filter(tie -> tie.getRoundOrder() == rounds - 1).toList();
            if (semifinals.size() == 2) {
                CupTie tie = baseTie(competition, "THIRD_PLACE", rounds + 1, 1, competition.getThirdPlaceLegs());
                tie.setHomeSourceTie(semifinals.get(0));
                tie.setAwaySourceTie(semifinals.get(1));
                tie.setHomeSourceResult("LOSER");
                tie.setAwaySourceResult("LOSER");
                cupTieRepository.save(tie);
            }
        }
    }

    private CupTie baseTie(Competition competition, String code, int round, int slot, int legs) {
        CupTie tie = new CupTie();
        tie.setCompetition(competition);
        tie.setRoundCode(code);
        tie.setRoundOrder(round);
        tie.setSlotOrder(slot);
        tie.setLegCount(legs);
        tie.setTitle(roundLabel(code) + ("FINAL".equals(code) || "THIRD_PLACE".equals(code) ? "" : " · пара " + slot));
        return tie;
    }

    private String roundCode(int bracketSize, int round) {
        int remaining = bracketSize >> round;
        if (remaining == 1) return "FINAL";
        if (remaining == 2) return "SEMIFINAL";
        if (remaining == 4) return "QUARTERFINAL";
        if (remaining == 8) return "ROUND_OF_16";
        return "ROUND_OF_" + (remaining * 2);
    }

    private String roundLabel(String code) {
        return switch (code) {
            case "FINAL" -> "Финал";
            case "SEMIFINAL" -> "1/2 финала";
            case "QUARTERFINAL" -> "1/4 финала";
            case "ROUND_OF_16" -> "1/8 финала";
            case "THIRD_PLACE" -> "Матч за 3 место";
            default -> code.startsWith("ROUND_OF_") ? "1/" + (Integer.parseInt(code.substring("ROUND_OF_".length())) / 2) + " финала" : "Раунд";
        };
    }

    private int legCount(Competition competition, int round, int totalRounds) {
        return round == totalRounds ? competition.getFinalLegs() : competition.getRegularTieLegs();
    }

    private void resetDraw(Competition competition) {
        cupTieRepository.deleteAllByCompetition_Id(competition.getId());
        competition.setDrawStatus("NOT_DRAWN");
        competitionRepository.save(competition);
    }

    private CompetitionData toData(Competition competition) {
        List<TeamData> teams = competitionTeamRepository.findAllDetailedByCompetitionId(competition.getId()).stream()
            .map(item -> new TeamData(item.getTeam().getId(), item.getTeam().getName(), item.getTeam().getShortName(), item.getSeedNumber())).toList();
        List<TieData> ties = competition.getType() == CompetitionType.CUP
            ? cupTieRepository.findAllDetailedByCompetitionId(competition.getId()).stream().map(this::toTieData).toList()
            : List.of();
        return new CompetitionData(competition.getId(), competition.getSeason().getId(), competition.getName(), competition.getType(), competition.getStatus(),
            competition.getRosterMode(), competition.getMaxRosterSize(), competition.getMatchRosterSize(), competition.getPlayersOnField(),
            competition.getRegularTieLegs(), competition.getFinalLegs(), competition.isThirdPlaceEnabled(), competition.getThirdPlaceLegs(),
            competition.isExtraTimeEnabled(), competition.getExtraTimeMinutes(), competition.isPenaltiesEnabled(), competition.getYellowCardsForSuspension(),
            competition.getYellowSuspensionMatches(), competition.getRedSuspensionMatches(), competition.getDrawStatus(), teams, ties);
    }

    private TieData toTieData(CupTie tie) {
        List<MatchData> matches = cupTieMatchRepository.findAllDetailedByTieId(tie.getId()).stream()
            .map(item -> new MatchData(item.getMatch().getId(), item.getLegNumber(), item.getMatch().getKickoffAt(),
                teamData(item.getMatch().getHomeTeam()), teamData(item.getMatch().getAwayTeam()),
                item.getMatch().getProtocol() == null ? null : item.getMatch().getProtocol().getStatus().name(),
                item.getMatch().getProtocol() == null ? null : item.getMatch().getProtocol().getHomeScore(),
                item.getMatch().getProtocol() == null ? null : item.getMatch().getProtocol().getAwayScore()))
            .toList();
        return new TieData(tie.getId(), tie.getRoundCode(), tie.getRoundOrder(), tie.getSlotOrder(), tie.getLegCount(), tie.getTitle(), tie.getStatus(),
            teamData(tie.getHomeTeam()), teamData(tie.getAwayTeam()), teamData(tie.getWinnerTeam()),
            tie.getHomeSourceTie() == null ? null : tie.getHomeSourceTie().getId(), tie.getAwaySourceTie() == null ? null : tie.getAwaySourceTie().getId(),
            tie.getHomeSourceResult(), tie.getAwaySourceResult(), tie.getAggregateHomeScore(), tie.getAggregateAwayScore(),
            tie.getHomePenaltyScore(), tie.getAwayPenaltyScore(), matches);
    }

    private TeamData teamData(Team team) { return team == null ? null : new TeamData(team.getId(), team.getName(), team.getShortName(), null); }
    private Season getSeason(Long id) { return seasonRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден.")); }
    private Competition getCup(Long seasonId, Long id) {
        Competition competition = competitionRepository.findDetailedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено."));
        if (!competition.getSeason().getId().equals(seasonId) || competition.getType() != CompetitionType.CUP || !competition.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Кубок не найден.");
        }
        return competition;
    }
    private int legs(Integer value) { return value == null ? 1 : Math.max(1, Math.min(2, value)); }
    private int positive(Integer value, int fallback, String label) { int normalized = value == null ? fallback : value; if (normalized <= 0) throw new IllegalArgumentException(label + " должно быть больше нуля."); return normalized; }

    public record CompetitionSettings(String name, CompetitionRosterMode rosterMode, Integer maxRosterSize, Integer matchRosterSize,
        Integer playersOnField, Integer regularTieLegs, Integer finalLegs, boolean thirdPlaceEnabled, Integer thirdPlaceLegs,
        boolean extraTimeEnabled, Integer extraTimeMinutes, boolean penaltiesEnabled, int yellowCardsForSuspension,
        Integer yellowSuspensionMatches, Integer redSuspensionMatches, List<Long> teamIds) {}
    public record CompetitionData(Long id, Long seasonId, String name, CompetitionType type, String status, CompetitionRosterMode rosterMode,
        Integer maxRosterSize, Integer matchRosterSize, int playersOnField, int regularTieLegs, int finalLegs, boolean thirdPlaceEnabled,
        int thirdPlaceLegs, boolean extraTimeEnabled, int extraTimeMinutes, boolean penaltiesEnabled, int yellowCardsForSuspension,
        int yellowSuspensionMatches, int redSuspensionMatches, String drawStatus, List<TeamData> teams, List<TieData> ties) {}
    public record TeamData(Long id, String name, String shortName, Integer seedNumber) {}
    public record TieData(Long id, String roundCode, int roundOrder, int slotOrder, int legCount, String title, String status,
        TeamData homeTeam, TeamData awayTeam, TeamData winnerTeam, Long homeSourceTieId, Long awaySourceTieId,
        String homeSourceResult, String awaySourceResult, Integer aggregateHomeScore, Integer aggregateAwayScore,
        Integer homePenaltyScore, Integer awayPenaltyScore, List<MatchData> matches) {}
    public record MatchData(Long id, int legNumber, OffsetDateTime kickoffAt, TeamData homeTeam, TeamData awayTeam,
        String protocolStatus, Integer homeScore, Integer awayScore) {}
    public record RosterPlayerData(Long playerId, String playerName, Long teamId, String teamName) {}
}
