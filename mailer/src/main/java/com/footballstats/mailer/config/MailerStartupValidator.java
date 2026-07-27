package com.footballstats.mailer.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Component;

@Component
public class MailerStartupValidator implements InitializingBean {

    private final MailerProperties mailerProperties;
    private final MailProperties mailProperties;

    public MailerStartupValidator(MailerProperties mailerProperties, MailProperties mailProperties) {
        this.mailerProperties = mailerProperties;
        this.mailProperties = mailProperties;
    }

    @Override
    public void afterPropertiesSet() {
        if (mailerProperties.getMaxRetryDelaySeconds() < mailerProperties.getRetryDelaySeconds()) {
            throw new IllegalStateException("MAILER_MAX_RETRY_DELAY_SECONDS не может быть меньше MAILER_RETRY_DELAY_SECONDS.");
        }
        if (!"smtp".equalsIgnoreCase(mailerProperties.getTransport().getType())) {
            return;
        }
        if (isBlank(mailProperties.getUsername()) || isBlank(mailProperties.getPassword())) {
            throw new IllegalStateException("Для MAILER_TRANSPORT_TYPE=smtp обязательны MAILER_SMTP_USERNAME и MAILER_SMTP_PASSWORD.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
