package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.NotificationCategory;
import com.footballstats.backend.domain.UserNotificationPreference;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.UserNotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {
    @Mock private UserNotificationPreferenceRepository repository;
    @Mock private AppUserRepository appUserRepository;
    private NotificationPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(repository, appUserRepository);
    }

    @Test
    void missingPreferencesKeepPreviousEnabledBehavior() {
        when(repository.findByUser_Id(7L)).thenReturn(List.of());

        List<NotificationPreferenceService.Setting> settings = service.getSettings(7L);

        assertThat(settings).hasSize(NotificationCategory.values().length);
        assertThat(settings).allMatch(setting -> setting.bellEnabled() && setting.emailEnabled());
    }

    @Test
    void mandatoryEventCannotBeDisabled() {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setCategory(NotificationCategory.DISCIPLINE);
        preference.setBellEnabled(false);
        preference.setEmailEnabled(false);
        when(repository.findByUser_IdAndCategory(7L, NotificationCategory.DISCIPLINE))
            .thenReturn(Optional.of(preference));

        assertThat(service.isEnabled(
            7L, NotificationCategory.DISCIPLINE, NotificationPreferenceService.Channel.EMAIL, true
        )).isTrue();
        assertThat(service.isEnabled(
            7L, NotificationCategory.DISCIPLINE, NotificationPreferenceService.Channel.EMAIL, false
        )).isFalse();
    }

    @Test
    void updatesOnlySubmittedCategories() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 7L);
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.findByUser_IdAndCategory(7L, NotificationCategory.NEWS)).thenReturn(Optional.empty());
        when(repository.findByUser_Id(7L)).thenReturn(List.of());
        when(repository.save(any(UserNotificationPreference.class))).thenAnswer(call -> call.getArgument(0));

        service.updateSettings(7L, List.of(
            new NotificationPreferenceService.SettingUpdate(NotificationCategory.NEWS, false, true)
        ));

        org.mockito.ArgumentCaptor<UserNotificationPreference> captor =
            org.mockito.ArgumentCaptor.forClass(UserNotificationPreference.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(NotificationCategory.NEWS);
        assertThat(captor.getValue().isBellEnabled()).isFalse();
        assertThat(captor.getValue().isEmailEnabled()).isTrue();
    }
}
