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
public class CompetitionHonorsService {
    private static final String TOP_SCORER = "TOP_SCORER";
    private final CompetitionRepository competitions;
    private final CompetitionTeamRepository competitionTeams;
    private final CompetitionRosterPlayerRepository competitionRoster;
    private final SeasonPlayerRepository seasonPlayers;
    private final PlayerRepository players;
    private final TeamRepository teams;
    private final CompetitionAwardRepository awards;
    private final CompetitionSelectionSlotRepository slots;
    private final CompetitionStatsService stats;
    private final MediaAssetService media;

    public CompetitionHonorsService(
        CompetitionRepository competitions, CompetitionTeamRepository competitionTeams,
        CompetitionRosterPlayerRepository competitionRoster, SeasonPlayerRepository seasonPlayers,
        PlayerRepository players, TeamRepository teams, CompetitionAwardRepository awards,
        CompetitionSelectionSlotRepository slots, CompetitionStatsService stats, MediaAssetService media
    ) {
        this.competitions = competitions;
        this.competitionTeams = competitionTeams;
        this.competitionRoster = competitionRoster;
        this.seasonPlayers = seasonPlayers;
        this.players = players;
        this.teams = teams;
        this.awards = awards;
        this.slots = slots;
        this.stats = stats;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public EditorData editor(Long seasonId, Long competitionId) {
        Competition competition = requireCompetition(seasonId, competitionId);
        return buildEditor(competition);
    }

    @Transactional
    public EditorData save(Long seasonId, Long competitionId, HonorsUpdate update) {
        Competition competition = requireCompetition(seasonId, competitionId);
        awards.deleteAllByCompetition_Id(competitionId);
        slots.deleteAllByCompetition_Id(competitionId);
        awards.flush();
        slots.flush();

        int fallbackSort = 10;
        for (AwardInput input : update.awards() == null ? List.<AwardInput>of() : update.awards()) {
            if (input == null || input.title() == null || input.title().isBlank() || TOP_SCORER.equals(input.code())) continue;
            saveManualAward(competition, input, fallbackSort);
            fallbackSort += 10;
        }
        appendTopScorers(competition);

        int slotSort = 10;
        Set<Long> selectedPlayers = new HashSet<>();
        for (SlotInput input : update.slots() == null ? List.<SlotInput>of() : update.slots()) {
            if (input == null || input.playerId() == null) continue;
            if (!selectedPlayers.add(input.playerId())) throw new IllegalArgumentException("Игрок не может занимать две позиции в символической сборной.");
            Player player = players.findById(input.playerId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));
            CandidateData candidate = candidateMap(competition).get(player.getId());
            if (candidate == null) throw new IllegalArgumentException("Игрок не участвовал в выбранном соревновании.");
            CompetitionSelectionSlot slot = new CompetitionSelectionSlot();
            slot.setCompetition(competition);
            slot.setPlayer(player);
            Team candidateTeam = teams.findById(candidate.teamId()).orElseThrow();
            slot.setTeam(candidateTeam);
            slot.setPlayerNameSnapshot(player.getFullName());
            slot.setTeamNameSnapshot(candidate.teamName());
            slot.setTeamShortNameSnapshot(candidate.teamShortName());
            slot.setPositionLabel(normalize(input.positionLabel(), "Игрок"));
            slot.setXPercent(bound(input.xPercent(), 5, 95, 50));
            slot.setYPercent(bound(input.yPercent(), 5, 95, 50));
            slot.setSortOrder(input.sortOrder() == null ? slotSort : input.sortOrder());
            slots.save(slot);
            slotSort += 10;
        }

        competition.setHonorsFormation(normalize(update.formation(), defaultFormation(competition.getPlayersOnField())));
        competition.setHonorsPublished(update.published());
        competition.setHonorsUpdatedAt(OffsetDateTime.now());
        competition.setUpdatedAt(OffsetDateTime.now());
        competitions.save(competition);
        return buildEditor(competition);
    }

    @Transactional(readOnly = true)
    public List<HallEntryData> hallOfFame() {
        return competitions.findAllPublishedHonors().stream().map(this::toHallEntry).toList();
    }

    private void saveManualAward(Competition competition, AwardInput input, int fallbackSort) {
        if (input.winnerId() == null) {
            throw new IllegalArgumentException("Для награды нужно выбрать победителя.");
        }
        String type = "TEAM".equalsIgnoreCase(input.winnerType()) ? "TEAM" : "PLAYER";
        CompetitionAward award = new CompetitionAward();
        award.setCompetition(competition);
        award.setAwardCode(normalize(input.code(), null));
        award.setTitle(input.title().trim());
        award.setWinnerType(type);
        award.setSortOrder(input.sortOrder() == null ? fallbackSort : input.sortOrder());
        if ("TEAM".equals(type)) {
            Team team = teams.findById(input.winnerId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
            if (!competitionTeams.existsByCompetition_IdAndTeam_Id(competition.getId(), team.getId())) throw new IllegalArgumentException("Команда не участвует в соревновании.");
            award.setTeam(team);
            award.setWinnerNameSnapshot(team.getName());
        } else {
            CandidateData candidate = candidateMap(competition).get(input.winnerId());
            if (candidate == null) throw new IllegalArgumentException("Игрок не участвовал в выбранном соревновании.");
            Player player = players.findById(input.winnerId()).orElseThrow();
            award.setPlayer(player);
            award.setTeam(teams.findById(candidate.teamId()).orElseThrow());
            award.setWinnerNameSnapshot(player.getFullName());
            award.setTeamNameSnapshot(candidate.teamName());
        }
        awards.save(award);
    }

    private void appendTopScorers(Competition competition) {
        List<CompetitionStatsService.PlayerStats> rows = stats.playerStats(competition.getId());
        Map<Long, CandidateData> candidates = candidateMap(competition);
        int max = rows.stream().mapToInt(CompetitionStatsService.PlayerStats::goals).max().orElse(0);
        if (max <= 0) return;
        int order = 80;
        for (CompetitionStatsService.PlayerStats row : rows) {
            if (row.goals() != max) continue;
            CompetitionAward award = new CompetitionAward();
            award.setCompetition(competition);
            award.setAwardCode(TOP_SCORER);
            award.setTitle("Лучший бомбардир");
            award.setWinnerType("PLAYER");
            award.setPlayer(players.findById(row.playerId()).orElseThrow());
            CandidateData candidate = candidates.get(row.playerId());
            if (candidate != null) award.setTeam(teams.findById(candidate.teamId()).orElseThrow());
            award.setWinnerNameSnapshot(row.playerName());
            award.setTeamNameSnapshot(row.teamNames());
            award.setStatValue(row.goals());
            award.setSortOrder(order++);
            awards.save(award);
        }
    }

    private EditorData buildEditor(Competition competition) {
        List<TeamData> teamData = competitionTeams.findAllDetailedByCompetitionId(competition.getId()).stream()
            .map(item -> new TeamData(item.getTeam().getId(), item.getTeam().getName(), item.getTeam().getShortName())).toList();
        List<CandidateData> candidates = new ArrayList<>(candidateMap(competition).values());
        candidates.sort(Comparator.comparing(CandidateData::playerName));
        return new EditorData(
            competition.getId(), competition.getSeason().getId(), competition.getSeason().getName(), competition.getName(),
            competition.getType().name(), competition.getPlayersOnField(), competition.isHonorsPublished(),
            normalize(competition.getHonorsFormation(), defaultFormation(competition.getPlayersOnField())),
            awardData(competition.getId()), slotData(competition.getId()), teamData, candidates
        );
    }

    private HallEntryData toHallEntry(Competition competition) {
        return new HallEntryData(
            competition.getId(), competition.getSeason().getId(), competition.getSeason().getName(), competition.getName(),
            competition.getType().name(), competition.getHonorsFormation(), awardData(competition.getId()), slotData(competition.getId())
        );
    }

    private List<AwardData> awardData(Long competitionId) {
        return awards.findAllByCompetition_IdOrderBySortOrderAscIdAsc(competitionId).stream().map(award -> new AwardData(
            award.getId(), award.getAwardCode(), award.getTitle(), award.getWinnerType(),
            award.getPlayer() == null ? null : award.getPlayer().getId(), award.getTeam() == null ? null : award.getTeam().getId(),
            award.getWinnerNameSnapshot(), award.getTeamNameSnapshot(),
            award.getTeam() == null ? award.getTeamNameSnapshot() : normalize(award.getTeam().getShortName(), award.getTeamNameSnapshot()),
            award.getStatValue(), award.getSortOrder(),
            award.getPlayer() == null ? null : media.loadDataUrl(MediaAssetService.OWNER_PLAYER, award.getPlayer().getId(), MediaAssetService.KIND_PLAYER_PHOTO)
        )).toList();
    }

    private List<SlotData> slotData(Long competitionId) {
        return slots.findAllByCompetition_IdOrderBySortOrderAscIdAsc(competitionId).stream().map(slot -> new SlotData(
            slot.getId(), slot.getPlayer().getId(), slot.getPlayerNameSnapshot(), slot.getTeam() == null ? null : slot.getTeam().getId(),
            slot.getTeamNameSnapshot(), slot.getTeamShortNameSnapshot(), slot.getPositionLabel(),
            slot.getXPercent(), slot.getYPercent(), slot.getSortOrder(),
            media.loadDataUrl(MediaAssetService.OWNER_PLAYER, slot.getPlayer().getId(), MediaAssetService.KIND_PLAYER_PHOTO)
        )).toList();
    }

    private Map<Long, CandidateData> candidateMap(Competition competition) {
        Map<Long, CandidateData> result = new LinkedHashMap<>();
        if (competition.getRosterMode() == CompetitionRosterMode.OWN) {
            for (CompetitionRosterPlayer item : competitionRoster.findAllActiveDetailedByCompetitionId(competition.getId())) {
                addCandidate(result, item.getPlayer(), item.getTeam());
            }
        } else {
            for (CompetitionTeam item : competitionTeams.findAllDetailedByCompetitionId(competition.getId())) {
                for (SeasonPlayer seasonPlayer : seasonPlayers.findAllActiveDetailedBySeasonIdAndTeamId(competition.getSeason().getId(), item.getTeam().getId())) {
                    addCandidate(result, seasonPlayer.getPlayer(), item.getTeam());
                }
            }
        }
        return result;
    }

    private void addCandidate(Map<Long, CandidateData> result, Player player, Team team) {
        result.put(player.getId(), new CandidateData(player.getId(), player.getFullName(), player.getPosition() == null ? null : player.getPosition().name(), team.getId(), team.getName(), normalize(team.getShortName(), team.getName())));
    }

    private Competition requireCompetition(Long seasonId, Long competitionId) {
        Competition competition = competitions.findDetailedById(competitionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено."));
        if (!competition.getSeason().getId().equals(seasonId) || !competition.isActive()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено.");
        return competition;
    }

    private static String normalize(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private static int bound(Integer value, int min, int max, int fallback) { return value == null ? fallback : Math.max(min, Math.min(max, value)); }
    private static String defaultFormation(int players) { return players <= 5 ? "1-2-1" : players <= 8 ? "2-3-2" : "4-4-2"; }

    public record HonorsUpdate(boolean published, String formation, List<AwardInput> awards, List<SlotInput> slots) {}
    public record AwardInput(String code, String title, String winnerType, Long winnerId, Integer sortOrder) {}
    public record SlotInput(Long playerId, String positionLabel, Integer xPercent, Integer yPercent, Integer sortOrder) {}
    public record TeamData(Long id, String name, String shortName) {}
    public record CandidateData(Long playerId, String playerName, String position, Long teamId, String teamName, String teamShortName) {}
    public record AwardData(Long id, String code, String title, String winnerType, Long playerId, Long teamId, String winnerName, String teamName, String teamShortName, Integer statValue, int sortOrder, String photoDataUrl) {}
    public record SlotData(Long id, Long playerId, String playerName, Long teamId, String teamName, String teamShortName, String positionLabel, int xPercent, int yPercent, int sortOrder, String photoDataUrl) {}
    public record EditorData(Long competitionId, Long seasonId, String seasonName, String competitionName, String competitionType, int playersOnField, boolean published, String formation, List<AwardData> awards, List<SlotData> slots, List<TeamData> teams, List<CandidateData> players) {}
    public record HallEntryData(Long competitionId, Long seasonId, String seasonName, String competitionName, String competitionType, String formation, List<AwardData> awards, List<SlotData> slots) {}
}
