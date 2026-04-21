package com.footballstats.mailer.domain;

public record EmailMessage(
    String subject,
    String body,
    boolean html
) {
}
