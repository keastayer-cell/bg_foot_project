package com.footballstats.mailer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.mailer.domain.EmailMessage;
import com.footballstats.mailer.domain.NotificationEventRecord;
import com.footballstats.mailer.domain.NotificationTemplateRecord;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer(new ObjectMapper());

    @Test
    void rendersPayloadAndEventVariables() {
        NotificationEventRecord event = event("""
            {"teamName":"Волга","seasonName":"Лето 2026"}
            """);
        NotificationTemplateRecord template = new NotificationTemplateRecord(
            "season-application",
            "Заявка ${teamName}",
            "Здравствуйте, ${recipientName}. ${teamName}: ${seasonName}. Тип: ${eventType}",
            "HTML"
        );

        EmailMessage message = renderer.render(event, template);

        assertThat(message.subject()).isEqualTo("Заявка Волга");
        assertThat(message.body()).contains("Иван", "Волга", "Лето 2026", "SEASON_APPLICATION");
        assertThat(message.html()).isTrue();
    }

    @Test
    void rejectsUnresolvedMacros() {
        NotificationTemplateRecord template = new NotificationTemplateRecord(
            "broken",
            "Привет ${missingValue}",
            "body",
            "TEXT"
        );

        assertThatThrownBy(() -> renderer.render(event("{}"), template))
            .isInstanceOf(NotificationProcessingException.class)
            .hasMessageContaining("макросы");
    }

    private NotificationEventRecord event(String payloadJson) {
        return new NotificationEventRecord(
            1L,
            "SEASON_APPLICATION",
            "season-application",
            10L,
            "ivan@example.com",
            "Иван",
            payloadJson,
            0,
            OffsetDateTime.parse("2026-07-27T12:00:00+03:00")
        );
    }
}
