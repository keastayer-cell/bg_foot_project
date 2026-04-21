package com.footballstats.mailer.service;

import com.footballstats.mailer.config.MailerProperties;
import com.footballstats.mailer.domain.EmailMessage;
import com.footballstats.mailer.domain.EmailSendResult;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mailer.transport", name = "type", havingValue = "smtp")
public class SmtpEmailTransport implements EmailTransport {

    private final JavaMailSender mailSender;
    private final MailerProperties mailerProperties;

    public SmtpEmailTransport(JavaMailSender mailSender, MailerProperties mailerProperties) {
        this.mailSender = mailSender;
        this.mailerProperties = mailerProperties;
    }

    @Override
    public EmailSendResult send(String recipientEmail, String recipientName, EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setFrom(new InternetAddress(
                mailerProperties.getTransport().getFromEmail(),
                mailerProperties.getTransport().getFromName(),
                StandardCharsets.UTF_8.name()
            ));
            helper.setSubject(message.subject());
            helper.setText(message.body(), message.html());
            mailSender.send(mimeMessage);
            return new EmailSendResult("SMTP", mimeMessage.getMessageID());
        } catch (Exception exception) {
            throw new IllegalStateException(buildSmtpErrorMessage(exception), exception);
        }
    }

    private String buildSmtpErrorMessage(Exception exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        String message = rootCause.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        if (message == null || message.isBlank()) {
            return "SMTP-отправка завершилась ошибкой без текста от почтового провайдера.";
        }

        return "SMTP-отправка завершилась ошибкой: " + message;
    }
}
