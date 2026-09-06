package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.SiteNotification;
import com.footballstats.backend.domain.SiteNotificationAudienceType;
import com.footballstats.backend.domain.SiteNotificationRecipient;
import com.footballstats.backend.domain.SiteNotificationSeverity;
import com.footballstats.backend.domain.SiteNotificationTemplate;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.SiteNotificationRecipientRepository;
import com.footballstats.backend.repository.SiteNotificationRepository;
import com.footballstats.backend.repository.SiteNotificationTemplateRepository;
import com.footballstats.backend.repository.UserRoleRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteNotificationServiceTest {

    @Mock private SiteNotificationRepository notificationRepository;
    @Mock private SiteNotificationRecipientRepository recipientRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserTeamScopeRepository userTeamScopeRepository;
    @Mock private SiteNotificationTemplateRepository templateRepository;
    @Mock private NotificationEventService notificationEventService;

    private SiteNotificationService service;

    @BeforeEach
    void setUp() {
        service = new SiteNotificationService(
            notificationRepository,
            recipientRepository,
            appUserRepository,
            userRoleRepository,
            userTeamScopeRepository,
            templateRepository,
            notificationEventService
        );
    }

    @Test
    void manualBroadcastCreatesOneRecipientStatePerUser() {
        AppUser first = user(1L, "one@test.local");
        AppUser second = user(2L, "two@test.local");
        when(appUserRepository.findAll()).thenReturn(List.of(first, second));
        when(notificationRepository.save(any(SiteNotification.class))).thenAnswer(invocation -> {
            SiteNotification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 10L);
            return notification;
        });

        service.createManual(99L, new SiteNotificationService.ManualNotificationCommand(
            "Старт сезона", "Первый тур начнётся в субботу.", SiteNotificationSeverity.IMPORTANT,
            "/", true, SiteNotificationAudienceType.ALL, null, null, null
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SiteNotificationRecipient>> captor = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(item -> item.getUser().getId()).containsExactly(1L, 2L);
    }

    @Test
    void acknowledgementAlsoMarksUnreadNotificationAsRead() {
        AppUser recipientUser = user(5L, "recipient@test.local");
        SiteNotification notification = new SiteNotification();
        ReflectionTestUtils.setField(notification, "id", 7L);
        notification.setEventType("MANUAL_ANNOUNCEMENT");
        notification.setTitle("Важно");
        notification.setBody("Ознакомьтесь");
        SiteNotificationRecipient recipient = new SiteNotificationRecipient();
        ReflectionTestUtils.setField(recipient, "id", 8L);
        recipient.setNotification(notification);
        recipient.setUser(recipientUser);
        when(recipientRepository.findByIdAndUser_Id(8L, 5L)).thenReturn(Optional.of(recipient));

        SiteNotificationService.UserNotificationData result = service.acknowledge(5L, 8L);

        assertThat(result.readAt()).isNotNull();
        assertThat(result.acknowledgedAt()).isNotNull();
        verify(recipientRepository).save(recipient);
    }

    @Test
    void publicFeedReturnsAnnouncementsForAllVisitors() {
        SiteNotification notification = new SiteNotification();
        ReflectionTestUtils.setField(notification, "id", 11L);
        notification.setEventType("SEASON_STARTED");
        notification.setTitle("Сезон начался");
        notification.setBody("Расписание опубликовано.");
        notification.setAudienceType(SiteNotificationAudienceType.ALL);
        when(notificationRepository.findVisiblePublic(eq(SiteNotificationAudienceType.ALL), any(), any()))
            .thenReturn(new PageImpl<>(List.of(notification)));

        SiteNotificationService.PublicNotificationPage result = service.getPublicNotifications(0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().notificationId()).isEqualTo(11L);
    }

    @Test
    void readNotificationRemainsInPagedHistory() {
        AppUser recipientUser = user(5L, "recipient@test.local");
        SiteNotification notification = new SiteNotification();
        ReflectionTestUtils.setField(notification, "id", 7L);
        notification.setEventType("MANUAL_ANNOUNCEMENT");
        notification.setTitle("Прочитано");
        notification.setSummary("Уведомление остаётся в истории");
        notification.setBody("<p>Уведомление остаётся в истории</p>");
        SiteNotificationRecipient recipient = new SiteNotificationRecipient();
        ReflectionTestUtils.setField(recipient, "id", 8L);
        recipient.setNotification(notification);
        recipient.setUser(recipientUser);
        recipient.setReadAt(OffsetDateTime.now());
        when(recipientRepository.findVisibleForUser(eq(5L), any(), any()))
            .thenReturn(new PageImpl<>(List.of(recipient)));

        SiteNotificationService.UserNotificationPage result = service.getUserNotifications(5L, 0, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().readAt()).isNotNull();
        assertThat(result.unreadCount()).isZero();
    }

    @Test
    void activeSeasonCreatesPublicAnnouncementEvenWithoutRegisteredUsers() {
        Season season = new Season();
        ReflectionTestUtils.setField(season, "id", 12L);
        season.setName("2026");
        season.setStatus(SeasonStatus.ACTIVE);
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(templateRepository.findByCodeAndActiveTrue("SEASON_STARTED"))
            .thenReturn(Optional.of(template("SEASON_STARTED", "Сезон ${seasonName} начался", "Сезон открыт")));
        when(notificationRepository.save(any(SiteNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.notifySeasonStatusChanged(season, SeasonStatus.DRAFT, 99L);

        ArgumentCaptor<SiteNotification> captor = ArgumentCaptor.forClass(SiteNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("SEASON_STARTED");
        assertThat(captor.getValue().getAudienceType()).isEqualTo(SiteNotificationAudienceType.ALL);
    }

    private SiteNotificationTemplate template(String code, String title, String summary) {
        SiteNotificationTemplate template = new SiteNotificationTemplate();
        ReflectionTestUtils.setField(template, "code", code);
        ReflectionTestUtils.setField(template, "titleTemplate", title);
        ReflectionTestUtils.setField(template, "summaryTemplate", summary);
        ReflectionTestUtils.setField(template, "bodyHtmlTemplate", "<p>" + summary + "</p>");
        ReflectionTestUtils.setField(template, "severity", SiteNotificationSeverity.INFO);
        ReflectionTestUtils.setField(template, "actionUrlTemplate", "/");
        ReflectionTestUtils.setField(template, "audienceScope", "PUBLIC");
        return template;
    }

    private AppUser user(Long id, String email) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail(email);
        user.setName(email);
        user.setPasswordHash("hash");
        return user;
    }
}
