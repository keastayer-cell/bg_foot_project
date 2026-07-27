package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonApplication;
import com.footballstats.backend.domain.SeasonApplicationPlayer;
import com.footballstats.backend.domain.SeasonApplicationStatus;
import com.footballstats.backend.domain.SeasonPlayer;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.UserRole;
import com.footballstats.backend.domain.UserTeamScope;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.SeasonApplicationPlayerRepository;
import com.footballstats.backend.repository.SeasonApplicationRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.UserRoleRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SeasonApplicationService {

    private final UserTeamScopeRepository userTeamScopeRepository;
    private final SeasonPlayerService seasonPlayerService;
    private final PlayerTeamRepository playerTeamRepository;
    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final TeamRepository teamRepository;
    private final SeasonApplicationRepository seasonApplicationRepository;
    private final SeasonApplicationPlayerRepository seasonApplicationPlayerRepository;
    private final MediaAssetService mediaAssetService;
    private final NotificationEventService notificationEventService;

    public SeasonApplicationService(
        UserTeamScopeRepository userTeamScopeRepository,
        SeasonPlayerService seasonPlayerService,
        PlayerTeamRepository playerTeamRepository,
        PlayerRepository playerRepository,
        AppUserRepository appUserRepository,
        UserRoleRepository userRoleRepository,
        TeamRepository teamRepository,
        SeasonApplicationRepository seasonApplicationRepository,
        SeasonApplicationPlayerRepository seasonApplicationPlayerRepository,
        MediaAssetService mediaAssetService,
        NotificationEventService notificationEventService
    ) {
        this.userTeamScopeRepository = userTeamScopeRepository;
        this.seasonPlayerService = seasonPlayerService;
        this.playerTeamRepository = playerTeamRepository;
        this.playerRepository = playerRepository;
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.teamRepository = teamRepository;
        this.seasonApplicationRepository = seasonApplicationRepository;
        this.seasonApplicationPlayerRepository = seasonApplicationPlayerRepository;
        this.mediaAssetService = mediaAssetService;
        this.notificationEventService = notificationEventService;
    }

    @Transactional(readOnly = true)
    public TeamRepService.TeamRepSeasonData toSeasonSummary(Long userId, Season season) {
        return toSeasonSummary(userId, null, season, false);
    }

    @Transactional(readOnly = true)
    public TeamRepService.TeamRepSeasonData toSeasonSummary(Long userId, Long requestedTeamId, Season season, boolean privilegedAccess) {
        TeamScopeContext context = requireApplicationScope(userId, requestedTeamId, privilegedAccess);
        SeasonApplication application = findApplication(context.teamId(), season.getId()).orElse(null);
        SeasonApplicationStatus applicationStatus = application == null ? SeasonApplicationStatus.DRAFT : application.getStatus();
        long selectedCount = application == null
            ? seasonPlayerService.countActiveSeasonPlayers(context.teamId(), season.getId())
            : seasonApplicationPlayerRepository.findAllDetailedByApplicationId(application.getId()).size();
        long rosterCount = playerTeamRepository.findCurrentRosterByTeamId(context.teamId()).size();
        return new TeamRepService.TeamRepSeasonData(
            season.getId(),
            season.getName(),
            season.getApplicationDeadline(),
            season.getStatus(),
            season.getMaxRosterSize(),
            season.getTransferWindowStartDate(),
            season.getTransferWindowEndDate(),
            isApplicationOpen(season) && isMutableStatus(applicationStatus),
            rosterCount,
            selectedCount,
            applicationStatus,
            application == null ? null : application.getSubmittedAt(),
            application == null ? null : application.getDecisionAt(),
            application == null ? null : application.getDecisionComment(),
            isSubmittable(season, selectedCount, applicationStatus)
        );
    }

    @Transactional(readOnly = true)
    public TeamRepService.TeamRepSeasonPlayersData getSeasonApplicationView(Long userId, Long seasonId) {
        return getSeasonApplicationView(userId, seasonId, null, false);
    }

    @Transactional(readOnly = true)
    public TeamRepService.TeamRepSeasonPlayersData getSeasonApplicationView(Long userId, Long seasonId, Long requestedTeamId, boolean privilegedAccess) {
        TeamScopeContext context = requireApplicationScope(userId, requestedTeamId, privilegedAccess);
        Season season = seasonPlayerService.getSeasonForTeam(context.teamId(), seasonId);
        SeasonApplication application = getOrCreateApplication(context, season, userId, false);
        List<TeamRepService.TeamRepSeasonPlayerData> players = buildDraftPlayers(context, season, application);
        List<Player> available = seasonPlayerService.listAvailablePlayersForSeason(context.teamId(), seasonId);
        Map<Long, String> availablePhotos = loadPlayerPhotos(available);
        List<TeamRepService.TeamRepAvailablePlayerData> availablePlayers = available.stream()
            .map(player -> toAvailablePlayerData(player, availablePhotos.get(player.getId())))
            .toList();
        long selectedCount = players.stream().filter(TeamRepService.TeamRepSeasonPlayerData::selectedForSeason).count();
        return new TeamRepService.TeamRepSeasonPlayersData(
            season.getId(),
            season.getName(),
            season.getApplicationDeadline(),
            season.getStatus(),
            season.getMaxRosterSize(),
            season.getTransferWindowStartDate(),
            season.getTransferWindowEndDate(),
            isApplicationOpen(season) && isMutableStatus(application.getStatus()),
            context.teamId(),
            context.teamName(),
            players,
            availablePlayers,
            application.getId(),
            application.getStatus(),
            application.getSubmittedAt(),
            application.getDecisionAt(),
            application.getDecisionComment(),
            isSubmittable(season, selectedCount, application.getStatus())
        );
    }

    @Transactional
    public TeamRepService.TeamRepSeasonPlayersData addPlayers(Long userId, Long seasonId, List<Long> playerIds) {
        return addPlayers(userId, seasonId, playerIds, null, false);
    }

    @Transactional
    public TeamRepService.TeamRepSeasonPlayersData addPlayers(Long userId, Long seasonId, List<Long> playerIds, Long requestedTeamId, boolean privilegedAccess) {
        TeamScopeContext context = requireApplicationScope(userId, requestedTeamId, privilegedAccess);
        Season season = seasonPlayerService.getSeasonForTeam(context.teamId(), seasonId);
        SeasonApplication application = getOrCreateApplication(context, season, userId, true);
        ensureEditableApplication(application, season);

        Set<Long> ids = new LinkedHashSet<>(playerIds == null ? List.of() : playerIds.stream().filter(java.util.Objects::nonNull).toList());
        Set<Long> existingIds = seasonApplicationPlayerRepository.findAllDetailedByApplicationId(application.getId()).stream()
            .map(item -> item.getPlayer().getId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        long requestedSize = ids.stream().filter(id -> !existingIds.contains(id)).count();
        seasonPlayerService.validateSeasonRosterCapacity(season, context.teamId(), requestedSize, null);

        for (Long playerId : ids) {
            boolean alreadyInRoster = playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(playerId, context.teamId()).isPresent();
            if (!alreadyInRoster) {
                ensureTransferWindowForRosterReassignment(season, context.teamId(), playerId);
                seasonPlayerService.ensurePlayerAssignedToTeam(context.teamId(), playerId, userId);
            }
            if (existingIds.contains(playerId)) {
                continue;
            }
            Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."));
            SeasonApplicationPlayer row = new SeasonApplicationPlayer();
            row.setApplication(application);
            row.setPlayer(player);
            row.setCreatedByUserId(userId);
            seasonApplicationPlayerRepository.save(row);
        }
        touch(application, userId);
        syncApprovedSeasonRoster(application, userId);
        return getSeasonApplicationView(userId, seasonId, requestedTeamId, privilegedAccess);
    }

    @Transactional
    public TeamRepService.TeamRepSeasonPlayersData removePlayer(Long userId, Long seasonId, Long playerId) {
        return removePlayer(userId, seasonId, playerId, null, false);
    }

    @Transactional
    public TeamRepService.TeamRepSeasonPlayersData removePlayer(Long userId, Long seasonId, Long playerId, Long requestedTeamId, boolean privilegedAccess) {
        TeamScopeContext context = requireApplicationScope(userId, requestedTeamId, privilegedAccess);
        Season season = seasonPlayerService.getSeasonForTeam(context.teamId(), seasonId);
        SeasonApplication application = getOrCreateApplication(context, season, userId, true);
        ensureEditableApplication(application, season);
        List<SeasonApplicationPlayer> rows = seasonApplicationPlayerRepository.findAllDetailedByApplicationId(application.getId());
        SeasonApplicationPlayer target = rows.stream()
            .filter(item -> item.getPlayer().getId().equals(playerId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден в сезонной заявке."));
        seasonApplicationPlayerRepository.delete(target);
        touch(application, userId);
        syncApprovedSeasonRoster(application, userId);
        return getSeasonApplicationView(userId, seasonId, requestedTeamId, privilegedAccess);
    }

    @Transactional
    public TeamRepService.TeamRepSeasonPlayersData submit(Long userId, Long seasonId) {
        return submit(userId, seasonId, null, false);
    }

    @Transactional
    public TeamRepService.TeamRepSeasonPlayersData submit(Long userId, Long seasonId, Long requestedTeamId, boolean privilegedAccess) {
        TeamScopeContext context = requireApplicationScope(userId, requestedTeamId, privilegedAccess);
        Season season = seasonPlayerService.getSeasonForTeam(context.teamId(), seasonId);
        SeasonApplication application = getOrCreateApplication(context, season, userId, true);
        ensureEditableApplication(application, season);
        List<SeasonApplicationPlayer> rows = seasonApplicationPlayerRepository.findAllDetailedByApplicationId(application.getId());
        if (!isSubmittable(season, rows.size(), application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заявка пока не готова к отправке на проверку.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        AppUser representativeUser = appUserRepository.findById(userId).orElse(null);
        application.setStatus(SeasonApplicationStatus.SUBMITTED);
        application.setSubmittedAt(now);
        application.setDecisionAt(null);
        application.setDecisionByUserId(null);
        application.setDecisionComment(null);
        application.setRepresentativeUser(representativeUser);
        application.setUpdatedAt(now);
        application.setUpdatedByUserId(userId);
        seasonApplicationRepository.save(application);

        notifyReferees(application);
        return getSeasonApplicationView(userId, seasonId, requestedTeamId, privilegedAccess);
    }

    @Transactional(readOnly = true)
    public ReviewQueueData getReviewQueue(Long seasonId) {
        List<SeasonApplicationStatus> statuses = List.of(
            SeasonApplicationStatus.SUBMITTED,
            SeasonApplicationStatus.APPROVED
        );
        List<SeasonApplication> applications = seasonId == null
            ? seasonApplicationRepository.findAllDetailedByStatusInOrderBySubmittedAtDesc(statuses)
            : seasonApplicationRepository.findAllDetailedBySeasonIdAndStatusInOrderBySubmittedAtDesc(seasonId, statuses);

        Map<Long, Long> playerCounts = applications.isEmpty()
            ? Map.of()
            : seasonApplicationPlayerRepository.countByApplicationIds(applications.stream().map(SeasonApplication::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                    SeasonApplicationPlayerRepository.ApplicationPlayerCount::getApplicationId,
                    SeasonApplicationPlayerRepository.ApplicationPlayerCount::getPlayersCount
                ));
        List<ReviewItemData> items = applications.stream()
            .map(application -> {
                return new ReviewItemData(
                    application.getId(),
                    application.getSeason().getId(),
                    application.getSeason().getName(),
                    application.getTeam().getId(),
                    application.getTeam().getName(),
                    application.getStatus(),
                    application.getSubmittedAt(),
                    application.getDecisionAt(),
                    application.getDecisionComment(),
                    playerCounts.getOrDefault(application.getId(), 0L).intValue(),
                    application.getRepresentativeUser() == null ? null : application.getRepresentativeUser().getId(),
                    application.getRepresentativeUser() == null ? null : application.getRepresentativeUser().getName()
                );
            })
            .toList();
        return new ReviewQueueData(items);
    }

    @Transactional(readOnly = true)
    public ReviewDetailsData getReviewDetails(Long applicationId) {
        SeasonApplication application = seasonApplicationRepository.findDetailedById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезонная заявка не найдена."));
        List<SeasonApplicationPlayer> applicationPlayers =
            seasonApplicationPlayerRepository.findAllDetailedByApplicationId(applicationId);
        Map<Long, String> photos = mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER,
            applicationPlayers.stream().map(item -> item.getPlayer().getId()).toList(),
            MediaAssetService.KIND_PLAYER_PHOTO
        );
        List<ReviewPlayerData> players = applicationPlayers.stream()
            .map(item -> new ReviewPlayerData(
                item.getPlayer().getId(),
                item.getPlayer().getFullName(),
                item.getPlayer().getBirthDate(),
                item.getPlayer().getResidence(),
                item.getPlayer().isGoalkeeper(),
                photos.get(item.getPlayer().getId())
            ))
            .toList();
        return new ReviewDetailsData(
            application.getId(),
            application.getSeason().getId(),
            application.getSeason().getName(),
            application.getTeam().getId(),
            application.getTeam().getName(),
            application.getStatus(),
            application.getSubmittedAt(),
            application.getDecisionAt(),
            application.getDecisionComment(),
            application.getRepresentativeUser() == null ? null : application.getRepresentativeUser().getName(),
            players
        );
    }

    @Transactional
    public ReviewDetailsData approve(Long reviewerUserId, Long applicationId, String decisionComment) {
        SeasonApplication application = requireSubmittedApplication(applicationId);
        List<SeasonApplicationPlayer> rows = seasonApplicationPlayerRepository.findAllDetailedByApplicationId(applicationId);
        List<Long> playerIds = rows.stream().map(item -> item.getPlayer().getId()).toList();
        seasonPlayerService.replaceSeasonPlayers(application.getTeam().getId(), application.getSeason().getId(), playerIds, reviewerUserId);
        setDecision(application, SeasonApplicationStatus.APPROVED, reviewerUserId, decisionComment);
        notifyRepresentative(application, SeasonApplicationStatus.APPROVED, decisionComment);
        return getReviewDetails(applicationId);
    }

    @Transactional
    public ReviewDetailsData returnToTeam(Long reviewerUserId, Long applicationId, String decisionComment) {
        SeasonApplication application = requireSubmittedApplication(applicationId);
        setDecision(application, SeasonApplicationStatus.RETURNED, reviewerUserId, normalizeDecisionComment(decisionComment));
        notifyRepresentative(application, SeasonApplicationStatus.RETURNED, decisionComment);
        return getReviewDetails(applicationId);
    }

    @Transactional
    public ReviewDetailsData reject(Long reviewerUserId, Long applicationId, String decisionComment) {
        SeasonApplication application = requireSubmittedApplication(applicationId);
        String normalizedComment = requireDecisionComment(decisionComment, "Для отклонения заявки нужно указать комментарий.");
        setDecision(application, SeasonApplicationStatus.REJECTED, reviewerUserId, normalizedComment);
        notifyRepresentative(application, SeasonApplicationStatus.REJECTED, normalizedComment);
        return getReviewDetails(applicationId);
    }

    private SeasonApplication requireSubmittedApplication(Long applicationId) {
        SeasonApplication application = seasonApplicationRepository.findDetailedByIdForUpdate(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезонная заявка не найдена."));
        if (application.getStatus() != SeasonApplicationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заявка уже не находится на проверке.");
        }
        return application;
    }

    private void notifyReferees(SeasonApplication application) {
        for (UserRole role : userRoleRepository.findByRole_CodeAndActiveTrue(RoleCode.REFEREE)) {
            AppUser user = role.getUser();
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                notificationEventService.enqueueSeasonApplicationSubmittedToReferee(
                    user,
                    application.getTeam(),
                    application.getSeason(),
                    application.getId(),
                    application.getSubmittedAt()
                );
            }
        }
    }

    private void notifyRepresentative(SeasonApplication application, SeasonApplicationStatus status, String decisionComment) {
        AppUser representative = application.getRepresentativeUser();
        if (representative == null || representative.getEmail() == null || representative.getEmail().isBlank()) {
            return;
        }

        if (status == SeasonApplicationStatus.APPROVED) {
            notificationEventService.enqueueSeasonApplicationApproved(
                representative, application.getSeason(), application.getId(), application.getDecisionAt()
            );
            return;
        }
        if (status == SeasonApplicationStatus.RETURNED) {
            notificationEventService.enqueueSeasonApplicationReturned(
                representative, application.getSeason(), decisionComment, application.getId(), application.getDecisionAt()
            );
            return;
        }
        if (status == SeasonApplicationStatus.REJECTED) {
            notificationEventService.enqueueSeasonApplicationRejected(
                representative, application.getSeason(), decisionComment, application.getId(), application.getDecisionAt()
            );
        }
    }

    private void setDecision(SeasonApplication application, SeasonApplicationStatus status, Long reviewerUserId, String decisionComment) {
        OffsetDateTime now = OffsetDateTime.now();
        application.setStatus(status);
        application.setDecisionAt(now);
        application.setDecisionByUserId(reviewerUserId);
        application.setDecisionComment(decisionComment);
        application.setUpdatedByUserId(reviewerUserId);
        application.setUpdatedAt(now);
        seasonApplicationRepository.save(application);
    }

    private String normalizeDecisionComment(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return "Комментарий не указан.";
        }
        return normalized;
    }

    private String requireDecisionComment(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private List<TeamRepService.TeamRepSeasonPlayerData> buildDraftPlayers(TeamScopeContext context, Season season, SeasonApplication application) {
        Set<Long> selectedIds = seasonApplicationPlayerRepository.findAllDetailedByApplicationId(application.getId()).stream()
            .map(item -> item.getPlayer().getId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (selectedIds.isEmpty()) {
            selectedIds.addAll(seasonPlayerService.getActivePlayerIds(context.teamId(), season.getId()));
        }

        List<Player> rosterPlayers = playerTeamRepository.findCurrentRosterByTeamId(context.teamId()).stream()
            .map(PlayerTeam::getPlayer)
            .toList();
        Set<Long> rosterIds = rosterPlayers.stream().map(Player::getId).collect(java.util.stream.Collectors.toSet());
        List<Player> seasonPlayers = seasonPlayerService.listActiveSeasonPlayers(context.teamId(), season.getId()).stream()
            .map(SeasonPlayer::getPlayer)
            .toList();
        Map<Long, String> photos = loadPlayerPhotos(
            java.util.stream.Stream.concat(rosterPlayers.stream(), seasonPlayers.stream()).distinct().toList()
        );
        Map<Long, TeamRepService.TeamRepSeasonPlayerData> playersById = new LinkedHashMap<>();
        for (Player player : rosterPlayers) {
            playersById.put(
                player.getId(),
                toDraftPlayerData(player, selectedIds.contains(player.getId()), true, photos.get(player.getId()))
            );
        }
        for (Player player : seasonPlayers) {
            playersById.put(player.getId(), toDraftPlayerData(
                player,
                selectedIds.contains(player.getId()),
                rosterIds.contains(player.getId()),
                photos.get(player.getId())
            ));
        }
        return playersById.values().stream()
            .sorted(java.util.Comparator.comparing(TeamRepService.TeamRepSeasonPlayerData::fullName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private TeamRepService.TeamRepSeasonPlayerData toDraftPlayerData(
        Player player,
        boolean selected,
        boolean inRoster,
        String photoDataUrl
    ) {
        return new TeamRepService.TeamRepSeasonPlayerData(
            player.getId(),
            player.getFullName(),
            player.getBirthDate(),
            player.getResidence(),
            player.isGoalkeeper(),
            photoDataUrl,
            selected,
            inRoster
        );
    }

    private TeamRepService.TeamRepAvailablePlayerData toAvailablePlayerData(Player player, String photoDataUrl) {
        return new TeamRepService.TeamRepAvailablePlayerData(
            player.getId(),
            player.getFullName(),
            player.getBirthDate(),
            player.getResidence(),
            player.isGoalkeeper(),
            photoDataUrl
        );
    }

    private Map<Long, String> loadPlayerPhotos(List<Player> players) {
        return mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER,
            players.stream().map(Player::getId).toList(),
            MediaAssetService.KIND_PLAYER_PHOTO
        );
    }

    private SeasonApplication getOrCreateApplication(TeamScopeContext context, Season season, Long actorUserId, boolean persistIfMissing) {
        return findApplication(context.teamId(), season.getId()).orElseGet(() -> {
            SeasonApplication application = new SeasonApplication();
            application.setSeason(season);
            application.setTeam(context.team());
            application.setStatus(SeasonApplicationStatus.DRAFT);
            application.setCreatedByUserId(actorUserId);
            application.setUpdatedByUserId(actorUserId);
            application.setRepresentativeUser(appUserRepository.findById(actorUserId).orElse(null));
            if (persistIfMissing) {
                SeasonApplication saved = seasonApplicationRepository.save(application);
                bootstrapFromActiveSeason(saved, context.teamId(), season.getId(), actorUserId);
                return saved;
            }
            return application;
        });
    }

    private void bootstrapFromActiveSeason(SeasonApplication application, Long teamId, Long seasonId, Long actorUserId) {
        List<SeasonPlayer> activePlayers = seasonPlayerService.listActiveSeasonPlayers(teamId, seasonId);
        for (SeasonPlayer seasonPlayer : activePlayers) {
            SeasonApplicationPlayer row = new SeasonApplicationPlayer();
            row.setApplication(application);
            row.setPlayer(seasonPlayer.getPlayer());
            row.setCreatedByUserId(actorUserId);
            seasonApplicationPlayerRepository.save(row);
        }
    }

    private java.util.Optional<SeasonApplication> findApplication(Long teamId, Long seasonId) {
        return seasonApplicationRepository.findDetailedBySeasonIdAndTeamId(seasonId, teamId);
    }

    private void ensureEditableApplication(SeasonApplication application, Season season) {
        if (!isMutableStatus(application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Заявка в текущем статусе недоступна для редактирования.");
        }
        if (!isApplicationOpen(season)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Срок подачи сезонной заявки истек.");
        }
    }

    private void syncApprovedSeasonRoster(SeasonApplication application, Long actorUserId) {
        if (application.getStatus() != SeasonApplicationStatus.APPROVED) {
            return;
        }

        List<Long> playerIds = seasonApplicationPlayerRepository.findAllDetailedByApplicationId(application.getId()).stream()
            .map(item -> item.getPlayer().getId())
            .toList();
        seasonPlayerService.replaceSeasonPlayers(application.getTeam().getId(), application.getSeason().getId(), playerIds, actorUserId);
    }

    private void ensureTransferWindowForRosterReassignment(Season season, Long teamId, Long playerId) {
        boolean reassignmentNeeded = playerTeamRepository.findByPlayer_IdAndActiveTrue(playerId).stream()
            .anyMatch(membership -> !membership.getTeam().getId().equals(teamId));
        if (!reassignmentNeeded) {
            return;
        }
        if (isTransferWindowOpen(season)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, buildTransferWindowClosedMessage(season));
    }

    private boolean isEditableStatus(SeasonApplicationStatus status) {
        return status == SeasonApplicationStatus.DRAFT || status == SeasonApplicationStatus.RETURNED;
    }

    private boolean isMutableStatus(SeasonApplicationStatus status) {
        return status == SeasonApplicationStatus.DRAFT
            || status == SeasonApplicationStatus.RETURNED
            || status == SeasonApplicationStatus.APPROVED;
    }

    private boolean isSubmittable(Season season, long selectedPlayersCount, SeasonApplicationStatus status) {
        if (!isEditableStatus(status)) {
            return false;
        }
        if (!isApplicationOpen(season)) {
            return false;
        }
        if (selectedPlayersCount <= 0) {
            return false;
        }
        return season.getMaxRosterSize() == null || selectedPlayersCount <= season.getMaxRosterSize();
    }

    private boolean isApplicationOpen(Season season) {
        return season.getStatus() == SeasonStatus.ACTIVE
            && (season.getApplicationDeadline() == null || !LocalDate.now().isAfter(season.getApplicationDeadline()));
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

    private void touch(SeasonApplication application, Long actorUserId) {
        application.setUpdatedByUserId(actorUserId);
        application.setUpdatedAt(OffsetDateTime.now());
        seasonApplicationRepository.save(application);
    }

    private TeamScopeContext requireApplicationScope(Long userId, Long requestedTeamId, boolean privilegedAccess) {
        if (privilegedAccess) {
            if (requestedTeamId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для режима SUPER_ADMIN нужно указать teamId.");
            }
            Team team = teamRepository.findById(requestedTeamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Команда не найдена."));
            return new TeamScopeContext(team.getId(), team.getName(), team);
        }
        UserTeamScope scope = userTeamScopeRepository.findByUser_IdAndActiveTrue(userId).stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Для пользователя не назначена команда."));
        if (!scope.isCanEditApplication()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет прав на редактирование заявки сезона.");
        }
        return new TeamScopeContext(scope.getTeam().getId(), scope.getTeam().getName(), scope.getTeam());
    }

    private record TeamScopeContext(Long teamId, String teamName, com.footballstats.backend.domain.Team team) {}

    public record ReviewQueueData(List<ReviewItemData> items) {}

    public record ReviewItemData(
        Long applicationId,
        Long seasonId,
        String seasonName,
        Long teamId,
        String teamName,
        SeasonApplicationStatus status,
        OffsetDateTime submittedAt,
        OffsetDateTime decisionAt,
        String decisionComment,
        int playersCount,
        Long representativeUserId,
        String representativeName
    ) {}

    public record ReviewDetailsData(
        Long applicationId,
        Long seasonId,
        String seasonName,
        Long teamId,
        String teamName,
        SeasonApplicationStatus status,
        OffsetDateTime submittedAt,
        OffsetDateTime decisionAt,
        String decisionComment,
        String representativeName,
        List<ReviewPlayerData> players
    ) {}

    public record ReviewPlayerData(
        Long id,
        String fullName,
        java.time.LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl
    ) {}
}
