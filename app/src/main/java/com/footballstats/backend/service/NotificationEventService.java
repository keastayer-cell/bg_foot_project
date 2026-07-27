package com.footballstats.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationEventService {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean mailerTriggerEnabled;
    private final URI mailerTriggerUri;

    public NotificationEventService(
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper,
        @Value("${mailer.trigger.enabled:true}") boolean mailerTriggerEnabled,
        @Value("${mailer.trigger.url:http://127.0.0.1:8090/internal/notifications/process}") String mailerTriggerUrl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        this.mailerTriggerEnabled = mailerTriggerEnabled;
        this.mailerTriggerUri = URI.create(mailerTriggerUrl);
    }

    public Long enqueueUserRegistered(AppUser user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", user.getName());
        payload.put("recipientEmail", user.getEmail());
        payload.put("userId", user.getId());
        payload.put("userName", user.getName());

        return enqueueEvent("USER_REGISTERED", user.getId(), payload, user.getId(), "USER_REGISTERED:" + user.getId(), true);
    }

    public Long enqueueTeamRepRoleGranted(AppUser user, Team team) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", user.getName());
        payload.put("recipientEmail", user.getEmail());
        payload.put("userId", user.getId());
        payload.put("teamName", team.getName());
        payload.put("contactEmail", "info@bgfoot.ru");

        return enqueueEvent(
            "TEAM_REP_ROLE_GRANTED", user.getId(), payload, user.getId(),
            "TEAM_REP_ROLE_GRANTED:" + user.getId() + ":" + team.getId(), true
        );
    }

    public Long enqueueRefereeRoleGranted(AppUser user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", user.getName());
        payload.put("recipientEmail", user.getEmail());
        payload.put("userId", user.getId());
        payload.put("contactEmail", "info@bgfoot.ru");

        return enqueueEvent("REFEREE_ROLE_GRANTED", user.getId(), payload, user.getId(), "REFEREE_ROLE_GRANTED:" + user.getId(), true);
    }

    public Long enqueuePasswordResetRequested(AppUser user, String resetLink, OffsetDateTime expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", user.getName());
        payload.put("recipientEmail", user.getEmail());
        payload.put("userId", user.getId());
        payload.put("resetLink", resetLink);
        payload.put("expiresAt", expiresAt == null ? null : expiresAt.toString());
        payload.put("contactEmail", "info@bgfoot.ru");

        return enqueueEvent(
            "PASSWORD_RESET_REQUESTED", user.getId(), payload, user.getId(),
            "PASSWORD_RESET_REQUESTED:" + user.getId() + ":" + sha256(resetLink), true
        );
    }

    public Long enqueueSeasonApplicationSubmittedToReferee(
        AppUser refereeUser,
        Team team,
        Season season,
        Long applicationId,
        OffsetDateTime submittedAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", refereeUser.getName());
        payload.put("recipientEmail", refereeUser.getEmail());
        payload.put("teamName", team.getName());
        payload.put("seasonName", season.getName());
        return enqueueEvent(
            "SEASON_APPLICATION_SUBMITTED_TO_REFEREE", refereeUser.getId(), payload, null,
            eventVersionKey("SEASON_APPLICATION_SUBMITTED_TO_REFEREE", applicationId, refereeUser.getId(), submittedAt), true
        );
    }

    public Long enqueueSeasonApplicationApproved(
        AppUser representativeUser,
        Season season,
        Long applicationId,
        OffsetDateTime decisionAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", representativeUser.getName());
        payload.put("recipientEmail", representativeUser.getEmail());
        payload.put("seasonName", season.getName());
        return enqueueEvent(
            "SEASON_APPLICATION_APPROVED", representativeUser.getId(), payload, representativeUser.getId(),
            eventVersionKey("SEASON_APPLICATION_APPROVED", applicationId, representativeUser.getId(), decisionAt), true
        );
    }

    public Long enqueueSeasonApplicationReturned(
        AppUser representativeUser,
        Season season,
        String decisionComment,
        Long applicationId,
        OffsetDateTime decisionAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", representativeUser.getName());
        payload.put("recipientEmail", representativeUser.getEmail());
        payload.put("seasonName", season.getName());
        payload.put("decisionComment", decisionComment == null ? "" : decisionComment);
        return enqueueEvent(
            "SEASON_APPLICATION_RETURNED", representativeUser.getId(), payload, representativeUser.getId(),
            eventVersionKey("SEASON_APPLICATION_RETURNED", applicationId, representativeUser.getId(), decisionAt), true
        );
    }

    public Long enqueueSeasonApplicationRejected(
        AppUser representativeUser,
        Season season,
        String decisionComment,
        Long applicationId,
        OffsetDateTime decisionAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientName", representativeUser.getName());
        payload.put("recipientEmail", representativeUser.getEmail());
        payload.put("seasonName", season.getName());
        payload.put("decisionComment", decisionComment == null ? "" : decisionComment);
        return enqueueEvent(
            "SEASON_APPLICATION_REJECTED", representativeUser.getId(), payload, representativeUser.getId(),
            eventVersionKey("SEASON_APPLICATION_REJECTED", applicationId, representativeUser.getId(), decisionAt), true
        );
    }

    private Long enqueueEvent(
        String eventType,
        Long recipientUserId,
        Map<String, Object> payload,
        Long createdByUserId,
        String deduplicationKey,
        boolean triggerImmediately
    ) {
        String payloadJson = toJson(payload);

        Long eventId = jdbcTemplate.queryForObject(
            "select mailer.enqueue_event(?, ?, cast(? as jsonb), ?, ?)",
            Long.class,
            eventType,
            recipientUserId,
            payloadJson,
            createdByUserId,
            deduplicationKey
        );

        triggerMailerAfterCommit(triggerImmediately);
        return eventId;
    }

    private void triggerMailerAfterCommit(boolean triggerImmediately) {
        if (!mailerTriggerEnabled || !triggerImmediately) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerMailerNow();
                }
            });
            return;
        }

        triggerMailerNow();
    }

    private void triggerMailerNow() {
        try {
            HttpRequest request = HttpRequest.newBuilder(mailerTriggerUri)
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        log.warn("Не удалось сразу триггернуть mailer после постановки события в очередь: {}", throwable.getMessage());
                        return;
                    }

                    if (response.statusCode() >= 400) {
                        log.warn("Mailer trigger endpoint returned non-success status: {}", response.statusCode());
                    }
                });
        } catch (Exception exception) {
            log.warn("Не удалось подготовить вызов mailer trigger endpoint: {}", exception.getMessage());
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось сериализовать payload уведомления.", exception);
        }
    }

    private String eventVersionKey(String eventType, Long applicationId, Long recipientUserId, OffsetDateTime versionAt) {
        return eventType + ":" + applicationId + ":" + recipientUserId + ":" + versionAt;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен для ключа дедупликации.", exception);
        }
    }
}
