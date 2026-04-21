package com.footballstats.mailer.service;

import com.footballstats.mailer.config.MailerProperties;
import com.footballstats.mailer.domain.EmailMessage;
import com.footballstats.mailer.domain.EmailSendResult;
import com.footballstats.mailer.domain.NotificationEventRecord;
import com.footballstats.mailer.domain.NotificationTemplateRecord;
import com.footballstats.mailer.repository.NotificationQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class NotificationProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessingService.class);

    private final NotificationQueueRepository queueRepository;
    private final TemplateRenderer templateRenderer;
    private final EmailTransport emailTransport;
    private final MailerProperties mailerProperties;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    public NotificationProcessingService(
        NotificationQueueRepository queueRepository,
        TemplateRenderer templateRenderer,
        EmailTransport emailTransport,
        MailerProperties mailerProperties
    ) {
        this.queueRepository = queueRepository;
        this.templateRenderer = templateRenderer;
        this.emailTransport = emailTransport;
        this.mailerProperties = mailerProperties;
    }

    @Scheduled(fixedDelayString = "${mailer.poll-interval-ms:600000}")
    public void pollQueue() {
        processPendingEvents();
    }

    public int processPendingEvents() {
        if (!processing.compareAndSet(false, true)) {
            log.info("Mailer queue processing is already running, skipping duplicate trigger");
            return 0;
        }

        try {
        List<NotificationEventRecord> events = queueRepository.claimPendingEvents(
            mailerProperties.getBatchSize(),
            mailerProperties.getStaleLockSeconds()
        );

        if (events.isEmpty()) {
            return 0;
        }

        log.info("Mailer picked {} notification event(s) for processing", events.size());
        for (NotificationEventRecord event : events) {
            processSingleEvent(event);
        }

        return events.size();
        } finally {
            processing.set(false);
        }
    }

    private void processSingleEvent(NotificationEventRecord event) {
        String renderedSubject = null;
        String renderedBody = null;

        try {
            NotificationTemplateRecord template = queueRepository.findActiveTemplateByCode(event.templateCode())
                .orElseThrow(() -> new IllegalStateException("Активный шаблон не найден для кода " + event.templateCode()));

            EmailMessage message = templateRenderer.render(event, template);
            renderedSubject = message.subject();
            renderedBody = message.body();

            EmailSendResult sendResult = emailTransport.send(event.recipientEmail(), event.recipientName(), message);
            queueRepository.markSent(event, renderedSubject, renderedBody, sendResult);
        } catch (NotificationProcessingException exception) {
            log.warn("Mailer skipped sending event {} ({}) due to processing error: {}", event.id(), event.eventType(), exception.getMessage());
            queueRepository.markFailed(
                event,
                renderedSubject,
                renderedBody,
                exception.getMessage(),
                mailerProperties.getRetryLimit(),
                mailerProperties.getRetryDelaySeconds()
            );
        } catch (Exception exception) {
            log.error("Mailer failed to process event {} ({})", event.id(), event.eventType(), exception);
            queueRepository.markFailed(
                event,
                renderedSubject,
                renderedBody,
                exception.getMessage(),
                mailerProperties.getRetryLimit(),
                mailerProperties.getRetryDelaySeconds()
            );
        }
    }
}