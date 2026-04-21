package com.footballstats.mailer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.mailer.domain.EmailMessage;
import com.footballstats.mailer.domain.NotificationEventRecord;
import com.footballstats.mailer.domain.NotificationTemplateRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class TemplateRenderer {

    private static final Pattern UNRESOLVED_MACRO_PATTERN = Pattern.compile("\\$\\{[^}]+}");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public TemplateRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EmailMessage render(NotificationEventRecord event, NotificationTemplateRecord template) {
        Map<String, Object> variables = parsePayload(event.payloadJson());
        variables.putIfAbsent("eventType", event.eventType());
        variables.putIfAbsent("recipientEmail", event.recipientEmail());
        variables.putIfAbsent("recipientName", event.recipientName());

        String subject = applyVariables(template.subjectTemplate(), variables);
        String body = applyVariables(template.bodyTemplate(), variables);
        return new EmailMessage(subject, body, template.htmlBody());
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            return new LinkedHashMap<>(objectMapper.readValue(payloadJson, MAP_TYPE));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать payload уведомления.", exception);
        }
    }

    private String applyVariables(String template, Map<String, Object> variables) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace("${" + entry.getKey() + "}", value);
        }

        if (UNRESOLVED_MACRO_PATTERN.matcher(rendered).find()) {
            throw new NotificationProcessingException("В шаблоне остались необработанные макросы.");
        }

        return rendered;
    }
}
