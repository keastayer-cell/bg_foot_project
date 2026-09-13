package com.footballstats.backend.repository;

import com.footballstats.backend.domain.NotificationCategory;
import com.footballstats.backend.domain.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {
    List<UserNotificationPreference> findByUser_Id(Long userId);
    Optional<UserNotificationPreference> findByUser_IdAndCategory(Long userId, NotificationCategory category);
}
