package com.footballstats.mailer.service;

import com.footballstats.mailer.config.MailerProperties;
import com.footballstats.mailer.domain.EmailMessage;
import com.footballstats.mailer.domain.EmailSendResult;
import com.footballstats.mailer.domain.NotificationEventRecord;
import com.footballstats.mailer.domain.NotificationTemplateRecord;
import com.footballstats.mailer.repository.NotificationQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {

    @Mock private NotificationQueueRepository queueRepository;
    @Mock private TemplateRenderer templateRenderer;
    @Mock private EmailTransport emailTransport;

    private MailerProperties properties;
    private NotificationProcessingService service;

    @BeforeEach
    void setUp() {
        properties = new MailerProperties();
        properties.setBatchSize(2);
        properties.setRetryLimit(3);
        properties.setRetryDelaySeconds(10);
        properties.setMaxRetryDelaySeconds(120);
        service = new NotificationProcessingService(queueRepository, templateRenderer, emailTransport, properties);
    }

    @Test
    void claimsOneEventAtATimeAndMarksSuccessfulDelivery() {
        NotificationEventRecord event = event(0);
        NotificationTemplateRecord template = new NotificationTemplateRecord("CODE", "subject", "body", "HTML");
        EmailMessage message = new EmailMessage("Rendered", "<p>Body</p>", true);
        when(queueRepository.claimPendingEvents(1, 300))
            .thenReturn(List.of(event))
            .thenReturn(List.of());
        when(queueRepository.findActiveTemplateByCode("CODE")).thenReturn(Optional.of(template));
        when(templateRenderer.render(event, template)).thenReturn(message);
        when(emailTransport.send("user@example.com", "User", message)).thenReturn(new EmailSendResult("SMTP", "provider-1"));
        when(queueRepository.markSent(event, "Rendered", "<p>Body</p>", new EmailSendResult("SMTP", "provider-1")))
            .thenReturn(true);

        assertThat(service.processPendingEvents()).isEqualTo(1);
        verify(queueRepository).markSent(event, "Rendered", "<p>Body</p>", new EmailSendResult("SMTP", "provider-1"));
    }

    @Test
    void schedulesRetryWhenTransportFails() {
        NotificationEventRecord event = event(1);
        NotificationTemplateRecord template = new NotificationTemplateRecord("CODE", "subject", "body", "TEXT");
        EmailMessage message = new EmailMessage("Rendered", "Body", false);
        when(queueRepository.claimPendingEvents(1, 300))
            .thenReturn(List.of(event))
            .thenReturn(List.of());
        when(queueRepository.findActiveTemplateByCode("CODE")).thenReturn(Optional.of(template));
        when(templateRenderer.render(event, template)).thenReturn(message);
        when(emailTransport.send("user@example.com", "User", message)).thenThrow(new IllegalStateException("SMTP unavailable"));

        assertThat(service.processPendingEvents()).isEqualTo(1);
        verify(queueRepository).markFailed(event, "Rendered", "Body", "SMTP unavailable", 3, 10, 120);
    }

    private NotificationEventRecord event(int attemptCount) {
        return new NotificationEventRecord(
            7L, "EVENT", "CODE", 10L, "user@example.com", "User", "{}",
            attemptCount, OffsetDateTime.parse("2026-07-27T12:00:00+03:00"), "lock-token"
        );
    }
}
