package com.footballstats.mailer.domain;

import java.time.OffsetDateTime;

public record NotificationEventRecord(
    Long id,
    String eventType,
    String templateCode,
    Long recipientUserId,
    String recipientEmail,
    String recipientName,
    String payloadJson,
    int attemptCount,
    OffsetDateTime createdAt
) {
}
