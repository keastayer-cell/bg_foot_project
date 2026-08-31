package com.footballstats.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CounterWindow> counters = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = resolveRule(request.getRequestURI());
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = normalizeClientIp(request);
        String key = rule.keyPrefix() + ':' + clientIp;
        Instant now = Instant.now();
        CounterWindow window = counters.compute(key, (ignored, current) -> advanceWindow(current, now, rule.window(), rule.limit()));

        if (window.count() > rule.limit()) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of(
                "error", "Слишком много запросов. Повторите попытку позже.",
                "code", "RATE_LIMIT_EXCEEDED"
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(String path) {
        if ("/api/auth/login".equals(path)) {
            return new RateLimitRule("auth-login", 10, Duration.ofMinutes(1));
        }
        if ("/api/auth/register".equals(path)) {
            return new RateLimitRule("auth-register", 5, Duration.ofMinutes(10));
        }
        if ("/api/auth/refresh".equals(path)) {
            return new RateLimitRule("auth-refresh", 30, Duration.ofMinutes(5));
        }
        if ("/api/auth/password-reset/complete".equals(path)) {
            return new RateLimitRule("auth-reset-complete", 10, Duration.ofMinutes(10));
        }
        if ("/api/auth/password-reset/request".equals(path)) {
            return new RateLimitRule("auth-reset-request", 5, Duration.ofMinutes(10));
        }
        if ("/api/auth/guest".equals(path)) {
            return new RateLimitRule("auth-guest", 20, Duration.ofMinutes(5));
        }
        return null;
    }

    private CounterWindow advanceWindow(CounterWindow current, Instant now, Duration ttl, int limit) {
        if (current == null || now.isAfter(current.startedAt().plus(ttl))) {
            cleanupExpired(ttl, now);
            return new CounterWindow(1, now);
        }
        return new CounterWindow(Math.min(current.count() + 1, limit + 1), current.startedAt());
    }

    private void cleanupExpired(Duration ttl, Instant now) {
        if (counters.size() < 512) {
            return;
        }
        counters.entrySet().removeIf(entry -> now.isAfter(entry.getValue().startedAt().plus(ttl).plus(ttl)));
    }

    private String normalizeClientIp(HttpServletRequest request) {
        String remoteAddr = String.valueOf(request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr()).trim();
        return remoteAddr.isEmpty() ? "unknown" : remoteAddr;
    }

    private record RateLimitRule(String keyPrefix, int limit, Duration window) {
    }

    private record CounterWindow(int count, Instant startedAt) {
    }
}
