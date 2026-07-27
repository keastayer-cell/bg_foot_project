package com.footballstats.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.domain.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventServiceTest {

    @Test
    void registrationUsesStableDeduplicationKey() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(11L);
        NotificationEventService service = new NotificationEventService(
            jdbcTemplate, new ObjectMapper(), false, "http://127.0.0.1:8090/internal/notifications/process"
        );
        AppUser user = user(42L);

        assertThat(service.enqueueUserRegistered(user)).isEqualTo(11L);

        verify(jdbcTemplate).queryForObject(
            eq("select mailer.enqueue_event(?, ?, cast(? as jsonb), ?, ?)"),
            eq(Long.class),
            eq("USER_REGISTERED"),
            eq(42L),
            anyString(),
            eq(42L),
            eq("USER_REGISTERED:42")
        );
    }

    @Test
    void passwordResetDeduplicationKeyDoesNotContainResetToken() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(12L);
        NotificationEventService service = new NotificationEventService(
            jdbcTemplate, new ObjectMapper(), false, "http://127.0.0.1:8090/internal/notifications/process"
        );
        String resetLink = "https://example.test/reset-password?token=top-secret";

        service.enqueuePasswordResetRequested(user(7L), resetLink, OffsetDateTime.parse("2026-07-27T12:30:00+03:00"));

        org.mockito.ArgumentCaptor<Object[]> arguments = org.mockito.ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), arguments.capture());
        String deduplicationKey = (String) arguments.getValue()[4];
        assertThat(deduplicationKey)
            .startsWith("PASSWORD_RESET_REQUESTED:7:")
            .doesNotContain("top-secret", resetLink);
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail("user@example.com");
        user.setName("User");
        return user;
    }
}
