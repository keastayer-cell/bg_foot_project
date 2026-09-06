package com.footballstats.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_site_notification", schema = "work")
public class SiteNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false, length = 300)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SiteNotificationSeverity severity = SiteNotificationSeverity.INFO;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "source_type", length = 60)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "requires_acknowledgement", nullable = false)
    private boolean requiresAcknowledgement;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    private SiteNotificationAudienceType audienceType = SiteNotificationAudienceType.USERS;

    @Column(name = "audience_value", length = 120)
    private String audienceValue;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public SiteNotificationSeverity getSeverity() { return severity; }
    public void setSeverity(SiteNotificationSeverity severity) { this.severity = severity; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public boolean isRequiresAcknowledgement() { return requiresAcknowledgement; }
    public void setRequiresAcknowledgement(boolean requiresAcknowledgement) { this.requiresAcknowledgement = requiresAcknowledgement; }
    public SiteNotificationAudienceType getAudienceType() { return audienceType; }
    public void setAudienceType(SiteNotificationAudienceType audienceType) { this.audienceType = audienceType; }
    public String getAudienceValue() { return audienceValue; }
    public void setAudienceValue(String audienceValue) { this.audienceValue = audienceValue; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
