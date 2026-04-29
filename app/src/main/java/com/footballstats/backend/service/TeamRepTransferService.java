package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.SeasonTransferRequest;
import com.footballstats.backend.domain.SeasonTransferStatus;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.UserTeamScope;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.SeasonTransferRequestRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TeamRepTransferService {

    private static final Set<SeasonTransferStatus> BLOCKING_TRANSFER_STATUSES = EnumSet.of(
        SeasonTransferStatus.PENDING,
        SeasonTransferStatus.APPROVED
    );

    private final UserTeamScopeRepository userTeamScopeRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final SeasonPlayerService seasonPlayerService;
    private final SeasonTransferRequestRepository seasonTransferRequestRepository;
    private final AppUserRepository appUserRepository;
    private final MediaAssetService mediaAssetService;

    public TeamRepTransferService(
        UserTeamScopeRepository userTeamScopeRepository,
        TeamRepository teamRepository,
        PlayerRepository playerRepository,
        SeasonRepository seasonRepository,
        SeasonTeamRepository seasonTeamRepository,
        SeasonPlayerService seasonPlayerService,
        SeasonTransferRequestRepository seasonTransferRequestRepository,
        AppUserRepository appUserRepository,
        MediaAssetService mediaAssetService
    ) {
        this.userTeamScopeRepository = userTeamScopeRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.seasonRepository = seasonRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.seasonPlayerService = seasonPlayerService;
        this.seasonTransferRequestRepository = seasonTransferRequestRepository;
        this.appUserRepository = appUserRepository;
        this.mediaAssetService = mediaAssetService;
    }

    @Transactional(readOnly = true)
    public TeamRepTransferOverviewData getSeasonTransfers(TransferActor actor, Long seasonId) {
        return getSeasonTransfers(actor, seasonId, 0, 20);
    }

    @Transactional(readOnly = true)
    public TeamRepTransferOverviewData getSeasonTransfers(TransferActor actor, Long seasonId, int pageNum, int pageSize) {
        TransferAccessContext access = requireTransferAccess(actor);
        Season season = access.privilegedAccess()
            ? requireSeason(seasonId)
            : seasonPlayerService.getSeasonForTeam(access.teamId(), seasonId);
        List<TeamRepTransferTeamData> seasonTeams = seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(seasonId).stream()
            .map(item -> item.getTeam())
            .sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
            .map(team -> new TeamRepTransferTeamData(team.getId(), team.getName(), team.getShortName(), team.getCity()))
            .toList();
        List<TeamRepTransferTeamData> sourceTeams = access.privilegedAccess()
            ? seasonTeams
            : seasonTeams.stream().filter(team -> !team.id().equals(access.teamId())).toList();
        List<TeamRepTransferTeamData> targetTeams = access.privilegedAccess()
            ? seasonTeams
            : seasonTeams.stream().filter(team -> team.id().equals(access.teamId())).toList();

        Pageable pageable = buildPageable(pageNum, pageSize);
        Page<SeasonTransferRequest> requestsPage = access.privilegedAccess()
            ? seasonTransferRequestRepository.findPageDetailedBySeasonId(seasonId, pageable)
            : seasonTransferRequestRepository.findPageDetailedBySeasonIdAndTeamId(seasonId, access.teamId(), pageable);
        Map<Long, String> userNames = resolveUserNames(requestsPage.getContent());

        return new TeamRepTransferOverviewData(
            season.getId(),
            season.getName(),
            season.getStatus(),
            season.getTransferWindowStartDate(),
            season.getTransferWindowEndDate(),
            isTransferWindowOpen(season),
            access.privilegedAccess() ? null : season.getMaxRosterSize(),
            access.teamId(),
            access.teamName(),
            access.teamId() == null ? 0 : seasonPlayerService.countActiveSeasonPlayers(access.teamId(), seasonId),
            sourceTeams,
            targetTeams,
            access.privilegedAccess(),
            requestsPage.getContent().stream().map(request -> toTransferData(request, access, userNames, season)).toList(),
            requestsPage.getNumber(),
            requestsPage.getSize(),
            requestsPage.getTotalElements(),
            requestsPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public IncomingTransferNotificationsData getIncomingPendingTransfers(Long userId, int pageNum, int pageSize) {
        TeamScopeContext context = requireApplicationScope(userId);
        Pageable pageable = buildPageable(pageNum, pageSize);
        Page<SeasonTransferRequest> requestsPage = seasonTransferRequestRepository.findIncomingPendingDetailedByTeamId(context.teamId(), pageable);
        Map<Long, String> userNames = resolveUserNames(requestsPage.getContent());
        long totalPendingCount = seasonTransferRequestRepository.countByStatusAndFromTeam_Id(SeasonTransferStatus.PENDING, context.teamId());
        TransferAccessContext access = new TransferAccessContext(userId, context.teamId(), context.teamName(), false, false);

        return new IncomingTransferNotificationsData(
            totalPendingCount,
            requestsPage.getContent().stream()
                .map(request -> toTransferData(request, access, userNames, request.getSeason()))
                .toList(),
            requestsPage.getNumber(),
            requestsPage.getSize(),
            requestsPage.getTotalElements(),
            requestsPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<TeamRepTransferCandidateData> listTransferCandidates(TransferActor actor, Long seasonId, Long fromTeamId, Long toTeamId) {
        TransferAccessContext access = requireTransferAccess(actor);
        Long resolvedToTeamId = resolveTargetTeamId(access, toTeamId);
        Season season = access.privilegedAccess()
            ? requireSeason(seasonId)
            : seasonPlayerService.getSeasonForTeam(resolvedToTeamId, seasonId);
        ensureTransferWindowOpen(season);
        validateTransferTeams(seasonId, resolvedToTeamId, fromTeamId);

        return seasonPlayerService.listActiveSeasonPlayers(fromTeamId, seasonId).stream()
            .filter(item -> !hasBlockingTransferRequest(seasonId, item.getPlayer().getId()))
            .map(SeasonPlayer::getPlayer)
            .sorted(Comparator.comparing(Player::getFullName, String.CASE_INSENSITIVE_ORDER))
            .map(player -> new TeamRepTransferCandidateData(
                player.getId(),
                player.getFullName(),
                player.getBirthDate(),
                player.getResidence(),
                player.isGoalkeeper(),
                mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, player.getId(), MediaAssetService.KIND_PLAYER_PHOTO)
            ))
            .toList();
    }

    @Transactional
    public TeamRepTransferOverviewData createTransferRequest(TransferActor actor, Long seasonId, Long fromTeamId, Long toTeamId, Long playerId, String requestComment) {
        TransferAccessContext access = requireTransferAccess(actor);
        Long resolvedToTeamId = resolveTargetTeamId(access, toTeamId);
        Season season = access.privilegedAccess()
            ? requireSeason(seasonId)
            : seasonPlayerService.getSeasonForTeam(resolvedToTeamId, seasonId);
        ensureTransferWindowOpen(season);
        validateTransferTeams(seasonId, resolvedToTeamId, fromTeamId);

        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));

        SeasonPlayer activeAssignment = seasonPlayerService.getActiveAssignmentForSeason(seasonId, playerId);
        if (activeAssignment == null || !activeAssignment.getTeam().getId().equals(fromTeamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Игрок уже не числится в выбранной команде этого сезона.");
        }
        if (hasBlockingTransferRequest(seasonId, playerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Игрок уже участвует в трансфере этого сезона или уже переходил в другой клуб.");
        }

        seasonPlayerService.validateSeasonRosterCapacity(season, resolvedToTeamId, 1L, null);

        SeasonTransferRequest request = new SeasonTransferRequest();
        request.setSeason(season);
        request.setPlayer(player);
        request.setFromTeam(requireTeam(fromTeamId));
        request.setToTeam(requireTeam(resolvedToTeamId));
        request.setRequestedByUserId(actor.userId());
        request.setRequestComment(normalizeOptional(requestComment));
        request.setRequestedAt(OffsetDateTime.now());
        request.setStatus(SeasonTransferStatus.PENDING);
        seasonTransferRequestRepository.save(request);

        return getSeasonTransfers(actor, seasonId);
    }

    @Transactional
    public TeamRepTransferOverviewData approveTransferRequest(TransferActor actor, Long requestId, String decisionComment) {
        TransferAccessContext access = requireTransferAccess(actor);
        SeasonTransferRequest request = seasonTransferRequestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Трансферная заявка не найдена."));
        validateProcessorScope(access, request);
        ensurePending(request);
        ensureTransferWindowOpen(request.getSeason());

        seasonPlayerService.transferSeasonPlayer(
            request.getSeason().getId(),
            request.getPlayer().getId(),
            request.getFromTeam().getId(),
            request.getToTeam().getId(),
            actor.userId()
        );

        request.setStatus(SeasonTransferStatus.APPROVED);
        request.setDecisionComment(normalizeOptional(decisionComment));
        request.setProcessedByUserId(actor.userId());
        request.setProcessedAt(OffsetDateTime.now());
        seasonTransferRequestRepository.save(request);
        return getSeasonTransfers(actor, request.getSeason().getId());
    }

    @Transactional
    public TeamRepTransferOverviewData rejectTransferRequest(TransferActor actor, Long requestId, String decisionComment) {
        TransferAccessContext access = requireTransferAccess(actor);
        SeasonTransferRequest request = seasonTransferRequestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Трансферная заявка не найдена."));
        validateProcessorScope(access, request);
        ensurePending(request);
        ensureTransferWindowOpen(request.getSeason());

        request.setStatus(SeasonTransferStatus.REJECTED);
        request.setDecisionComment(normalizeOptional(decisionComment));
        request.setProcessedByUserId(actor.userId());
        request.setProcessedAt(OffsetDateTime.now());
        seasonTransferRequestRepository.save(request);
        return getSeasonTransfers(actor, request.getSeason().getId());
    }

    @Transactional
    public TeamRepTransferOverviewData revokeTransferRequest(TransferActor actor, Long requestId, String decisionComment) {
        TransferAccessContext access = requireTransferAccess(actor);
        SeasonTransferRequest request = seasonTransferRequestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Трансферная заявка не найдена."));
        validateRequesterScope(access, request);

        if (request.getStatus() == SeasonTransferStatus.APPROVED) {
            if (!access.canRevokeProcessedTransfers()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Отзывать подтвержденные трансферы может только супер администратор.");
            }
            seasonPlayerService.transferSeasonPlayer(
                request.getSeason().getId(),
                request.getPlayer().getId(),
                request.getToTeam().getId(),
                request.getFromTeam().getId(),
                actor.userId()
            );
        } else {
            ensurePending(request);
        }

        request.setStatus(SeasonTransferStatus.REVOKED);
        request.setDecisionComment(normalizeOptional(decisionComment));
        request.setProcessedByUserId(actor.userId());
        request.setProcessedAt(OffsetDateTime.now());
        seasonTransferRequestRepository.save(request);
        return getSeasonTransfers(actor, request.getSeason().getId());
    }

    private TeamRepTransferData toTransferData(
        SeasonTransferRequest request,
        TransferAccessContext access,
        Map<Long, String> userNames,
        Season season
    ) {
        boolean canProcess = request.getStatus() == SeasonTransferStatus.PENDING
            && (access.privilegedAccess() || request.getFromTeam().getId().equals(access.teamId()))
            && isTransferWindowOpen(season);
        boolean canRevoke = access.canRevokeProcessedTransfers()
            ? request.getStatus() == SeasonTransferStatus.PENDING || request.getStatus() == SeasonTransferStatus.APPROVED
            : request.getStatus() == SeasonTransferStatus.PENDING
                && access.teamId() != null
                && request.getToTeam().getId().equals(access.teamId());

        return new TeamRepTransferData(
            request.getId(),
            request.getPlayer().getId(),
            request.getPlayer().getFullName(),
            mediaAssetService.loadDataUrl(MediaAssetService.OWNER_PLAYER, request.getPlayer().getId(), MediaAssetService.KIND_PLAYER_PHOTO),
            request.getPlayer().isGoalkeeper(),
            request.getFromTeam().getId(),
            request.getFromTeam().getName(),
            request.getToTeam().getId(),
            request.getToTeam().getName(),
            request.getStatus(),
            request.getRequestComment(),
            request.getDecisionComment(),
            request.getRequestedAt(),
            request.getProcessedAt(),
            userNames.getOrDefault(request.getRequestedByUserId(), "-"),
            userNames.getOrDefault(request.getProcessedByUserId(), "-"),
            canProcess,
            canProcess,
            canRevoke
        );
    }

    private Map<Long, String> resolveUserNames(List<SeasonTransferRequest> requests) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (SeasonTransferRequest request : requests) {
            if (request.getRequestedByUserId() != null) {
                userIds.add(request.getRequestedByUserId());
            }
            if (request.getProcessedByUserId() != null) {
                userIds.add(request.getProcessedByUserId());
            }
        }
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> result = new LinkedHashMap<>();
        for (AppUser appUser : appUserRepository.findAllById(userIds)) {
            result.put(appUser.getId(), appUser.getName());
        }
        return result;
    }

    private void validateProcessorScope(TransferAccessContext access, SeasonTransferRequest request) {
        if (!access.privilegedAccess() && !request.getFromTeam().getId().equals(access.teamId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Подтверждать или отклонять трансфер может только представитель команды-источника.");
        }
    }

    private void validateRequesterScope(TransferAccessContext access, SeasonTransferRequest request) {
        if (access.canRevokeProcessedTransfers()) {
            return;
        }
        if (!request.getToTeam().getId().equals(access.teamId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Отозвать трансфер может только представитель команды, создавшей заявку.");
        }
    }

    private void ensurePending(SeasonTransferRequest request) {
        if (request.getStatus() != SeasonTransferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заявка уже обработана.");
        }
    }

    private boolean hasBlockingTransferRequest(Long seasonId, Long playerId) {
        return seasonTransferRequestRepository.existsBySeason_IdAndPlayer_IdAndStatusIn(
            seasonId,
            playerId,
            BLOCKING_TRANSFER_STATUSES
        );
    }

    private void validateTransferTeams(Long seasonId, Long toTeamId, Long fromTeamId) {
        if (fromTeamId == null || fromTeamId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно выбрать команду, из которой переводится игрок.");
        }
        if (toTeamId == null || toTeamId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно выбрать команду назначения.");
        }
        if (fromTeamId.equals(toTeamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя запросить трансфер внутри одной и той же команды.");
        }
        if (!seasonTeamRepository.existsBySeason_IdAndTeam_Id(seasonId, fromTeamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда-источник не участвует в выбранном сезоне.");
        }
        if (!seasonTeamRepository.existsBySeason_IdAndTeam_Id(seasonId, toTeamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда назначения не участвует в выбранном сезоне.");
        }
    }

    private Season requireSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
    }

    private Long resolveTargetTeamId(TransferAccessContext access, Long requestedTeamId) {
        if (!access.privilegedAccess()) {
            return access.teamId();
        }
        if (requestedTeamId == null || requestedTeamId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для создания трансфера нужно выбрать команду назначения.");
        }
        return requestedTeamId;
    }

    private Team requireTeam(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
    }

    private void ensureTransferWindowOpen(Season season) {
        if (!isTransferWindowOpen(season)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, buildTransferWindowClosedMessage(season));
        }
    }

    private boolean isTransferWindowOpen(Season season) {
        LocalDate today = LocalDate.now();
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            return false;
        }
        if (season.getTransferWindowStartDate() != null && today.isBefore(season.getTransferWindowStartDate())) {
            return false;
        }
        return season.getTransferWindowEndDate() == null || !today.isAfter(season.getTransferWindowEndDate());
    }

    private String buildTransferWindowClosedMessage(Season season) {
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            return "Трансферы доступны только в активном сезоне.";
        }
        if (season.getTransferWindowStartDate() != null && LocalDate.now().isBefore(season.getTransferWindowStartDate())) {
            return "Окно трансферов еще не открыто.";
        }
        if (season.getTransferWindowEndDate() != null) {
            return "Окно трансферов закрыто с " + season.getTransferWindowEndDate() + ".";
        }
        return "Трансферы в этом сезоне закрыты.";
    }

    private String normalizeOptional(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Pageable buildPageable(int pageNum, int pageSize) {
        return PageRequest.of(Math.max(pageNum, 0), Math.min(Math.max(pageSize, 1), 100));
    }

    private TeamScopeContext requireApplicationScope(Long userId) {
        UserTeamScope scope = userTeamScopeRepository.findByUser_IdAndActiveTrue(userId).stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Для пользователя не назначена команда."));
        if (!scope.isCanEditApplication()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав на управление заявкой сезона.");
        }
        return new TeamScopeContext(scope, scope.getTeam().getId(), scope.getTeam().getName());
    }

    private TransferAccessContext requireTransferAccess(TransferActor actor) {
        if (actor == null || actor.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь не авторизован.");
        }
        if (actor.superAdmin()) {
            return new TransferAccessContext(actor.userId(), null, null, true, true);
        }
        if (actor.referee()) {
            return new TransferAccessContext(actor.userId(), null, null, true, false);
        }

        TeamScopeContext scope = requireApplicationScope(actor.userId());
        return new TransferAccessContext(actor.userId(), scope.teamId(), scope.teamName(), false, false);
    }

    private record TeamScopeContext(UserTeamScope scope, Long teamId, String teamName) {}

    private record TransferAccessContext(
        Long userId,
        Long teamId,
        String teamName,
        boolean privilegedAccess,
        boolean canRevokeProcessedTransfers
    ) {}

    public record TeamRepTransferOverviewData(
        Long seasonId,
        String seasonName,
        SeasonStatus seasonStatus,
        LocalDate transferWindowStartDate,
        LocalDate transferWindowEndDate,
        boolean transferWindowOpen,
        Integer maxRosterSize,
        Long teamId,
        String teamName,
        long selectedPlayersCount,
        List<TeamRepTransferTeamData> sourceTeams,
        List<TeamRepTransferTeamData> targetTeams,
        boolean privilegedAccess,
        List<TeamRepTransferData> requests,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
    ) {}

    public record TransferActor(Long userId, boolean teamRep, boolean superAdmin, boolean referee) {}

    public record IncomingTransferNotificationsData(
        long totalPendingCount,
        List<TeamRepTransferData> requests,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
    ) {}

    public record TeamRepTransferTeamData(Long id, String name, String shortName, String city) {}

    public record TeamRepTransferCandidateData(
        Long id,
        String fullName,
        LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl
    ) {}

    public record TeamRepTransferData(
        Long id,
        Long playerId,
        String playerName,
        String playerPhotoDataUrl,
        boolean playerGoalkeeper,
        Long fromTeamId,
        String fromTeamName,
        Long toTeamId,
        String toTeamName,
        SeasonTransferStatus status,
        String requestComment,
        String decisionComment,
        OffsetDateTime requestedAt,
        OffsetDateTime processedAt,
        String requestedByName,
        String processedByName,
        boolean canApprove,
        boolean canReject,
        boolean canRevoke
    ) {}
}