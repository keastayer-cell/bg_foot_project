package com.footballstats.mailer.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailerStartupValidatorTest {

    @Test
    void logModeDoesNotRequireSmtpCredentials() {
        MailerProperties properties = new MailerProperties();
        properties.getTransport().setType("log");

        assertThatCode(() -> new MailerStartupValidator(properties, new MailProperties()).afterPropertiesSet())
            .doesNotThrowAnyException();
    }

    @Test
    void smtpModeRequiresUsernameAndPassword() {
        MailerProperties properties = new MailerProperties();
        properties.getTransport().setType("smtp");

        assertThatThrownBy(() -> new MailerStartupValidator(properties, new MailProperties()).afterPropertiesSet())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MAILER_SMTP_USERNAME", "MAILER_SMTP_PASSWORD");
    }
}
