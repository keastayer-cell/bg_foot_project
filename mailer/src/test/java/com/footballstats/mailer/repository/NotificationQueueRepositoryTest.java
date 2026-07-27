package com.footballstats.mailer.repository;

import com.footballstats.mailer.domain.NotificationEventRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationQueueRepositoryTest {

    @Test
    void retryDelayUsesExponentialBackoffAndCap() {
        assertThat(NotificationQueueRepository.calculateRetryDelay(60, 3600, 1)).isEqualTo(60);
        assertThat(NotificationQueueRepository.calculateRetryDelay(60, 3600, 2)).isEqualTo(120);
        assertThat(NotificationQueueRepository.calculateRetryDelay(60, 3600, 4)).isEqualTo(480);
        assertThat(NotificationQueueRepository.calculateRetryDelay(60, 300, 8)).isEqualTo(300);
    }

    @Test
    @SuppressWarnings("unchecked")
    void claimQueryReclaimsStaleProcessingEvents() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        new NotificationQueueRepository(jdbcTemplate).claimPendingEvents(1, 300);

        org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(sql.getValue())
            .contains("e.status = 'PROCESSING'", "for update skip locked", "lock_token = ?");
    }

    @Test
    void finalFailureMovesEventToDeadAndWritesAttemptLogs() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        NotificationQueueRepository repository = new NotificationQueueRepository(jdbcTemplate);
        NotificationEventRecord event = new NotificationEventRecord(
            5L, "EVENT", "CODE", 2L, "user@example.com", "User", "{}",
            2, OffsetDateTime.parse("2026-07-27T12:00:00+03:00"), "owner-token"
        );

        boolean updated = repository.markFailed(event, "subject", "body", "failure", 3, 60, 3600);

        assertThat(updated).isTrue();
        org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate, org.mockito.Mockito.times(3))
            .update(sql.capture(), any(Object[].class));
        assertThat(sql.getAllValues().getFirst())
            .contains("status = ?", "lock_token = ?", "processed_at = case when ? then now()");
        assertThat(sql.getAllValues().get(1)).contains("notification_event_log");
        assertThat(sql.getAllValues().get(2)).contains("notification_delivery_log", "on conflict");
    }
}
