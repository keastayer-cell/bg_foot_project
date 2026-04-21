package com.footballstats.mailer.repository;

import com.footballstats.mailer.domain.NotificationEventRecord;
import com.footballstats.mailer.domain.NotificationTemplateRecord;
import com.footballstats.mailer.domain.EmailSendResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationQueueRepository {

    private static final RowMapper<NotificationEventRecord> EVENT_ROW_MAPPER = new RowMapper<>() {
        @Override
        public NotificationEventRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new NotificationEventRecord(
                rs.getLong("id"),
                rs.getString("event_type"),
                rs.getString("template_code"),
                rs.getObject("recipient_user_id") == null ? null : rs.getLong("recipient_user_id"),
                rs.getString("recipient_email"),
                rs.getString("recipient_name"),
                rs.getString("payload_json"),
                rs.getInt("attempt_count"),
                rs.getObject("created_at", OffsetDateTime.class)
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public NotificationQueueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NotificationEventRecord> claimPendingEvents(int batchSize, int staleLockSeconds) {
        return jdbcTemplate.query(
            """
                with candidate as (
                    select e.id
                    from mailer.notification_event e
                    where e.status in ('NEW', 'FAILED')
                      and e.next_retry_at <= now()
                      and (e.locked_at is null or e.locked_at < now() - (? * interval '1 second'))
                    order by e.created_at, e.id
                    limit ?
                    for update skip locked
                )
                update mailer.notification_event e
                   set status = 'PROCESSING',
                       locked_at = now(),
                       processing_started_at = now()
                  from candidate
                 where e.id = candidate.id
                returning e.id,
                          e.event_type,
                          e.template_code,
                          e.recipient_user_id,
                          e.recipient_email,
                          e.recipient_name,
                          e.payload_json::text as payload_json,
                          e.attempt_count,
                          e.created_at
                """,
            EVENT_ROW_MAPPER,
            staleLockSeconds,
            batchSize
        );
    }

    public Optional<NotificationTemplateRecord> findActiveTemplateByCode(String code) {
        List<NotificationTemplateRecord> templates = jdbcTemplate.query(
            """
                select code, subject_template, body_template, body_format
                from mailer.notification_template
                where code = ?
                  and channel = 'EMAIL'
                  and is_active = true
                limit 1
                """,
            (rs, rowNum) -> new NotificationTemplateRecord(
                rs.getString("code"),
                rs.getString("subject_template"),
                rs.getString("body_template"),
                rs.getString("body_format")
            ),
            code
        );
        return templates.stream().findFirst();
    }

    public void markSent(NotificationEventRecord event, String subject, String body, EmailSendResult result) {
        int nextAttemptNumber = event.attemptCount() + 1;

        jdbcTemplate.update(
            """
                update mailer.notification_event
                   set attempt_count = ?,
                       status = 'SENT',
                       processed_at = now(),
                       locked_at = null,
                       processing_started_at = null,
                       last_error = null
                 where id = ?
                """,
            nextAttemptNumber,
            event.id()
        );

        insertEventLog(event.id(), "SENT", "Письмо успешно отправлено");
        insertDeliveryLog(event.id(), nextAttemptNumber, event.recipientEmail(), subject, body, result.transportType(), result.providerMessageId(), "SENT", null);
    }

    public void markFailed(NotificationEventRecord event, String subject, String body, String errorText, int retryLimit, int retryDelaySeconds) {
        int nextAttemptNumber = event.attemptCount() + 1;
        boolean dead = nextAttemptNumber >= retryLimit;
        String nextStatus = dead ? "DEAD" : "FAILED";

        jdbcTemplate.update(
            """
                update mailer.notification_event
                   set attempt_count = ?,
                       status = ?,
                       next_retry_at = case when ? then next_retry_at else now() + (? * interval '1 second') end,
                       processed_at = case when ? then now() else null end,
                       locked_at = null,
                       processing_started_at = null,
                       last_error = ?
                 where id = ?
                """,
            nextAttemptNumber,
            nextStatus,
            dead,
            retryDelaySeconds,
            dead,
            truncateError(errorText),
            event.id()
        );

        insertEventLog(event.id(), nextStatus, dead ? "Событие переведено в DEAD после исчерпания попыток" : "Попытка отправки завершилась ошибкой, событие оставлено на повтор");
        insertDeliveryLog(event.id(), nextAttemptNumber, event.recipientEmail(), subject, body, "INTERNAL", null, nextStatus, errorText);
    }

    private void insertEventLog(Long eventId, String status, String message) {
        jdbcTemplate.update(
            "insert into mailer.notification_event_log (event_id, status, message) values (?, ?, ?)",
            eventId,
            status,
            message
        );
    }

    private void insertDeliveryLog(
        Long eventId,
        int attemptNumber,
        String recipientEmail,
        String subject,
        String body,
        String transportType,
        String providerMessageId,
        String status,
        String errorText
    ) {
        jdbcTemplate.update(
            """
                insert into mailer.notification_delivery_log (
                    event_id,
                    attempt_number,
                    recipient_email,
                    subject_rendered,
                    body_rendered,
                    transport_type,
                    provider_message_id,
                    status,
                    error_text
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            eventId,
            attemptNumber,
            recipientEmail,
            subject,
            body,
            transportType,
            providerMessageId,
            status,
            truncateError(errorText)
        );
    }

    private String truncateError(String errorText) {
        if (errorText == null) {
            return null;
        }
        return errorText.length() <= 4000 ? errorText : errorText.substring(0, 4000);
    }
}
