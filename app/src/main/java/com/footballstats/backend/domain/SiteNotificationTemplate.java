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
@Table(name = "w_site_notification_template", schema = "work")
public class SiteNotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "title_template", nullable = false, length = 180)
    private String titleTemplate;

    @Column(name = "summary_template", nullable = false, length = 300)
    private String summaryTemplate;

    @Column(name = "body_html_template", nullable = false)
    private String bodyHtmlTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SiteNotificationSeverity severity = SiteNotificationSeverity.INFO;

    @Column(name = "action_url_template", length = 500)
    private String actionUrlTemplate;

    @Column(name = "requires_acknowledgement", nullable = false)
    private boolean requiresAcknowledgement;

    @Column(name = "audience_scope", nullable = false, length = 20)
    private String audienceScope;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitleTemplate() { return titleTemplate; }
    public String getSummaryTemplate() { return summaryTemplate; }
    public String getBodyHtmlTemplate() { return bodyHtmlTemplate; }
    public SiteNotificationSeverity getSeverity() { return severity; }
    public String getActionUrlTemplate() { return actionUrlTemplate; }
    public boolean isRequiresAcknowledgement() { return requiresAcknowledgement; }
    public String getAudienceScope() { return audienceScope; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
