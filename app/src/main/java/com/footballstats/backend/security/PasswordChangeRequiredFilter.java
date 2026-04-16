package com.footballstats.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal) || !appUserPrincipal.isMustChangePassword()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAllowedWhilePasswordChangeRequired(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(
            response.getWriter(),
            Map.of(
                "error", "Требуется смена пароля.",
                "code", "PASSWORD_CHANGE_REQUIRED"
            )
        );
    }

    private boolean isAllowedWhilePasswordChangeRequired(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/api/health") || path.startsWith("/api/health/")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/auth/me".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/admin/access/me".equals(path)) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) && "/api/auth/change-password".equals(path);
    }
}