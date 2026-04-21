package com.footballstats.mailer.domain;

public record NotificationTemplateRecord(
    String code,
    String subjectTemplate,
    String bodyTemplate,
    String bodyFormat
) {
    public boolean htmlBody() {
        return bodyFormat == null || !"TEXT".equalsIgnoreCase(bodyFormat);
    }
}
