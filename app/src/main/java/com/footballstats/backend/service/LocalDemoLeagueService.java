package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.DemoDataset;
import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.MatchLineup;
import com.footballstats.backend.domain.MatchLineupPlayer;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Referee;
import com.footballstats.backend.domain.Role;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.SeasonPlayoffTie;
import com.footballstats.backend.domain.SeasonPlayoffTieMatch;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.SeasonTransferRequest;
import com.footballstats.backend.domain.SeasonTransferStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.domain.UserRole;
import com.footballstats.backend.domain.UserTeamScope;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.DemoDatasetRepository;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.MatchLineupPlayerRepository;
import com.footballstats.backend.repository.MatchLineupRepository;
import com.footballstats.backend.repository.MatchProtocolRepository;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.RefereeRepository;
import com.footballstats.backend.repository.RoleRepository;
import com.footballstats.backend.repository.SeasonPlayerRepository;
import com.footballstats.backend.repository.SeasonPlayoffTieMatchRepository;
import com.footballstats.backend.repository.SeasonPlayoffTieRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonTransferRequestRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import com.footballstats.backend.repository.TourRepository;
import com.footballstats.backend.repository.UserRoleRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Profile({"local", "test"})
@ConditionalOnProperty(name = "demo.tools.enabled", havingValue = "true")
public class LocalDemoLeagueService {

    private static final String DATASET_CODE = "local-product-league-v1";
    private static final String DATASET_NAME = "Локальная продуктовая лига";
    private static final String DEMO_PASSWORD = "Demo2026!";
    private static final int PLAYERS_PER_TEAM = 15;
    private static final int COMPLETED_MATCHES = 20;

    private static final List<TeamSeed> TEAM_SEEDS = List.of(
        new TeamSeed("Атлетик Богородск", "Атлетик", "Богородск"),
        new TeamSeed("Волна Дуденево", "Волна", "Дуденево"),
        new TeamSeed("Заря Кудьма", "Заря", "Кудьма"),
        new TeamSeed("Искра Окский", "Искра", "Окский"),
        new TeamSeed("Метеор Новинки", "Метеор", "Новинки"),
        new TeamSeed("Олимп Афанасьево", "Олимп", "Афанасьево"),
        new TeamSeed("Рубин Шапкино", "Рубин", "Шапкино"),
        new TeamSeed("Сокол Каменки", "Сокол", "Каменки"),
        new TeamSeed("Спарта Подвязье", "Спарта", "Подвязье"),
        new TeamSeed("Факел Лакша", "Факел", "Лакша")
    );

    private static final List<String> FIRST_NAMES = List.of(
        "Александр", "Алексей", "Андрей", "Антон", "Артём",
        "Виктор", "Даниил", "Дмитрий", "Егор", "Иван",
        "Илья", "Кирилл", "Максим", "Михаил", "Никита"
    );

    private static final List<String> LAST_NAMES = List.of(
        "Белов", "Волков", "Громов", "Жуков", "Крылов",
        "Лебедев", "Морозов", "Орлов", "Панов", "Романов",
        "Соколов", "Титов", "Фомин", "Чернов", "Яковлев"
    );

    private static final List<String> REFEREE_NAMES = List.of(
        "Алексей Воронцов", "Борис Данилов", "Виктор Ершов", "Георгий Карпов",
        "Денис Лапшин", "Игорь Мельников", "Роман Нестеров", "Сергей Осипов"
    );

    private final DemoDatasetRepository demoDatasetRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final RefereeRepository refereeRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonPlayerRepository seasonPlayerRepository;
    private final TourRepository tourRepository;
    private final TourMatchRepository tourMatchRepository;
    private final MatchProtocolRepository matchProtocolRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final MatchLineupPlayerRepository matchLineupPlayerRepository;
    private final MatchEventRepository matchEventRepository;
    private final SeasonTransferRequestRepository transferRequestRepository;
    private final SeasonPlayoffTieRepository playoffTieRepository;
    private final SeasonPlayoffTieMatchRepository playoffTieMatchRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserTeamScopeRepository userTeamScopeRepository;
    private final SeasonService seasonService;
    private final CompetitionService competitionService;
    private final TourService tourService;
    private final SeasonStandingsService standingsService;
    private final SeasonPlayoffService playoffService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public LocalDemoLeagueService(
        DemoDatasetRepository demoDatasetRepository,
        TeamRepository teamRepository,
        PlayerRepository playerRepository,
        PlayerTeamRepository playerTeamRepository,
        RefereeRepository refereeRepository,
        SeasonRepository seasonRepository,
        SeasonPlayerRepository seasonPlayerRepository,
        TourRepository tourRepository,
        TourMatchRepository tourMatchRepository,
        MatchProtocolRepository matchProtocolRepository,
        MatchLineupRepository matchLineupRepository,
        MatchLineupPlayerRepository matchLineupPlayerRepository,
        MatchEventRepository matchEventRepository,
        SeasonTransferRequestRepository transferRequestRepository,
        SeasonPlayoffTieRepository playoffTieRepository,
        SeasonPlayoffTieMatchRepository playoffTieMatchRepository,
        AppUserRepository appUserRepository,
        RoleRepository roleRepository,
        UserRoleRepository userRoleRepository,
        UserTeamScopeRepository userTeamScopeRepository,
        SeasonService seasonService,
        CompetitionService competitionService,
        TourService tourService,
        SeasonStandingsService standingsService,
        SeasonPlayoffService playoffService,
        BCryptPasswordEncoder passwordEncoder,
        JdbcTemplate jdbcTemplate
    ) {
        this.demoDatasetRepository = demoDatasetRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.refereeRepository = refereeRepository;
        this.seasonRepository = seasonRepository;
        this.seasonPlayerRepository = seasonPlayerRepository;
        this.tourRepository = tourRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.matchProtocolRepository = matchProtocolRepository;
        this.matchLineupRepository = matchLineupRepository;
        this.matchLineupPlayerRepository = matchLineupPlayerRepository;
        this.matchEventRepository = matchEventRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.playoffTieRepository = playoffTieRepository;
        this.playoffTieMatchRepository = playoffTieMatchRepository;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userTeamScopeRepository = userTeamScopeRepository;
        this.seasonService = seasonService;
        this.competitionService = competitionService;
        this.tourService = tourService;
        this.standingsService = standingsService;
        this.playoffService = playoffService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public DemoLeagueStatus getStatus() {
        return demoDatasetRepository.findByCode(DATASET_CODE)
            .map(this::buildStatus)
            .orElseGet(DemoLeagueStatus::empty);
    }

    @Transactional
    public DemoLeagueStatus createBase(Long actorUserId) {
        if (demoDatasetRepository.findByCode(DATASET_CODE).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Демо-лига уже существует. Сначала удалите текущий набор.");
        }

        DemoDataset dataset = new DemoDataset();
        dataset.setCode(DATASET_CODE);
        dataset.setName(DATASET_NAME);
        dataset.setStage("BASE");
        dataset.setCreatedByUserId(actorUserId);
        dataset = demoDatasetRepository.save(dataset);

        List<Team> teams = createTeams(dataset.getId(), actorUserId);
        Map<Long, List<Player>> playersByTeam = createPlayers(dataset.getId(), teams, actorUserId);
        List<Referee> referees = createReferees(dataset.getId(), actorUserId);
        createDemoAccounts(dataset.getId(), teams, actorUserId);

        Season season = seasonService.createSeason(
            "Демо-лига Богородского округа 2026",
            2,
            false,
            null,
            LocalDate.now().plusDays(21),
            SeasonStatus.ACTIVE,
            22,
            11,
            LocalDate.now().minusDays(3),
            LocalDate.now().plusDays(30),
            false,
            List.of("POINTS", "GOAL_DIFFERENCE", "GOALS_FOR", "WINS", "ALPHABETICAL"),
            referees.stream().map(Referee::getId).toList(),
            4,
            1,
            1,
            actorUserId
        );
        track(dataset.getId(), "SEASON", season.getId());
        seasonService.replaceSeasonTeams(season.getId(), teams.stream().map(Team::getId).toList(), actorUserId);
        competitionService.createChampionship(season.getId(), "Чемпионат", actorUserId);
        seasonService.initializeChampionship(season.getId(), actorUserId);

        for (Team team : teams) {
            for (Player player : playersByTeam.get(team.getId())) {
                SeasonPlayer assignment = new SeasonPlayer();
                assignment.setSeason(season);
                assignment.setTeam(team);
                assignment.setPlayer(player);
                assignment.setCreatedByUserId(actorUserId);
                assignment.setUpdatedByUserId(actorUserId);
                seasonPlayerRepository.save(assignment);
            }
        }

        standingsService.recalculateSeasonStandings(season.getId(), actorUserId);
        dataset.setSeasonId(season.getId());
        dataset.setUpdatedAt(OffsetDateTime.now());
        demoDatasetRepository.save(dataset);
        return buildStatus(dataset);
    }

    @Transactional
    public DemoLeagueStatus createSchedule(Long actorUserId) {
        DemoDataset dataset = requireDataset();
        requireStage(dataset, "BASE");
        Season season = requireSeason(dataset);
        List<Team> teams = trackedTeams(dataset.getId());
        List<Tour> tours = tourRepository.findAllActiveDetailedBySeasonId(season.getId());
        List<RoundPairing> pairings = buildDoubleRoundRobin(teams);

        if (tours.size() != pairings.size()) {
            throw new IllegalStateException("Структура сезона не совпадает с ожидаемыми 18 турами.");
        }

        OffsetDateTime firstKickoff = LocalDate.now()
            .plusDays(3)
            .atTime(18, 30)
            .atOffset(ZoneOffset.ofHours(3));
        for (int roundIndex = 0; roundIndex < pairings.size(); roundIndex += 1) {
            Tour tour = tours.get(roundIndex);
            List<TeamPair> round = pairings.get(roundIndex).matches();
            for (int matchIndex = 0; matchIndex < round.size(); matchIndex += 1) {
                TeamPair pair = round.get(matchIndex);
                tourService.createMatch(
                    tour.getId(),
                    pair.home().getId(),
                    pair.away().getId(),
                    firstKickoff.plusWeeks(roundIndex).plusMinutes(matchIndex * 110L),
                    actorUserId
                );
            }
            tourService.publishTour(tour.getId(), actorUserId);
        }

        updateStage(dataset, "SCHEDULE");
        return buildStatus(dataset);
    }

    @Transactional
    public DemoLeagueStatus addResults(Long actorUserId) {
        DemoDataset dataset = requireDataset();
        requireStage(dataset, "SCHEDULE");
        Season season = requireSeason(dataset);
        List<TourMatch> matches = tourMatchRepository.findAllActiveDetailedBySeasonId(season.getId());
        Map<Long, List<Player>> playersByTeam = loadSeasonPlayersByTeam(season.getId());
        List<Referee> referees = trackedReferees(dataset.getId());

        for (int index = 0; index < Math.min(COMPLETED_MATCHES, matches.size()); index += 1) {
            TourMatch match = matches.get(index);
            List<Player> homePlayers = playersByTeam.get(match.getHomeTeam().getId());
            List<Player> awayPlayers = playersByTeam.get(match.getAwayTeam().getId());
            createLineup(match, match.getHomeTeam(), homePlayers, actorUserId);
            createLineup(match, match.getAwayTeam(), awayPlayers, actorUserId);

            int homeScore = (index * 3 + 1) % 5;
            int awayScore = (index * 2 + 2) % 4;
            MatchProtocol protocol = matchProtocolRepository.findByMatch_Id(match.getId())
                .orElseThrow(() -> new IllegalStateException("Протокол матча не создан."));
            protocol.setStatus(MatchProtocolStatus.VERIFIED);
            protocol.setHomeScore(homeScore);
            protocol.setAwayScore(awayScore);
            protocol.setBestPlayer(homeScore >= awayScore ? homePlayers.get(2) : awayPlayers.get(2));
            protocol.setChiefReferee(referees.get(index % referees.size()));
            protocol.setAssistantRefereeOne(referees.get((index + 1) % referees.size()));
            protocol.setAssistantRefereeTwo(referees.get((index + 2) % referees.size()));
            protocol.setNotes("Проверенный протокол демо-матча.");
            protocol.setStartedAt(match.getKickoffAt());
            protocol.setFinishedAt(match.getKickoffAt().plusMinutes(100));
            protocol.setUpdatedByUserId(actorUserId);
            protocol.setUpdatedAt(OffsetDateTime.now());
            matchProtocolRepository.save(protocol);

            addGoalEvents(match, match.getHomeTeam(), homePlayers, homeScore, actorUserId, 0);
            addGoalEvents(match, match.getAwayTeam(), awayPlayers, awayScore, actorUserId, homeScore);
        }

        standingsService.recalculateSeasonStandings(season.getId(), actorUserId);
        updateStage(dataset, "RESULTS");
        return buildStatus(dataset);
    }

    @Transactional
    public DemoLeagueStatus prepareTransfers(Long actorUserId) {
        DemoDataset dataset = requireDataset();
        requireStage(dataset, "RESULTS");
        Season season = requireSeason(dataset);
        List<Team> teams = trackedTeams(dataset.getId());
        Map<Long, List<Player>> playersByTeam = loadSeasonPlayersByTeam(season.getId());
        AppUser requestingUser = trackedUsers(dataset.getId()).stream()
            .filter(user -> "demo.rep2@local.test".equals(user.getEmail()))
            .findFirst()
            .orElseThrow();

        season.setTransferWindowStartDate(LocalDate.now().minusDays(3));
        season.setTransferWindowEndDate(LocalDate.now().plusDays(30));
        season.setStatus(SeasonStatus.ACTIVE);
        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(OffsetDateTime.now());
        seasonRepository.save(season);

        Team source = teams.get(0);
        Team target = teams.get(1);
        Player candidate = playersByTeam.get(source.getId()).get(4);
        SeasonTransferRequest request = new SeasonTransferRequest();
        request.setSeason(season);
        request.setPlayer(candidate);
        request.setFromTeam(source);
        request.setToTeam(target);
        request.setRequestedByUserId(requestingUser.getId());
        request.setRequestComment("Демо-заявка: проверить согласование перехода между командами.");
        request.setStatus(SeasonTransferStatus.PENDING);
        transferRequestRepository.save(request);

        updateStage(dataset, "TRANSFERS");
        return buildStatus(dataset);
    }

    @Transactional
    public DemoLeagueStatus preparePlayoffs(Long actorUserId) {
        DemoDataset dataset = requireDataset();
        requireStage(dataset, "TRANSFERS");
        Season season = requireSeason(dataset);
        List<Referee> referees = trackedReferees(dataset.getId());
        Map<Long, List<Player>> playersByTeam = loadSeasonPlayersByTeam(season.getId());

        season.setPlayoffEnabled(true);
        season.setPlayoffTeamCount(8);
        season.setUpdatedByUserId(actorUserId);
        season.setUpdatedAt(OffsetDateTime.now());
        seasonRepository.save(season);
        playoffService.syncSeasonPlayoffConfig(season.getId(), true, 8, true, actorUserId);

        List<TourMatch> regularMatches = tourMatchRepository.findAllActiveDetailedBySeasonId(season.getId()).stream()
            .filter(match -> SeasonStructureService.REGULAR_STAGE.equalsIgnoreCase(match.getTour().getStageType()))
            .toList();
        for (int index = 0; index < regularMatches.size(); index += 1) {
            TourMatch match = regularMatches.get(index);
            MatchProtocol protocol = matchProtocolRepository.findByMatch_Id(match.getId())
                .orElseThrow(() -> new IllegalStateException("Протокол матча не создан."));
            if (protocol.getStatus() == MatchProtocolStatus.VERIFIED) {
                continue;
            }
            int homeScore = (index * 2 + 1) % 4;
            int awayScore = (index * 3) % 3;
            completeProtocol(match, protocol, homeScore, awayScore, referees, actorUserId, index);
        }

        standingsService.recalculateSeasonStandings(season.getId(), actorUserId);
        playoffService.completeRegularSeason(season.getId(), actorUserId);

        Map<String, Tour> playoffTours = new LinkedHashMap<>();
        for (Tour tour : tourRepository.findAllBySeason_IdAndStageTypeOrderBySortOrderAscIdAsc(
            season.getId(),
            SeasonPlayoffService.PLAYOFF_STAGE
        )) {
            playoffTours.put(playoffRoundCode(tour.getName()), tour);
        }

        List<SeasonPlayoffTie> ties = playoffTieRepository.findAllDetailedBySeasonId(season.getId());
        Map<String, List<SeasonPlayoffTie>> tiesByRound = new LinkedHashMap<>();
        for (SeasonPlayoffTie tie : ties) {
            tiesByRound.computeIfAbsent(tie.getRoundCode(), ignored -> new ArrayList<>()).add(tie);
        }

        OffsetDateTime playoffKickoff = LocalDate.now()
            .plusWeeks(20)
            .atTime(18, 30)
            .atOffset(ZoneOffset.ofHours(3));

        List<SeasonPlayoffTie> quarterfinals = tiesByRound.getOrDefault("QUARTERFINAL", List.of());
        for (int index = 0; index < quarterfinals.size(); index += 1) {
            SeasonPlayoffTie tie = quarterfinals.get(index);
            int homeScore = 2 + (index % 2);
            int awayScore = index % 2;
            completePlayoffTie(
                tie,
                requirePlayoffTour(playoffTours, "QUARTERFINAL"),
                tie.getHomeTeam(),
                tie.getAwayTeam(),
                homeScore,
                awayScore,
                playoffKickoff.plusDays(index),
                playersByTeam,
                referees,
                actorUserId,
                index
            );
        }

        List<SeasonPlayoffTie> semifinals = tiesByRound.getOrDefault("SEMIFINAL", List.of());
        for (int index = 0; index < semifinals.size(); index += 1) {
            SeasonPlayoffTie tie = semifinals.get(index);
            Team home = sourceParticipant(tie.getHomeSourceTie(), tie.getHomeSourceResult());
            Team away = sourceParticipant(tie.getAwaySourceTie(), tie.getAwaySourceResult());
            tie.setHomeTeam(home);
            tie.setAwayTeam(away);
            completePlayoffTie(
                tie,
                requirePlayoffTour(playoffTours, "SEMIFINAL"),
                home,
                away,
                index == 0 ? 2 : 1,
                index == 0 ? 0 : 2,
                playoffKickoff.plusWeeks(2).plusDays(index),
                playersByTeam,
                referees,
                actorUserId,
                10 + index
            );
        }

        for (String roundCode : List.of("FINAL", "THIRD_PLACE")) {
            for (SeasonPlayoffTie tie : tiesByRound.getOrDefault(roundCode, List.of())) {
                Team home = sourceParticipant(tie.getHomeSourceTie(), tie.getHomeSourceResult());
                Team away = sourceParticipant(tie.getAwaySourceTie(), tie.getAwaySourceResult());
                tie.setHomeTeam(home);
                tie.setAwayTeam(away);
                tie.setStatus("READY");
                tie.setUpdatedAt(OffsetDateTime.now());
                playoffTieRepository.save(tie);
                createPlayoffMatch(
                    tie,
                    requirePlayoffTour(playoffTours, roundCode),
                    home,
                    away,
                    playoffKickoff.plusWeeks(roundCode.equals("FINAL") ? 4 : 3),
                    actorUserId
                );
            }
        }

        for (Tour tour : playoffTours.values()) {
            if (tourMatchRepository.existsByTour_IdAndActiveTrue(tour.getId())) {
                tourService.publishTour(tour.getId(), actorUserId);
            }
        }

        updateStage(dataset, "PLAYOFF");
        return buildStatus(dataset);
    }

    @Transactional
    public DemoLeagueStatus reset() {
        DemoDataset dataset = requireDataset();
        Long datasetId = dataset.getId();
        Long seasonId = dataset.getSeasonId();

        if (seasonId != null) {
            jdbcTemplate.update("DELETE FROM work.w_season_transfer_request WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_application_player WHERE application_id IN (SELECT id FROM work.w_season_application WHERE season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_application WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_match_protocol_export_snapshot WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_match_event WHERE match_id IN (SELECT tm.id FROM work.w_tour_match tm JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_match_lineup_player WHERE lineup_id IN (SELECT ml.id FROM work.w_match_lineup ml JOIN work.w_tour_match tm ON tm.id = ml.match_id JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_match_lineup WHERE match_id IN (SELECT tm.id FROM work.w_tour_match tm JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_playoff_tie_match WHERE match_id IN (SELECT tm.id FROM work.w_tour_match tm JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_cup_tie_match WHERE match_id IN (SELECT tm.id FROM work.w_tour_match tm JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_match_protocol WHERE match_id IN (SELECT tm.id FROM work.w_tour_match tm JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_player WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_team WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_tour_match WHERE tour_id IN (SELECT id FROM work.w_tour WHERE season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_tour WHERE season_id = ?", seasonId);
            jdbcTemplate.update("UPDATE work.w_cup_tie SET home_source_tie_id = NULL, away_source_tie_id = NULL WHERE competition_id IN (SELECT id FROM work.w_competition WHERE season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_cup_tie WHERE competition_id IN (SELECT id FROM work.w_competition WHERE season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_competition_roster_player WHERE competition_id IN (SELECT id FROM work.w_competition WHERE season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_competition_team WHERE competition_id IN (SELECT id FROM work.w_competition WHERE season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_competition WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_referee WHERE season_id = ?", seasonId);
            jdbcTemplate.update(
                """
                    UPDATE work.w_season_playoff_tie
                    SET home_source_tie_id = NULL, away_source_tie_id = NULL
                    WHERE bracket_id IN (
                        SELECT id FROM work.w_season_playoff_bracket WHERE season_id = ?
                    )
                    """,
                seasonId
            );
            jdbcTemplate.update("DELETE FROM work.w_season_playoff_tie WHERE bracket_id IN (SELECT id FROM work.w_season_playoff_bracket WHERE season_id = ?)", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_playoff_bracket WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_playoff_config WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_standings_row WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season_standings_config WHERE season_id = ?", seasonId);
            jdbcTemplate.update("DELETE FROM work.w_season WHERE id = ?", seasonId);
        }

        deleteTrackedUsers(datasetId);
        jdbcTemplate.update(
            "DELETE FROM work.w_player_team WHERE player_id IN (SELECT object_id FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = 'PLAYER')",
            datasetId
        );
        deleteTrackedEntities(datasetId, "PLAYER", "work.w_player");
        deleteTrackedEntities(datasetId, "REFEREE", "work.w_referee");
        deleteTrackedEntities(datasetId, "TEAM", "work.w_team");
        demoDatasetRepository.delete(dataset);
        return DemoLeagueStatus.empty();
    }

    private List<Team> createTeams(Long datasetId, Long actorUserId) {
        List<Team> teams = new ArrayList<>();
        for (TeamSeed seed : TEAM_SEEDS) {
            Team team = new Team();
            team.setName(seed.name());
            team.setShortName(seed.shortName());
            team.setCity(seed.city());
            team.setCreatedByUserId(actorUserId);
            team.setUpdatedByUserId(actorUserId);
            team.setUpdatedAt(OffsetDateTime.now());
            team = teamRepository.save(team);
            track(datasetId, "TEAM", team.getId());
            teams.add(team);
        }
        return teams;
    }

    private Map<Long, List<Player>> createPlayers(Long datasetId, List<Team> teams, Long actorUserId) {
        Map<Long, List<Player>> playersByTeam = new LinkedHashMap<>();
        LocalDate joinedAt = LocalDate.now().minusYears(1);
        for (int teamIndex = 0; teamIndex < teams.size(); teamIndex += 1) {
            Team team = teams.get(teamIndex);
            List<Player> players = new ArrayList<>();
            for (int playerIndex = 0; playerIndex < PLAYERS_PER_TEAM; playerIndex += 1) {
                Player player = new Player();
                player.setFullName(playerName(teamIndex, playerIndex, team.getShortName()));
                player.setBirthDate(LocalDate.of(1990 + ((teamIndex + playerIndex) % 15), 1 + (playerIndex % 12), 1 + ((teamIndex * 2 + playerIndex) % 27)));
                player.setResidence(team.getCity());
                player.setGoalkeeper(playerIndex < 2);
                player.setCreatedByUserId(actorUserId);
                player.setUpdatedByUserId(actorUserId);
                player.setUpdatedAt(OffsetDateTime.now());
                player = playerRepository.save(player);
                track(datasetId, "PLAYER", player.getId());

                PlayerTeam membership = new PlayerTeam();
                membership.setPlayer(player);
                membership.setTeam(team);
                membership.setValidFrom(joinedAt);
                playerTeamRepository.save(membership);
                players.add(player);
            }
            playersByTeam.put(team.getId(), players);
        }
        return playersByTeam;
    }

    private List<Referee> createReferees(Long datasetId, Long actorUserId) {
        List<Referee> referees = new ArrayList<>();
        for (int index = 0; index < REFEREE_NAMES.size(); index += 1) {
            Referee referee = new Referee();
            referee.setFullName(REFEREE_NAMES.get(index) + " (демо)");
            referee.setCity(index % 2 == 0 ? "Богородск" : "Нижний Новгород");
            referee.setBirthDate(LocalDate.of(1980 + index, 2 + index, 5 + index));
            referee.setCreatedByUserId(actorUserId);
            referee.setUpdatedByUserId(actorUserId);
            referee.setUpdatedAt(OffsetDateTime.now());
            referee = refereeRepository.save(referee);
            track(datasetId, "REFEREE", referee.getId());
            referees.add(referee);
        }
        return referees;
    }

    private void createDemoAccounts(Long datasetId, List<Team> teams, Long actorUserId) {
        AppUser fan = createUser(datasetId, "demo.fan@local.test", "Демо-болельщик");
        assignRole(fan, RoleCode.USER, actorUserId);

        AppUser firstRep = createUser(datasetId, "demo.rep1@local.test", "Представитель «Атлетик»");
        assignRole(firstRep, RoleCode.TEAM_REP, actorUserId);
        assignTeamScope(firstRep, teams.get(0), actorUserId);

        AppUser secondRep = createUser(datasetId, "demo.rep2@local.test", "Представитель «Волна»");
        assignRole(secondRep, RoleCode.TEAM_REP, actorUserId);
        assignTeamScope(secondRep, teams.get(1), actorUserId);

        AppUser referee = createUser(datasetId, "demo.referee@local.test", "Демо-рефери");
        assignRole(referee, RoleCode.REFEREE, actorUserId);
    }

    private AppUser createUser(Long datasetId, String email, String name) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(OffsetDateTime.now());
        user = appUserRepository.save(user);
        track(datasetId, "USER", user.getId());
        return user;
    }

    private void assignRole(AppUser user, RoleCode roleCode, Long actorUserId) {
        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new IllegalStateException("Роль " + roleCode + " не найдена."));
        UserRole assignment = new UserRole();
        assignment.setUser(user);
        assignment.setRole(role);
        assignment.setGrantedByUserId(actorUserId);
        userRoleRepository.save(assignment);
    }

    private void assignTeamScope(AppUser user, Team team, Long actorUserId) {
        UserTeamScope scope = new UserTeamScope();
        scope.setUser(user);
        scope.setTeam(team);
        scope.setGrantedByUserId(actorUserId);
        scope.setGrantedAt(OffsetDateTime.now());
        scope.setValidFrom(OffsetDateTime.now().minusDays(1));
        scope.setCanEditRoster(true);
        scope.setCanEditApplication(true);
        userTeamScopeRepository.save(scope);
    }

    private void completePlayoffTie(
        SeasonPlayoffTie tie,
        Tour tour,
        Team home,
        Team away,
        int homeScore,
        int awayScore,
        OffsetDateTime kickoffAt,
        Map<Long, List<Player>> playersByTeam,
        List<Referee> referees,
        Long actorUserId,
        int seed
    ) {
        TourMatch match = createPlayoffMatch(tie, tour, home, away, kickoffAt, actorUserId);
        createLineup(match, home, playersByTeam.get(home.getId()), actorUserId);
        createLineup(match, away, playersByTeam.get(away.getId()), actorUserId);
        MatchProtocol protocol = matchProtocolRepository.findByMatch_Id(match.getId())
            .orElseThrow(() -> new IllegalStateException("Протокол матча плей-офф не создан."));
        completeProtocol(match, protocol, homeScore, awayScore, referees, actorUserId, seed);

        tie.setAggregateHomeScore(homeScore);
        tie.setAggregateAwayScore(awayScore);
        tie.setWinnerTeam(homeScore > awayScore ? home : away);
        tie.setStatus("COMPLETED");
        tie.setUpdatedAt(OffsetDateTime.now());
        playoffTieRepository.save(tie);
    }

    private TourMatch createPlayoffMatch(
        SeasonPlayoffTie tie,
        Tour tour,
        Team home,
        Team away,
        OffsetDateTime kickoffAt,
        Long actorUserId
    ) {
        TourMatch match = tourService.createMatch(tour.getId(), home.getId(), away.getId(), kickoffAt, actorUserId);
        SeasonPlayoffTieMatch link = new SeasonPlayoffTieMatch();
        link.setTie(tie);
        link.setLegNumber(1);
        link.setMatch(match);
        link.setUpdatedAt(OffsetDateTime.now());
        playoffTieMatchRepository.save(link);
        return match;
    }

    private void completeProtocol(
        TourMatch match,
        MatchProtocol protocol,
        int homeScore,
        int awayScore,
        List<Referee> referees,
        Long actorUserId,
        int seed
    ) {
        protocol.setStatus(MatchProtocolStatus.VERIFIED);
        protocol.setHomeScore(homeScore);
        protocol.setAwayScore(awayScore);
        protocol.setChiefReferee(referees.get(seed % referees.size()));
        protocol.setAssistantRefereeOne(referees.get((seed + 1) % referees.size()));
        protocol.setAssistantRefereeTwo(referees.get((seed + 2) % referees.size()));
        protocol.setStartedAt(match.getKickoffAt());
        protocol.setFinishedAt(match.getKickoffAt().plusMinutes(100));
        protocol.setNotes("Проверенный протокол демо-матча.");
        protocol.setUpdatedByUserId(actorUserId);
        protocol.setUpdatedAt(OffsetDateTime.now());
        matchProtocolRepository.save(protocol);
    }

    private Team sourceParticipant(SeasonPlayoffTie sourceTie, String sourceResult) {
        if (sourceTie == null || sourceTie.getWinnerTeam() == null) {
            throw new IllegalStateException("Предыдущая пара плей-офф еще не определила участника.");
        }
        if (!"LOSER".equalsIgnoreCase(sourceResult)) {
            return sourceTie.getWinnerTeam();
        }
        if (sourceTie.getHomeTeam().getId().equals(sourceTie.getWinnerTeam().getId())) {
            return sourceTie.getAwayTeam();
        }
        return sourceTie.getHomeTeam();
    }

    private Tour requirePlayoffTour(Map<String, Tour> tours, String roundCode) {
        Tour tour = tours.get(roundCode);
        if (tour == null) {
            throw new IllegalStateException("Тур плей-офф " + roundCode + " не найден.");
        }
        return tour;
    }

    private String playoffRoundCode(String tourName) {
        return switch (tourName) {
            case "1/4 финала" -> "QUARTERFINAL";
            case "1/2 финала" -> "SEMIFINAL";
            case "Финал" -> "FINAL";
            case "Матч за 3 место" -> "THIRD_PLACE";
            default -> tourName;
        };
    }

    private void createLineup(TourMatch match, Team team, List<Player> players, Long actorUserId) {
        if (matchLineupRepository.existsByMatch_IdAndTeam_Id(match.getId(), team.getId())) {
            return;
        }
        MatchLineup lineup = new MatchLineup();
        lineup.setMatch(match);
        lineup.setTeam(team);
        lineup.setSubmittedByUserId(actorUserId);
        lineup.setUpdatedByUserId(actorUserId);
        lineup.setSubmittedAt(OffsetDateTime.now());
        lineup = matchLineupRepository.save(lineup);

        for (int index = 0; index < Math.min(12, players.size()); index += 1) {
            MatchLineupPlayer item = new MatchLineupPlayer();
            item.setLineup(lineup);
            item.setPlayer(players.get(index));
            item.setSortOrder(index + 1);
            item.setStarter(index < match.getTour().getSeason().getPlayersOnField());
            item.setCreatedByUserId(actorUserId);
            item.setUpdatedByUserId(actorUserId);
            matchLineupPlayerRepository.save(item);
        }
    }

    private void addGoalEvents(
        TourMatch match,
        Team team,
        List<Player> players,
        int goals,
        Long actorUserId,
        int sortOffset
    ) {
        for (int index = 0; index < goals; index += 1) {
            MatchEvent event = new MatchEvent();
            event.setMatch(match);
            event.setTeam(team);
            event.setPlayer(players.get(2 + (index % Math.max(players.size() - 2, 1))));
            event.setEventType(index == 2 ? MatchEventType.PENALTY_GOAL : MatchEventType.GOAL);
            event.setMinute(12 + index * 19 + sortOffset * 3);
            event.setSortOrder(sortOffset + index + 1);
            event.setCreatedByUserId(actorUserId);
            event.setUpdatedByUserId(actorUserId);
            matchEventRepository.save(event);
        }
    }

    private List<RoundPairing> buildDoubleRoundRobin(List<Team> sourceTeams) {
        List<Team> rotation = new ArrayList<>(sourceTeams);
        List<RoundPairing> firstRound = new ArrayList<>();
        int rounds = rotation.size() - 1;
        for (int roundIndex = 0; roundIndex < rounds; roundIndex += 1) {
            List<TeamPair> matches = new ArrayList<>();
            for (int pairIndex = 0; pairIndex < rotation.size() / 2; pairIndex += 1) {
                Team left = rotation.get(pairIndex);
                Team right = rotation.get(rotation.size() - 1 - pairIndex);
                boolean reverse = (roundIndex + pairIndex) % 2 == 1;
                matches.add(reverse ? new TeamPair(right, left) : new TeamPair(left, right));
            }
            firstRound.add(new RoundPairing(matches));
            Team last = rotation.remove(rotation.size() - 1);
            rotation.add(1, last);
        }

        List<RoundPairing> allRounds = new ArrayList<>(firstRound);
        for (RoundPairing round : firstRound) {
            allRounds.add(new RoundPairing(round.matches().stream()
                .map(pair -> new TeamPair(pair.away(), pair.home()))
                .toList()));
        }
        return allRounds;
    }

    private Map<Long, List<Player>> loadSeasonPlayersByTeam(Long seasonId) {
        Map<Long, List<Player>> result = new LinkedHashMap<>();
        for (Team team : seasonService.listSeasonTeams(seasonId)) {
            result.put(
                team.getId(),
                seasonPlayerRepository.findAllActiveDetailedBySeasonIdAndTeamId(seasonId, team.getId()).stream()
                    .map(SeasonPlayer::getPlayer)
                    .toList()
            );
        }
        return result;
    }

    private DemoLeagueStatus buildStatus(DemoDataset dataset) {
        long teamCount = countTracked(dataset.getId(), "TEAM");
        long playerCount = countTracked(dataset.getId(), "PLAYER");
        long refereeCount = countTracked(dataset.getId(), "REFEREE");
        long userCount = countTracked(dataset.getId(), "USER");
        long tourCount = dataset.getSeasonId() == null ? 0 : count("SELECT COUNT(*) FROM work.w_tour WHERE season_id = ? AND active = TRUE", dataset.getSeasonId());
        long matchCount = dataset.getSeasonId() == null ? 0 : count(
            "SELECT COUNT(*) FROM work.w_tour_match tm JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ? AND tm.active = TRUE",
            dataset.getSeasonId()
        );
        long completedCount = dataset.getSeasonId() == null ? 0 : count(
            "SELECT COUNT(*) FROM work.w_match_protocol mp JOIN work.w_tour_match tm ON tm.id = mp.match_id JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ? AND mp.status = 'VERIFIED'",
            dataset.getSeasonId()
        );
        long transferCount = dataset.getSeasonId() == null ? 0 : count(
            "SELECT COUNT(*) FROM work.w_season_transfer_request WHERE season_id = ?",
            dataset.getSeasonId()
        );
        long playoffTieCount = dataset.getSeasonId() == null ? 0 : count(
            "SELECT COUNT(*) FROM work.w_season_playoff_tie tie JOIN work.w_season_playoff_bracket bracket ON bracket.id = tie.bracket_id WHERE bracket.season_id = ?",
            dataset.getSeasonId()
        );
        long playoffMatchCount = dataset.getSeasonId() == null ? 0 : count(
            "SELECT COUNT(*) FROM work.w_tour_match tm JOIN work.w_tour t ON t.id = tm.tour_id WHERE t.season_id = ? AND t.stage_type = 'PLAYOFF' AND tm.active = TRUE",
            dataset.getSeasonId()
        );

        return new DemoLeagueStatus(
            true,
            dataset.getCode(),
            dataset.getName(),
            dataset.getStage(),
            stageIndex(dataset.getStage()),
            dataset.getSeasonId(),
            dataset.getSeasonId() == null ? null : seasonRepository.findById(dataset.getSeasonId()).map(Season::getName).orElse(null),
            new DemoCounts(
                teamCount,
                playerCount,
                refereeCount,
                userCount,
                tourCount,
                matchCount,
                completedCount,
                transferCount,
                playoffTieCount,
                playoffMatchCount
            ),
            demoAccounts(),
            allowedActions(dataset.getStage()),
            dataset.getUpdatedAt()
        );
    }

    private List<DemoAccount> demoAccounts() {
        return List.of(
            new DemoAccount("Болельщик", "demo.fan@local.test", DEMO_PASSWORD, "Просмотр публичной части"),
            new DemoAccount("Представитель 1", "demo.rep1@local.test", DEMO_PASSWORD, "Атлетик Богородск"),
            new DemoAccount("Представитель 2", "demo.rep2@local.test", DEMO_PASSWORD, "Волна Дуденево"),
            new DemoAccount("Рефери", "demo.referee@local.test", DEMO_PASSWORD, "Проверка заявок и протоколов")
        );
    }

    private List<String> allowedActions(String stage) {
        return switch (stage) {
            case "BASE" -> List.of("SCHEDULE", "RESET");
            case "SCHEDULE" -> List.of("RESULTS", "RESET");
            case "RESULTS" -> List.of("TRANSFERS", "RESET");
            case "TRANSFERS" -> List.of("PLAYOFF", "RESET");
            default -> List.of("RESET");
        };
    }

    private int stageIndex(String stage) {
        return switch (stage) {
            case "BASE" -> 1;
            case "SCHEDULE" -> 2;
            case "RESULTS" -> 3;
            case "TRANSFERS" -> 4;
            case "PLAYOFF" -> 5;
            default -> 0;
        };
    }

    private DemoDataset requireDataset() {
        return demoDatasetRepository.findByCode(DATASET_CODE)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Демо-лига еще не создана."));
    }

    private Season requireSeason(DemoDataset dataset) {
        return seasonRepository.findById(dataset.getSeasonId())
            .orElseThrow(() -> new IllegalStateException("Сезон демо-лиги не найден."));
    }

    private void requireStage(DemoDataset dataset, String requiredStage) {
        if (!requiredStage.equals(dataset.getStage())) {
            throw new ResponseStatusException(
                CONFLICT,
                "Операция недоступна на стадии " + dataset.getStage() + ". Ожидается стадия " + requiredStage + "."
            );
        }
    }

    private void updateStage(DemoDataset dataset, String stage) {
        dataset.setStage(stage);
        dataset.setUpdatedAt(OffsetDateTime.now());
        demoDatasetRepository.save(dataset);
    }

    private void track(Long datasetId, String objectType, Long objectId) {
        jdbcTemplate.update(
            "INSERT INTO work.w_demo_dataset_object(dataset_id, object_type, object_id, created_at) VALUES (?, ?, ?, ?)",
            datasetId,
            objectType,
            objectId,
            OffsetDateTime.now()
        );
    }

    private List<Long> trackedIds(Long datasetId, String type) {
        return jdbcTemplate.queryForList(
            "SELECT object_id FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = ? ORDER BY object_id",
            Long.class,
            datasetId,
            type
        );
    }

    private List<Team> trackedTeams(Long datasetId) {
        return teamRepository.findAllById(trackedIds(datasetId, "TEAM")).stream()
            .sorted(Comparator.comparing(Team::getId))
            .toList();
    }

    private List<Referee> trackedReferees(Long datasetId) {
        return refereeRepository.findAllById(trackedIds(datasetId, "REFEREE")).stream()
            .sorted(Comparator.comparing(Referee::getId))
            .toList();
    }

    private List<AppUser> trackedUsers(Long datasetId) {
        return appUserRepository.findAllById(trackedIds(datasetId, "USER"));
    }

    private long countTracked(Long datasetId, String type) {
        return count(
            "SELECT COUNT(*) FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = ?",
            datasetId,
            type
        );
    }

    private long count(String sql, Object... arguments) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return result == null ? 0 : result;
    }

    private void deleteTrackedUsers(Long datasetId) {
        List<Long> userIds = trackedIds(datasetId, "USER");
        if (userIds.isEmpty()) {
            return;
        }
        jdbcTemplate.update(
            "DELETE FROM work.w_refresh_token_session WHERE user_id IN (SELECT object_id FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = 'USER')",
            datasetId
        );
        jdbcTemplate.update(
            "DELETE FROM work.w_user_team_scope WHERE user_id IN (SELECT object_id FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = 'USER')",
            datasetId
        );
        jdbcTemplate.update(
            "DELETE FROM work.w_user_role WHERE user_id IN (SELECT object_id FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = 'USER')",
            datasetId
        );
        jdbcTemplate.update(
            "DELETE FROM work.w_user_login WHERE id IN (SELECT object_id FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = 'USER')",
            datasetId
        );
    }

    private void deleteTrackedEntities(Long datasetId, String type, String tableName) {
        jdbcTemplate.update(
            "DELETE FROM " + tableName + " WHERE id IN (SELECT object_id FROM work.w_demo_dataset_object WHERE dataset_id = ? AND object_type = ?)",
            datasetId,
            type
        );
    }

    private String playerName(int teamIndex, int playerIndex, String teamShortName) {
        int offset = (teamIndex * PLAYERS_PER_TEAM) + playerIndex;
        String firstName = FIRST_NAMES.get(offset % FIRST_NAMES.size());
        String lastName = LAST_NAMES.get((offset / FIRST_NAMES.size() + playerIndex * 4 + teamIndex) % LAST_NAMES.size());
        return firstName + " " + lastName + " (" + teamShortName + ")";
    }

    private record TeamSeed(String name, String shortName, String city) {}
    private record TeamPair(Team home, Team away) {}
    private record RoundPairing(List<TeamPair> matches) {}

    public record DemoLeagueStatus(
        boolean exists,
        String code,
        String name,
        String stage,
        int stageIndex,
        Long seasonId,
        String seasonName,
        DemoCounts counts,
        List<DemoAccount> accounts,
        List<String> allowedActions,
        OffsetDateTime updatedAt
    ) {
        static DemoLeagueStatus empty() {
            return new DemoLeagueStatus(
                false,
                DATASET_CODE,
                DATASET_NAME,
                null,
                0,
                null,
                null,
                new DemoCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(),
                List.of("BASE"),
                null
            );
        }
    }

    public record DemoCounts(
        long teams,
        long players,
        long referees,
        long users,
        long tours,
        long matches,
        long completedMatches,
        long transfers,
        long playoffTies,
        long playoffMatches
    ) {}

    public record DemoAccount(String role, String email, String password, String context) {}
}
