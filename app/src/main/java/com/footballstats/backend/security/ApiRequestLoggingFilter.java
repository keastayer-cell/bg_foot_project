package com.footballstats.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/") || path.startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            int status = response.getStatus();
            String userId = resolveUserId();
            String message = String.format(
                "requestId=%s method=%s path=%s status=%d durationMs=%d userId=%s",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                status,
                durationMs,
                userId
            );

            if (failure != null) {
                log.error("{} failure={} error={}", message, failure.getClass().getSimpleName(), safeMessage(failure), failure);
            } else if (status >= 500) {
                log.error("{}", message);
            } else if (status >= 400) {
                log.warn("{}", message);
            } else {
                log.info("{}", message);
            }

            MDC.remove(REQUEST_ID_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);
        if (header == null) {
            return UUID.randomUUID().toString();
        }

        String normalized = header.trim();
        if (normalized.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        if (normalized.length() > 64) {
            return normalized.substring(0, 64);
        }

        return normalized;
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            Long userId = appUserPrincipal.getUserId();
            if (userId != null && userId > 0) {
                return String.valueOf(userId);
            }
        }

        return "anonymous";
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message.replace('\n', ' ').replace('\r', ' ');
    }
}