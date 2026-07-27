package com.footballstats.mailer.config;

import java.nio.charset.StandardCharsets;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mailer")
public class MailerProperties {

    @Min(100)
    private long pollIntervalMs = 5000;
    @Min(1)
    private int batchSize = 20;
    @Min(1)
    private int retryLimit = 5;
    @Min(1)
    private int retryDelaySeconds = 60;
    @Min(1)
    private int maxRetryDelaySeconds = 3600;
    @Min(30)
    private int staleLockSeconds = 300;
    @Valid
    private final Transport transport = new Transport();

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public int getStaleLockSeconds() {
        return staleLockSeconds;
    }

    public int getMaxRetryDelaySeconds() {
        return maxRetryDelaySeconds;
    }

    public void setMaxRetryDelaySeconds(int maxRetryDelaySeconds) {
        this.maxRetryDelaySeconds = maxRetryDelaySeconds;
    }

    public void setStaleLockSeconds(int staleLockSeconds) {
        this.staleLockSeconds = staleLockSeconds;
    }

    public Transport getTransport() {
        return transport;
    }

    public static class Transport {

        @Pattern(regexp = "log|smtp", message = "mailer.transport.type должен быть log или smtp")
        private String type = "log";
        @NotBlank
        private String fromEmail = "no-reply@football.local";
        @NotBlank
        private String fromName = "Football Stats";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFromEmail() {
            return fromEmail;
        }

        public void setFromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = normalizePossiblyMisdecodedUtf8(fromName);
        }

        private String normalizePossiblyMisdecodedUtf8(String value) {
            if (value == null || value.isBlank()) {
                return value;
            }
            if (!looksLikeUtf8Mojibake(value)) {
                return value;
            }
            return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }

        private boolean looksLikeUtf8Mojibake(String value) {
            return value.indexOf('Ð') >= 0 || value.indexOf('Ñ') >= 0 || value.indexOf('Ã') >= 0;
        }
    }
}
