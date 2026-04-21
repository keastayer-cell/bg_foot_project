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
            "MAILER LOG transport -> to='{}' name='{}' html={} subject='{}' body='{}'",
            recipientEmail,
            recipientName,
            message.html(),
            message.subject(),
            message.body()
        );
        return new EmailSendResult("LOG", null);
    }
}
