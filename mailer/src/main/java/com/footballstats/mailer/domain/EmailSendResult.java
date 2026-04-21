package com.footballstats.mailer.domain;

public record EmailSendResult(
    String transportType,
    String providerMessageId
) {
}
