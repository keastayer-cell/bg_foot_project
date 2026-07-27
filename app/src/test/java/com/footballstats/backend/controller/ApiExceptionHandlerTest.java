package com.footballstats.backend.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void responseStatusUsesStableErrorEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/players/404");

        var response = handler.handleResponseStatus(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден."),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Игрок не найден.");
        assertThat(response.getBody().path()).isEqualTo("/api/players/404");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void unexpectedErrorsDoNotLeakInternalMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/seasons");
        Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            var response = handler.handleUnexpected(new RuntimeException("database password"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error()).isEqualTo("Внутренняя ошибка сервера.");
            assertThat(appender.list)
                .allSatisfy(event -> {
                    assertThat(event.getFormattedMessage()).doesNotContain("database password");
                    assertThat(event.getThrowableProxy()).isNull();
                });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
