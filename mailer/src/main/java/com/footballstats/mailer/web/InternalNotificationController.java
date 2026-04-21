package com.footballstats.mailer.web;

import com.footballstats.mailer.service.NotificationProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final NotificationProcessingService notificationProcessingService;

    public InternalNotificationController(NotificationProcessingService notificationProcessingService) {
        this.notificationProcessingService = notificationProcessingService;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processQueueNow() {
        int processedCount = notificationProcessingService.processPendingEvents();
        return ResponseEntity.accepted().body(Map.of(
            "accepted", true,
            "processedCount", processedCount
        ));
    }
}