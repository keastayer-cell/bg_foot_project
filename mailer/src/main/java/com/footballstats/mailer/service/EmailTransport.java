package com.footballstats.mailer.service;

import com.footballstats.mailer.domain.EmailMessage;
import com.footballstats.mailer.domain.EmailSendResult;

public interface EmailTransport {

    EmailSendResult send(String recipientEmail, String recipientName, EmailMessage message);
}
