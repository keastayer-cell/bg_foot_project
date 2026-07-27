package com.footballstats.mailer.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailerQueueHealthIndicatorTest {

    @Test
    @SuppressWarnings("unchecked")
    void reportsQueueMetricsWhenDatabaseIsAvailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class))).thenReturn(
            new MailerQueueHealthIndicator.QueueHealthSnapshot(
                3, 1, 2, OffsetDateTime.parse("2026-07-27T12:00:00+03:00")
            )
        );

        var health = new MailerQueueHealthIndicator(jdbcTemplate).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("pending", 3L).containsEntry("dead", 2L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsDownWhenQueueCannotBeRead() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
            .thenThrow(new IllegalStateException("database unavailable"));

        assertThat(new MailerQueueHealthIndicator(jdbcTemplate).health().getStatus()).isEqualTo(Status.DOWN);
    }
}
