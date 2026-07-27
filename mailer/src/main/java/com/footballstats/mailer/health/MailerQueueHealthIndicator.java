package com.footballstats.mailer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class MailerQueueHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public MailerQueueHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            QueueHealthSnapshot snapshot = jdbcTemplate.queryForObject(
                """
                    select
                        count(*) filter (where status in ('NEW', 'FAILED')) as pending_count,
                        count(*) filter (where status = 'PROCESSING') as processing_count,
                        count(*) filter (where status = 'DEAD') as dead_count,
                        min(created_at) filter (where status in ('NEW', 'FAILED')) as oldest_pending_at
                    from mailer.notification_event
                    """,
                (rs, rowNum) -> new QueueHealthSnapshot(
                    rs.getLong("pending_count"),
                    rs.getLong("processing_count"),
                    rs.getLong("dead_count"),
                    rs.getObject("oldest_pending_at", OffsetDateTime.class)
                )
            );
            if (snapshot == null) {
                return Health.unknown().withDetail("reason", "queue snapshot is unavailable").build();
            }
            Health.Builder health = Health.up()
                .withDetail("pending", snapshot.pendingCount())
                .withDetail("processing", snapshot.processingCount())
                .withDetail("dead", snapshot.deadCount());
            if (snapshot.oldestPendingAt() != null) {
                health.withDetail("oldestPendingAt", snapshot.oldestPendingAt());
            }
            return health.build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }

    record QueueHealthSnapshot(long pendingCount, long processingCount, long deadCount, OffsetDateTime oldestPendingAt) {}
}
