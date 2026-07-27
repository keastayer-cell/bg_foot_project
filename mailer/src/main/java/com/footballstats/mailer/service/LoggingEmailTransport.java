package com.footballstats.mailer.service;

import com.footballstats.mailer.domain.EmailMessage;
import com.footballstats.mailer.domain.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mailer.transport", name = "type", havingValue = "log", matchIfMissing = true)
public class LoggingEmailTransport implements EmailTransport {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailTransport.class);

    @Override
    public EmailSendResult send(String recipientEmail, String recipientName, EmailMessage message) {
        log.info(
            "MAILER LOG transport -> to='{}' html={} subjectLength={} bodyLength={}",
            maskEmail(recipientEmail),
            message.html(),
            message.subject() == null ? 0 : message.subject().length(),
            message.body() == null ? 0 : message.body().length()
        );
        return new EmailSendResult("LOG", null);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int separator = email.indexOf('@');
        String localPart = email.substring(0, separator);
        String domain = email.substring(separator + 1);
        return (localPart.isEmpty() ? "*" : localPart.substring(0, 1) + "***") + "@" + domain;
    }
}
