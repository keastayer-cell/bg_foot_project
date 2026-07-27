package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.SeasonApplication;
import com.footballstats.backend.domain.SeasonApplicationStatus;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.SeasonTeam;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.SeasonApplicationRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TeamProfileService {

    private final TeamRepository teamRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final TourMatchRepository tourMatchRepository;
    private final MediaAssetService mediaAssetService;
    private final SeasonPlayerService seasonPlayerService;
    private final SeasonApplicationRepository seasonApplicationRepository;

    public TeamProfileService(
        TeamRepository teamRepository,
        SeasonTeamRepository seasonTeamRepository,
        TourMatchRepository tourMatchRepository,
        MediaAssetService mediaAssetService,
        SeasonPlayerService seasonPlayerService,
        SeasonApplicationRepository seasonApplicationRepository
    ) {
        this.teamRepository = teamRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.mediaAssetService = mediaAssetService;
        this.seasonPlayerService = seasonPlayerService;
        this.seasonApplicationRepository = seasonApplicationRepository;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public TeamProfileData getTeamProfile(Long teamId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));

        List<TeamSeasonData> seasons = seasonTeamRepository.findAllDetailedByTeamId(teamId).stream()
            .map(SeasonTeam::getSeason)
            .map(season -> new TeamSeasonData(
                season.getId(),
                season.getName(),
                season.isActive(),
                season.getCreatedAt()
            ))
            .toList();

        List<TeamMatchData> completedMatches = tourMatchRepository.findAllPublishedDetailedByTeamId(teamId).stream()
            .filter(this::isCompletedMatch)
            .map(match -> toMatchData(teamId, match))
            .toList();

        return new TeamProfileData(
            team.getId(),
            team.getName(),
            team.getShortName(),
            team.getCity(),
            mediaAssetService.loadDataUrl(MediaAssetService.OWNER_TEAM, team.getId(), MediaAssetService.KIND_TEAM_LOGO),
            team.isActive(),
            buildSummary(seasons.size(), completedMatches),
            seasons,
            completedMatches
        );
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public TeamSeasonRosterViewData getSeasonRoster(Long teamId, Long seasonId) {
        teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));

        SeasonApplication application = seasonApplicationRepository.findDetailedBySeasonIdAndTeamId(seasonId, teamId)
            .orElse(null);
        String representativeName = application != null && application.getRepresentativeUser() != null
            ? application.getRepresentativeUser().getName()
            : null;
        List<com.footballstats.backend.domain.Player> rosterPlayers =
            seasonPlayerService.listActiveSeasonPlayers(teamId, seasonId).stream()
                .map(SeasonPlayer::getPlayer)
                .toList();
        Map<Long, String> photos = mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER,
            rosterPlayers.stream().map(com.footballstats.backend.domain.Player::getId).toList(),
            MediaAssetService.KIND_PLAYER_PHOTO
        );
        List<TeamSeasonRosterPlayerData> activePlayers = rosterPlayers.stream()
            .map(player -> new TeamSeasonRosterPlayerData(
                player.getId(),
                player.getFullName(),
                player.isGoalkeeper(),
                player.getBirthDate(),
                player.getResidence(),
                photos.get(player.getId())
            ))
            .toList();

        if (!activePlayers.isEmpty()) {
            return new TeamSeasonRosterViewData(SeasonRosterStatus.APPROVED, representativeName, activePlayers);
        }

        if (application != null && application.getStatus() == SeasonApplicationStatus.SUBMITTED) {
            return new TeamSeasonRosterViewData(SeasonRosterStatus.UNDER_REVIEW, representativeName, List.of());
        }

        return new TeamSeasonRosterViewData(SeasonRosterStatus.WAITING_FILL, representativeName, List.of());
    }

    private boolean isCompletedMatch(TourMatch match) {
        MatchProtocol protocol = match.getProtocol();
        if (protocol == null || protocol.getHomeScore() == null || protocol.getAwayScore() == null) {
            return false;
        }
        return protocol.getStatus() == MatchProtocolStatus.FINISHED
            || protocol.getStatus() == MatchProtocolStatus.VERIFIED;
    }

    private TeamMatchData toMatchData(Long teamId, TourMatch match) {
        boolean home = match.getHomeTeam().getId().equals(teamId);
        Team opponent = home ? match.getAwayTeam() : match.getHomeTeam();
        MatchProtocol protocol = match.getProtocol();
        int teamScore = home ? protocol.getHomeScore() : protocol.getAwayScore();
        int opponentScore = home ? protocol.getAwayScore() : protocol.getHomeScore();
        String resultCode = teamScore > opponentScore ? "W" : teamScore < opponentScore ? "L" : "D";
        String resultLabel = teamScore > opponentScore ? "Победа" : teamScore < opponentScore ? "Поражение" : "Ничья";

        return new TeamMatchData(
            match.getId(),
            match.getTour().getSeason().getId(),
            match.getTour().getSeason().getName(),
            match.getTour().getId(),
            match.getTour().getName(),
            match.getKickoffAt(),
            opponent.getId(),
            opponent.getName(),
            home,
            teamScore,
            opponentScore,
            resultCode,
            resultLabel
        );
    }

    private TeamSummaryData buildSummary(int seasonsCount, List<TeamMatchData> matches) {
        int wins = 0;
        int draws = 0;
        int losses = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;

        for (TeamMatchData match : matches) {
            goalsFor += match.teamScore();
            goalsAgainst += match.opponentScore();
            switch (match.resultCode()) {
                case "W" -> wins++;
                case "L" -> losses++;
                default -> draws++;
            }
        }

        return new TeamSummaryData(matches.size(), wins, draws, losses, goalsFor, goalsAgainst, seasonsCount);
    }

    public record TeamProfileData(
        Long id,
        String name,
        String shortName,
        String city,
        String logoDataUrl,
        boolean active,
        TeamSummaryData summary,
        List<TeamSeasonData> seasons,
        List<TeamMatchData> recentMatches
    ) {}

    public record TeamSummaryData(
        int matchesPlayed,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int seasonsCount
    ) {}

    public record TeamSeasonData(Long id, String name, boolean active, OffsetDateTime createdAt) {}

    public record TeamSeasonRosterPlayerData(
        Long id,
        String fullName,
        boolean goalkeeper,
        LocalDate birthDate,
        String residence,
        String photoDataUrl
    ) {}

    public record TeamSeasonRosterViewData(
        SeasonRosterStatus status,
        String representativeName,
        List<TeamSeasonRosterPlayerData> players
    ) {}

    public enum SeasonRosterStatus {
        WAITING_FILL,
        UNDER_REVIEW,
        APPROVED
    }

    public record TeamMatchData(
        Long matchId,
        Long seasonId,
        String seasonName,
        Long tourId,
        String tourName,
        OffsetDateTime kickoffAt,
        Long opponentTeamId,
        String opponentName,
        boolean home,
        int teamScore,
        int opponentScore,
        String resultCode,
        String resultLabel
    ) {}
}
