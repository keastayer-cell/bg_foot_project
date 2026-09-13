package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.NotificationCategory;
import com.footballstats.backend.domain.UserNotificationPreference;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.UserNotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationPreferenceService {
    private final UserNotificationPreferenceRepository repository;
    private final AppUserRepository appUserRepository;

    public NotificationPreferenceService(
        UserNotificationPreferenceRepository repository,
        AppUserRepository appUserRepository
    ) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<Setting> getSettings(Long userId) {
        Map<NotificationCategory, UserNotificationPreference> stored = new EnumMap<>(NotificationCategory.class);
        repository.findByUser_Id(userId).forEach(value -> stored.put(value.getCategory(), value));
        return java.util.Arrays.stream(NotificationCategory.values())
            .map(category -> toSetting(category, stored.get(category)))
            .toList();
    }

    @Transactional
    public List<Setting> updateSettings(Long userId, List<SettingUpdate> updates) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
        Map<NotificationCategory, SettingUpdate> requested = new EnumMap<>(NotificationCategory.class);
        if (updates != null) {
            for (SettingUpdate update : updates) {
                if (update != null && update.category() != null) requested.put(update.category(), update);
            }
        }
        for (NotificationCategory category : NotificationCategory.values()) {
            SettingUpdate update = requested.get(category);
            if (update == null) continue;
            UserNotificationPreference preference = repository.findByUser_IdAndCategory(userId, category)
                .orElseGet(() -> {
                    UserNotificationPreference created = new UserNotificationPreference();
                    created.setUser(user);
                    created.setCategory(category);
                    return created;
                });
            preference.setBellEnabled(update.bellEnabled());
            preference.setEmailEnabled(update.emailEnabled());
            preference.setUpdatedAt(OffsetDateTime.now());
            repository.save(preference);
        }
        return getSettings(userId);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId, NotificationCategory category, Channel channel, boolean mandatory) {
        if (mandatory) return true;
        return repository.findByUser_IdAndCategory(userId, category)
            .map(value -> channel == Channel.BELL ? value.isBellEnabled() : value.isEmailEnabled())
            .orElse(true);
    }

    private Setting toSetting(NotificationCategory category, UserNotificationPreference value) {
        return new Setting(category, value == null || value.isBellEnabled(), value == null || value.isEmailEnabled());
    }

    public enum Channel { BELL, EMAIL }
    public record Setting(NotificationCategory category, boolean bellEnabled, boolean emailEnabled) {}
    public record SettingUpdate(NotificationCategory category, boolean bellEnabled, boolean emailEnabled) {}
}
