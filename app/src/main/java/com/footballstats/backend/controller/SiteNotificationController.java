package com.footballstats.backend.controller;

import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.domain.SiteNotificationAudienceType;
import com.footballstats.backend.domain.SiteNotificationSeverity;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.SiteNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/notifications")
public class SiteNotificationController {

    private final SiteNotificationService notificationService;

    public SiteNotificationController(SiteNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<SiteNotificationService.UserNotificationPage> list(
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize,
        Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(currentUserId(authentication), pagenum, pagesize));
    }

    @GetMapping("/public")
    public ResponseEntity<SiteNotificationService.PublicNotificationPage> publicList(
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize
    ) {
        return ResponseEntity.ok(notificationService.getPublicNotifications(pagenum, pagesize));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public ResponseEntity<SiteNotificationService.UnreadCountData> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUserId(authentication)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{recipientId}/read")
    public ResponseEntity<SiteNotificationService.UserNotificationData> markRead(
        @PathVariable Long recipientId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.markRead(currentUserId(authentication), recipientId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{recipientId}/acknowledge")
    public ResponseEntity<SiteNotificationService.UserNotificationData> acknowledge(
        @PathVariable Long recipientId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.acknowledge(currentUserId(authentication), recipientId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/read-all")
    public ResponseEntity<SiteNotificationService.UnreadCountData> markAllRead(Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAllRead(currentUserId(authentication)));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @GetMapping("/admin")
    public ResponseEntity<SiteNotificationService.AdminNotificationPage> adminList(
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize
    ) {
        return ResponseEntity.ok(notificationService.getAdminNotifications(pagenum, pagesize));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    @PostMapping("/admin")
    public ResponseEntity<SiteNotificationService.AdminNotificationData> createManual(
        @Valid @RequestBody ManualNotificationRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createManual(
            currentUserId(authentication),
            new SiteNotificationService.ManualNotificationCommand(
                request.title(), request.body(), request.severity(), request.actionUrl(),
                request.requiresAcknowledgement(), request.audienceType(), request.roleCode(),
                request.teamId(), request.expiresAt()
            )
        ));
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) return appUserPrincipal.getUserId();
        return null;
    }

    public record ManualNotificationRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 12000) String body,
        SiteNotificationSeverity severity,
        @Size(max = 500) String actionUrl,
        boolean requiresAcknowledgement,
        SiteNotificationAudienceType audienceType,
        RoleCode roleCode,
        Long teamId,
        OffsetDateTime expiresAt
    ) {}
}
