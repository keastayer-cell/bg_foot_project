package com.footballstats.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_site_notification_recipient", schema = "work")
public class SiteNotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private SiteNotification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "delivered_at", nullable = false)
    private OffsetDateTime deliveredAt = OffsetDateTime.now();

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    public Long getId() { return id; }
    public SiteNotification getNotification() { return notification; }
    public void setNotification(SiteNotification notification) { this.notification = notification; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(OffsetDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
    public OffsetDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
