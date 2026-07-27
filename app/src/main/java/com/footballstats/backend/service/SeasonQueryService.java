package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.SeasonTransferStatus;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.SeasonTransferRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SeasonQueryService {

    private final PlayerTeamRepository playerTeamRepository;
    private final SeasonTransferRequestRepository seasonTransferRequestRepository;
    private final SeasonPlayerService seasonPlayerService;
    private final MediaAssetService mediaAssetService;

    public SeasonQueryService(
        PlayerTeamRepository playerTeamRepository,
        SeasonTransferRequestRepository seasonTransferRequestRepository,
        SeasonPlayerService seasonPlayerService,
        MediaAssetService mediaAssetService
    ) {
        this.playerTeamRepository = playerTeamRepository;
        this.seasonTransferRequestRepository = seasonTransferRequestRepository;
        this.seasonPlayerService = seasonPlayerService;
        this.mediaAssetService = mediaAssetService;
    }

    @Transactional(readOnly = true)
    public List<SeasonRosterPlayerData> getRoster(Long seasonId, Long teamId) {
        Set<Long> selectedIds = seasonPlayerService.getActivePlayerIds(teamId, seasonId);
        var roster = playerTeamRepository.findCurrentRosterByTeamId(teamId);
        List<Long> playerIds = roster.stream().map(link -> link.getPlayer().getId()).toList();
        Map<Long, String> photos = mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER, playerIds, MediaAssetService.KIND_PLAYER_PHOTO
        );
        return roster.stream().map(link -> {
            Player player = link.getPlayer();
            return new SeasonRosterPlayerData(
                player.getId(), player.getFullName(), player.getBirthDate(), player.getResidence(),
                player.isGoalkeeper(), photos.get(player.getId()), selectedIds.contains(player.getId()), link.getValidFrom()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<SeasonTransferData> listTransfers(Long seasonId) {
        var requests = seasonTransferRequestRepository.findAllDetailedBySeasonId(seasonId);
        Map<Long, String> photos = mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER,
            requests.stream().map(request -> request.getPlayer().getId()).toList(),
            MediaAssetService.KIND_PLAYER_PHOTO
        );
        return requests.stream().map(request -> toTransferData(request, photos.get(request.getPlayer().getId()))).toList();
    }

    @Transactional(readOnly = true)
    public Page<SeasonTransferData> listTransfers(Long seasonId, int pageNum, int pageSize) {
        var requests = seasonTransferRequestRepository.findPageDetailedBySeasonId(
            seasonId, PageRequest.of(Math.max(pageNum, 0), Math.min(Math.max(pageSize, 1), 100))
        );
        Map<Long, String> photos = mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER,
            requests.getContent().stream().map(request -> request.getPlayer().getId()).toList(),
            MediaAssetService.KIND_PLAYER_PHOTO
        );
        return requests.map(request -> toTransferData(request, photos.get(request.getPlayer().getId())));
    }

    private SeasonTransferData toTransferData(com.footballstats.backend.domain.SeasonTransferRequest request, String photo) {
        return new SeasonTransferData(
            request.getId(), request.getPlayer().getId(), request.getPlayer().getFullName(),
            request.getPlayer().isGoalkeeper(), photo,
            request.getFromTeam().getId(), request.getFromTeam().getName(),
            request.getToTeam().getId(), request.getToTeam().getName(),
            request.getRequestedAt() == null ? null : request.getRequestedAt().toLocalDate(),
            request.getStatus(), request.getRequestComment(), request.getDecisionComment(),
            request.getRequestedAt(), request.getProcessedAt()
        );
    }

    public record SeasonRosterPlayerData(
        Long id, String fullName, LocalDate birthDate, String residence, boolean isGoalkeeper,
        String photoDataUrl, boolean selectedForSeason, LocalDate inTeamSince
    ) {}

    public record SeasonTransferData(
        Long id, Long playerId, String playerName, boolean playerGoalkeeper, String playerPhotoDataUrl,
        Long fromTeamId, String fromTeamName, Long toTeamId, String toTeamName, LocalDate requestedDate,
        SeasonTransferStatus status, String requestComment, String decisionComment,
        OffsetDateTime requestedAt, OffsetDateTime processedAt
    ) {}
}
