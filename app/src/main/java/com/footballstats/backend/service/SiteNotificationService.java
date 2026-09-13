package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.domain.SeasonTransferRequest;
import com.footballstats.backend.domain.SeasonTransferStatus;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonApplication;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.SiteNotification;
import com.footballstats.backend.domain.SiteNotificationAudienceType;
import com.footballstats.backend.domain.SiteNotificationRecipient;
import com.footballstats.backend.domain.SiteNotificationSeverity;
import com.footballstats.backend.domain.SiteNotificationTemplate;
import com.footballstats.backend.domain.NotificationCategory;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.UserTeamScope;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.SiteNotificationRecipientRepository;
import com.footballstats.backend.repository.SiteNotificationRepository;
import com.footballstats.backend.repository.SiteNotificationTemplateRepository;
import com.footballstats.backend.repository.UserRoleRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import com.footballstats.backend.repository.UserFavoriteRepository;
import com.footballstats.backend.domain.FavoriteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SiteNotificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Logger log = LoggerFactory.getLogger(SiteNotificationService.class);

    private final SiteNotificationRepository notificationRepository;
    private final SiteNotificationRecipientRepository recipientRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserTeamScopeRepository userTeamScopeRepository;
    private final SiteNotificationTemplateRepository templateRepository;
    private final NotificationEventService notificationEventService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final UserFavoriteRepository favoriteRepository;

    public SiteNotificationService(
        SiteNotificationRepository notificationRepository,
        SiteNotificationRecipientRepository recipientRepository,
        AppUserRepository appUserRepository,
        UserRoleRepository userRoleRepository,
        UserTeamScopeRepository userTeamScopeRepository,
        SiteNotificationTemplateRepository templateRepository,
        NotificationEventService notificationEventService,
        NotificationPreferenceService notificationPreferenceService,
        UserFavoriteRepository favoriteRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.userTeamScopeRepository = userTeamScopeRepository;
        this.templateRepository = templateRepository;
        this.notificationEventService = notificationEventService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional(readOnly = true)
    public UserNotificationPage getUserNotifications(Long userId, int pageNumber, int pageSize) {
        requireUserId(userId);
        Page<SiteNotificationRecipient> page = recipientRepository.findVisibleForUser(
            userId,
            OffsetDateTime.now(),
            PageRequest.of(normalizePage(pageNumber), normalizePageSize(pageSize))
        );
        return new UserNotificationPage(
            recipientRepository.countUnreadForUser(userId, OffsetDateTime.now()),
            page.getContent().stream().map(this::toUserData).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountData getUnreadCount(Long userId) {
        requireUserId(userId);
        return new UnreadCountData(recipientRepository.countUnreadForUser(userId, OffsetDateTime.now()));
    }

    @Transactional(readOnly = true)
    public PublicNotificationPage getPublicNotifications(int pageNumber, int pageSize) {
        Page<SiteNotification> page = notificationRepository.findVisiblePublic(
            SiteNotificationAudienceType.ALL,
            OffsetDateTime.now(),
            PageRequest.of(normalizePage(pageNumber), normalizePageSize(pageSize))
        );
        return new PublicNotificationPage(
            page.getContent().stream().map(this::toPublicData).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional
    public UserNotificationData markRead(Long userId, Long recipientId) {
        SiteNotificationRecipient recipient = requireRecipient(userId, recipientId);
        if (recipient.getReadAt() == null) {
            recipient.setReadAt(OffsetDateTime.now());
            recipientRepository.save(recipient);
        }
        return toUserData(recipient);
    }

    @Transactional
    public UserNotificationData acknowledge(Long userId, Long recipientId) {
        SiteNotificationRecipient recipient = requireRecipient(userId, recipientId);
        OffsetDateTime now = OffsetDateTime.now();
        if (recipient.getReadAt() == null) recipient.setReadAt(now);
        if (recipient.getAcknowledgedAt() == null) recipient.setAcknowledgedAt(now);
        recipientRepository.save(recipient);
        return toUserData(recipient);
    }

    @Transactional
    public UnreadCountData markAllRead(Long userId) {
        requireUserId(userId);
        recipientRepository.markAllRead(userId, OffsetDateTime.now());
        return new UnreadCountData(0);
    }

    @Transactional
    public AdminNotificationData createManual(Long actorUserId, ManualNotificationCommand command) {
        requireUserId(actorUserId);
        if (command == null) throw new IllegalArgumentException("Параметры уведомления обязательны.");

        SiteNotificationAudienceType audienceType = command.audienceType() == null
            ? SiteNotificationAudienceType.ALL
            : command.audienceType();
        Audience audience = resolveAudience(audienceType, command.roleCode(), command.teamId());
        if (audience.users().isEmpty() && audienceType != SiteNotificationAudienceType.ALL) {
            throw new IllegalArgumentException("В выбранной аудитории нет получателей.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (command.expiresAt() != null && !command.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Срок действия уведомления должен быть в будущем.");
        }

        SiteNotification notification = new SiteNotification();
        notification.setEventType("MANUAL_ANNOUNCEMENT");
        notification.setTitle(requireText(command.title(), "Тема уведомления обязательна.", 180));
        String bodyHtml = sanitizeHtml(requireText(command.body(), "Текст уведомления обязателен.", 12000));
        notification.setBody(bodyHtml);
        notification.setSummary(toSummary(bodyHtml));
        notification.setSeverity(command.severity() == null ? SiteNotificationSeverity.INFO : command.severity());
        notification.setActionUrl(normalizeActionUrl(command.actionUrl()));
        notification.setRequiresAcknowledgement(command.requiresAcknowledgement());
        notification.setAudienceType(audienceType);
        notification.setAudienceValue(audience.label());
        notification.setCreatedByUserId(actorUserId);
        notification.setCreatedAt(now);
        notification.setExpiresAt(command.expiresAt());

        List<AppUser> bellUsers = filterUsers(
            audience.users(), NotificationCategory.NEWS,
            NotificationPreferenceService.Channel.BELL, command.requiresAcknowledgement()
        );
        return toAdminData(deliver(notification, bellUsers));
    }

    @Transactional(readOnly = true)
    public AdminNotificationPage getAdminNotifications(int pageNumber, int pageSize) {
        Page<SiteNotification> page = notificationRepository.findAllByOrderByCreatedAtDescIdDesc(
            PageRequest.of(normalizePage(pageNumber), normalizePageSize(pageSize))
        );
        return new AdminNotificationPage(
            page.getContent().stream().map(this::toAdminData).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional
    public void notifyTransferRequested(SeasonTransferRequest request, Long actorUserId) {
        List<AppUser> recipients = teamUsers(request.getFromTeam().getId(), actorUserId);
        if (recipients.isEmpty()) return;
        deliverFromTemplate(
            "TRANSFER_REQUEST_CREATED", transferVariables(request), recipients, actorUserId,
            "SEASON_TRANSFER_REQUEST", request.getId(), "Участники трансфера"
        );
    }

    @Transactional
    public void notifyTransferDecision(SeasonTransferRequest request, Long actorUserId) {
        Map<Long, AppUser> recipients = new LinkedHashMap<>();
        if (request.getRequestedByUserId() != null && !request.getRequestedByUserId().equals(actorUserId)) {
            appUserRepository.findById(request.getRequestedByUserId())
                .ifPresent(user -> recipients.put(user.getId(), user));
        }
        for (AppUser user : teamUsers(request.getToTeam().getId(), actorUserId)) {
            recipients.put(user.getId(), user);
        }
        for (AppUser user : teamUsers(request.getFromTeam().getId(), actorUserId)) {
            recipients.put(user.getId(), user);
        }
        if (request.getStatus() == SeasonTransferStatus.APPROVED) {
            favoriteRepository.findByTargetTypeAndTargetId(FavoriteType.PLAYER, request.getPlayer().getId()).stream()
                .map(favorite -> favorite.getUser())
                .filter(user -> !user.getId().equals(actorUserId))
                .forEach(user -> recipients.put(user.getId(), user));
        }
        if (recipients.isEmpty()) return;

        deliverFromTemplate(
            "TRANSFER_REQUEST_" + request.getStatus().name(), transferVariables(request),
            List.copyOf(recipients.values()), actorUserId,
            "SEASON_TRANSFER_REQUEST", request.getId(), "Участники трансфера"
        );
    }

    @Transactional
    public void notifySeasonStatusChanged(Season season, SeasonStatus previousStatus, Long actorUserId) {
        if (season == null || season.getStatus() == null || season.getStatus() == previousStatus) return;
        if (season.getStatus() == SeasonStatus.DRAFT) return;

        String eventType = season.getStatus() == SeasonStatus.ACTIVE ? "SEASON_STARTED" : "SEASON_CLOSED";
        deliverFromTemplate(
            eventType, Map.of("seasonName", season.getName()), appUserRepository.findAll(), actorUserId,
            "SEASON", season.getId(), "Все посетители сайта"
        );
    }

    @Transactional
    public void notifyTourPublished(Tour tour, Long actorUserId) {
        deliverFromTemplate(
            "TOUR_PUBLISHED",
            Map.of("tourName", tour.getName(), "seasonName", tour.getSeason().getName()),
            appUserRepository.findAll(), actorUserId, "TOUR", tour.getId(), "Все посетители сайта"
        );
    }

    @Transactional
    public void notifyPlayerSuspended(
        TourMatch match,
        Player player,
        Team team,
        String cause,
        int suspensionMatches,
        Long actorUserId
    ) {
        if (match == null || player == null || team == null || suspensionMatches <= 0) return;
        String normalizedCause = "RED".equals(cause) ? "RED" : "YELLOW";
        String eventType = "PLAYER_SUSPENDED_" + normalizedCause;
        String sourceType = "MATCH_DISCIPLINE_" + normalizedCause + "_PLAYER_" + player.getId();
        if (notificationRepository.existsByEventTypeAndSourceTypeAndSourceId(eventType, sourceType, match.getId())) return;

        List<AppUser> recipients = teamUsers(team.getId(), null);
        if (recipients.isEmpty()) return;

        String tournamentName = match.getTour().getCompetition() == null
            ? match.getTour().getSeason().getName()
            : match.getTour().getCompetition().getName();
        String suspensionMatchesText = suspensionMatches == 1
            ? "следующий матч"
            : suspensionMatches + " " + matchWord(suspensionMatches);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("playerName", player.getFullName());
        variables.put("teamName", team.getName());
        variables.put("tournamentName", tournamentName);
        variables.put("suspensionMatchesText", suspensionMatchesText);
        variables.put("matchId", match.getId());
        variables.put("matchName", match.getHomeTeam().getName() + " — " + match.getAwayTeam().getName());

        deliverFromTemplate(
            eventType, variables, recipients, actorUserId, sourceType, match.getId(),
            "Представители команды «" + team.getName() + "»"
        );
    }

    @Transactional
    public void notifyDisciplineAdjusted(
        com.footballstats.backend.domain.Competition competition,
        Player player,
        Team team,
        int remainingMatches,
        String reason,
        Long actorUserId
    ) {
        List<AppUser> recipients = teamUsers(team.getId(), null);
        if (recipients.isEmpty()) return;
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("playerName", player.getFullName());
        variables.put("teamName", team.getName());
        variables.put("tournamentName", competition.getName());
        variables.put("remainingMatches", remainingMatches);
        variables.put("reason", reason);
        variables.put("seasonId", competition.getSeason().getId());
        variables.put("competitionId", competition.getId());
        deliverFromTemplate("PLAYER_SUSPENSION_ADJUSTED", variables, recipients, actorUserId,
            "DISCIPLINE_ADJUSTMENT_PLAYER_" + player.getId(), competition.getId(),
            "Представители команды «" + team.getName() + "»");
    }

    @Transactional
    public void notifyMatchScheduleChanged(TourMatch match, OffsetDateTime previousKickoff, Long actorUserId) {
        Map<Long, AppUser> recipients = new LinkedHashMap<>();
        teamUsers(match.getHomeTeam().getId(), null).forEach(user -> recipients.put(user.getId(), user));
        teamUsers(match.getAwayTeam().getId(), null).forEach(user -> recipients.put(user.getId(), user));
        for (Long teamId : List.of(match.getHomeTeam().getId(), match.getAwayTeam().getId())) {
            favoriteRepository.findByTargetTypeAndTargetId(FavoriteType.TEAM, teamId).stream()
                .map(favorite -> favorite.getUser()).forEach(user -> recipients.put(user.getId(), user));
        }
        if (recipients.isEmpty()) return;
        String matchName = match.getHomeTeam().getName() + " — " + match.getAwayTeam().getName();
        String summary = switch (match.getScheduleStatus()) {
            case CANCELLED -> "Матч отменён";
            case RESCHEDULED -> "Матч перенесён с " + previousKickoff + " на " + match.getKickoffAt();
            default -> "Расписание матча обновлено";
        };
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("matchName", matchName); variables.put("matchId", match.getId());
        variables.put("changeSummary", summary);
        variables.put("venueName", match.getVenue() == null ? "уточняется" : match.getVenue().getName());
        variables.put("reason", Objects.toString(match.getScheduleChangeReason(), "не указана"));
        deliverFromTemplate("MATCH_SCHEDULE_CHANGED", variables, List.copyOf(recipients.values()), actorUserId,
            "MATCH_SCHEDULE_CHANGE", match.getId(), "Команды матча и подписанные болельщики");
    }

    @Transactional
    public void notifySeasonApplicationSubmitted(SeasonApplication application, List<AppUser> referees) {
        deliverFromTemplate(
            "SEASON_APPLICATION_SUBMITTED_TO_REFEREE", applicationVariables(application, null), referees,
            application.getUpdatedByUserId(), "SEASON_APPLICATION", application.getId(), "Рефери"
        );
    }

    @Transactional
    public void notifySeasonApplicationDecision(
        SeasonApplication application,
        AppUser representative,
        String decisionComment
    ) {
        deliverFromTemplate(
            "SEASON_APPLICATION_" + application.getStatus().name(),
            applicationVariables(application, decisionComment), List.of(representative),
            application.getDecisionByUserId(), "SEASON_APPLICATION", application.getId(),
            "Представитель команды «" + application.getTeam().getName() + "»"
        );
    }

    private void deliverFromTemplate(
        String eventType,
        Map<String, ?> variables,
        List<AppUser> users,
        Long actorUserId,
        String sourceType,
        Long sourceId,
        String audienceValue
    ) {
        SiteNotificationTemplate template = templateRepository.findByCodeAndActiveTrue(eventType).orElse(null);
        if (template == null) {
            log.warn("Автоматическое уведомление {} пропущено: активный шаблон не найден", eventType);
            return;
        }
        SiteNotification notification = new SiteNotification();
        notification.setEventType(eventType);
        notification.setTitle(renderPlain(template.getTitleTemplate(), variables));
        notification.setSummary(renderPlain(template.getSummaryTemplate(), variables));
        notification.setBody(renderHtml(template.getBodyHtmlTemplate(), variables));
        notification.setSeverity(template.getSeverity());
        notification.setActionUrl(normalizeActionUrl(renderPlain(template.getActionUrlTemplate(), variables)));
        notification.setSourceType(sourceType);
        notification.setSourceId(sourceId);
        notification.setRequiresAcknowledgement(template.isRequiresAcknowledgement());
        notification.setAudienceType("PUBLIC".equals(template.getAudienceScope())
            ? SiteNotificationAudienceType.ALL : SiteNotificationAudienceType.USERS);
        notification.setAudienceValue(audienceValue);
        notification.setCreatedByUserId(actorUserId);
        notification.setCreatedAt(OffsetDateTime.now());
        NotificationCategory category = categoryFor(eventType);
        boolean mandatory = template.isRequiresAcknowledgement();
        List<AppUser> bellUsers = filterUsers(
            users, category, NotificationPreferenceService.Channel.BELL, mandatory
        );
        SiteNotification saved = deliver(notification, bellUsers);
        if (template.isEmailEnabled()) {
            Long emailSourceId = sourceType.startsWith("MATCH_DISCIPLINE_") ? saved.getId() : sourceId;
            List<AppUser> emailUsers = filterUsers(
                users, category, NotificationPreferenceService.Channel.EMAIL, mandatory
            );
            for (AppUser user : emailUsers) {
                if (user.getEmail() == null || user.getEmail().isBlank()) continue;
                Map<String, Object> emailVariables = new LinkedHashMap<>();
                variables.forEach(emailVariables::put);
                notificationEventService.enqueueSiteNotificationEmail(
                    eventType, user, emailVariables, notification.getActionUrl(), emailSourceId, actorUserId
                );
            }
        }
    }

    private List<AppUser> filterUsers(
        List<AppUser> users,
        NotificationCategory category,
        NotificationPreferenceService.Channel channel,
        boolean mandatory
    ) {
        return users.stream()
            .filter(user -> notificationPreferenceService.isEnabled(user.getId(), category, channel, mandatory))
            .toList();
    }

    private NotificationCategory categoryFor(String eventType) {
        if (eventType == null) return NotificationCategory.NEWS;
        if (eventType.startsWith("TRANSFER_")) return NotificationCategory.TRANSFERS;
        if (eventType.startsWith("PLAYER_SUSPENDED_") || eventType.startsWith("PLAYER_SUSPENSION_")) return NotificationCategory.DISCIPLINE;
        if (eventType.startsWith("MATCH_")) return NotificationCategory.MATCHES;
        if (eventType.startsWith("SEASON_") || eventType.startsWith("TOUR_")) {
            return NotificationCategory.TOURNAMENTS;
        }
        return NotificationCategory.NEWS;
    }

    private Map<String, Object> transferVariables(SeasonTransferRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("playerName", request.getPlayer().getFullName());
        values.put("fromTeamName", request.getFromTeam().getName());
        values.put("toTeamName", request.getToTeam().getName());
        values.put("seasonId", request.getSeason().getId());
        values.put("seasonName", request.getSeason().getName());
        values.put("decisionComment", Objects.toString(request.getDecisionComment(), "Комментарий не указан."));
        return values;
    }

    private Map<String, Object> applicationVariables(SeasonApplication application, String decisionComment) {
        return Map.of(
            "teamName", application.getTeam().getName(),
            "seasonName", application.getSeason().getName(),
            "decisionComment", Objects.toString(decisionComment, "Комментарий не указан.")
        );
    }

    private SiteNotification deliver(SiteNotification notification, List<AppUser> users) {
        SiteNotification saved = notificationRepository.save(notification);
        OffsetDateTime deliveredAt = OffsetDateTime.now();
        List<SiteNotificationRecipient> recipients = users.stream()
            .collect(java.util.stream.Collectors.toMap(
                AppUser::getId,
                user -> user,
                (first, ignored) -> first,
                LinkedHashMap::new
            ))
            .values().stream()
            .map(user -> {
                SiteNotificationRecipient recipient = new SiteNotificationRecipient();
                recipient.setNotification(saved);
                recipient.setUser(user);
                recipient.setDeliveredAt(deliveredAt);
                return recipient;
            })
            .toList();
        recipientRepository.saveAll(recipients);
        return saved;
    }

    private Audience resolveAudience(SiteNotificationAudienceType type, RoleCode roleCode, Long teamId) {
        return switch (type) {
            case ALL -> new Audience(appUserRepository.findAll(), "Все посетители сайта");
            case ROLE -> {
                if (roleCode == null) throw new IllegalArgumentException("Выберите роль получателей.");
                List<AppUser> users = userRoleRepository.findByRole_CodeAndActiveTrue(roleCode).stream()
                    .map(userRole -> userRole.getUser()).toList();
                yield new Audience(users, "Роль: " + roleCode.name());
            }
            case TEAM -> {
                if (teamId == null) throw new IllegalArgumentException("Выберите команду получателей.");
                List<UserTeamScope> scopes = userTeamScopeRepository.findByTeam_IdAndActiveTrue(teamId);
                List<AppUser> users = scopes.stream()
                    .map(scope -> scope.getUser()).toList();
                String teamName = scopes.isEmpty() ? "ID " + teamId : scopes.getFirst().getTeam().getName();
                yield new Audience(users, "Команда: " + teamName);
            }
            case USERS -> throw new IllegalArgumentException("Ручной выбор отдельных пользователей пока не поддерживается.");
        };
    }

    private List<AppUser> teamUsers(Long teamId, Long excludedUserId) {
        return userTeamScopeRepository.findByTeam_IdAndActiveTrue(teamId).stream()
            .map(scope -> scope.getUser())
            .filter(user -> excludedUserId == null || !excludedUserId.equals(user.getId()))
            .toList();
    }

    private String matchWord(int value) {
        int lastTwo = Math.abs(value) % 100;
        int last = Math.abs(value) % 10;
        if (lastTwo >= 11 && lastTwo <= 14) return "матчей";
        if (last == 1) return "матч";
        if (last >= 2 && last <= 4) return "матча";
        return "матчей";
    }

    private UserNotificationData toUserData(SiteNotificationRecipient recipient) {
        SiteNotification notification = recipient.getNotification();
        return new UserNotificationData(
            recipient.getId(), notification.getId(), notification.getEventType(), notification.getTitle(),
            notification.getSummary(), notification.getBody(), notification.getSeverity(), notification.getActionUrl(),
            notification.isRequiresAcknowledgement(), notification.getCreatedAt(), notification.getExpiresAt(),
            recipient.getReadAt(), recipient.getAcknowledgedAt()
        );
    }

    private AdminNotificationData toAdminData(SiteNotification notification) {
        return new AdminNotificationData(
            notification.getId(), notification.getEventType(), notification.getTitle(), notification.getSummary(), notification.getBody(),
            notification.getSeverity(), notification.getActionUrl(), notification.isRequiresAcknowledgement(),
            notification.getAudienceType(), notification.getAudienceValue(), notification.getCreatedAt(),
            notification.getExpiresAt(), recipientRepository.countByNotification_Id(notification.getId()),
            recipientRepository.countByNotification_IdAndReadAtIsNotNull(notification.getId()),
            recipientRepository.countByNotification_IdAndAcknowledgedAtIsNotNull(notification.getId())
        );
    }

    private PublicNotificationData toPublicData(SiteNotification notification) {
        return new PublicNotificationData(
            notification.getId(), notification.getEventType(), notification.getTitle(), notification.getSummary(), notification.getBody(),
            notification.getSeverity(), notification.getActionUrl(), notification.getCreatedAt(), notification.getExpiresAt()
        );
    }

    private SiteNotificationRecipient requireRecipient(Long userId, Long recipientId) {
        requireUserId(userId);
        return recipientRepository.findByIdAndUser_Id(recipientId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Уведомление не найдено."));
    }

    private void requireUserId(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация.");
    }

    private int normalizePage(int value) { return Math.max(value, 0); }
    private int normalizePageSize(int value) { return Math.min(Math.max(value, 1), MAX_PAGE_SIZE); }

    private String requireText(String value, String message, int maxLength) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(message);
        if (normalized.length() > maxLength) throw new IllegalArgumentException("Превышена допустимая длина текста.");
        return normalized;
    }

    private String normalizeActionUrl(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        if (normalized.isEmpty()) return null;
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            throw new IllegalArgumentException("Ссылка уведомления должна вести на внутреннюю страницу сайта.");
        }
        return normalized;
    }

    private String sanitizeHtml(String value) {
        Safelist safelist = Safelist.relaxed()
            .removeTags("img")
            .addAttributes("a", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto");
        return Jsoup.clean(value, safelist);
    }

    private String toSummary(String bodyHtml) {
        String text = Jsoup.parse(bodyHtml).text().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Текст уведомления не должен быть пустым.");
        return text.length() <= 300 ? text : text.substring(0, 297) + "…";
    }

    private String renderPlain(String template, Map<String, ?> variables) {
        if (template == null) return null;
        String rendered = template;
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            String value = Objects.toString(entry.getValue(), "");
            rendered = rendered
                .replace("${" + entry.getKey() + "}", value)
                .replace("{{" + entry.getKey() + "}}", value);
        }
        return rendered;
    }

    private String renderHtml(String template, Map<String, ?> variables) {
        if (template == null) return null;
        String rendered = template;
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            String value = org.springframework.web.util.HtmlUtils.htmlEscape(Objects.toString(entry.getValue(), ""));
            rendered = rendered
                .replace("${" + entry.getKey() + "}", value)
                .replace("{{" + entry.getKey() + "}}", value);
        }
        return sanitizeHtml(rendered);
    }

    private record Audience(List<AppUser> users, String label) {}

    public record ManualNotificationCommand(
        String title,
        String body,
        SiteNotificationSeverity severity,
        String actionUrl,
        boolean requiresAcknowledgement,
        SiteNotificationAudienceType audienceType,
        RoleCode roleCode,
        Long teamId,
        OffsetDateTime expiresAt
    ) {}

    public record UserNotificationData(
        Long id, Long notificationId, String eventType, String title, String summary, String body,
        SiteNotificationSeverity severity, String actionUrl, boolean requiresAcknowledgement,
        OffsetDateTime createdAt, OffsetDateTime expiresAt, OffsetDateTime readAt, OffsetDateTime acknowledgedAt
    ) {}

    public record UserNotificationPage(
        long unreadCount, List<UserNotificationData> items, int pageNumber, int pageSize,
        long totalElements, int totalPages
    ) {}

    public record UnreadCountData(long unreadCount) {}

    public record PublicNotificationData(
        Long notificationId, String eventType, String title, String summary, String body,
        SiteNotificationSeverity severity, String actionUrl,
        OffsetDateTime createdAt, OffsetDateTime expiresAt
    ) {}

    public record PublicNotificationPage(
        List<PublicNotificationData> items, int pageNumber, int pageSize,
        long totalElements, int totalPages
    ) {}

    public record AdminNotificationData(
        Long id, String eventType, String title, String summary, String body, SiteNotificationSeverity severity,
        String actionUrl, boolean requiresAcknowledgement, SiteNotificationAudienceType audienceType,
        String audienceValue, OffsetDateTime createdAt, OffsetDateTime expiresAt,
        long recipientCount, long readCount, long acknowledgedCount
    ) {}

    public record AdminNotificationPage(
        List<AdminNotificationData> items, int pageNumber, int pageSize, long totalElements, int totalPages
    ) {}
}
