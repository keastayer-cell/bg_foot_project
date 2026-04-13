package com.footballstats.backend.security;

import com.footballstats.backend.service.ApiAccessRuleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiAccessRuleFilter extends OncePerRequestFilter {

    private final ApiAccessRuleService apiAccessRuleService;

    public ApiAccessRuleFilter(ApiAccessRuleService apiAccessRuleService) {
        this.apiAccessRuleService = apiAccessRuleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || isPublicApi(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        List<String> roleCodes = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(this::stripRolePrefix)
            .toList();

        boolean allowed = apiAccessRuleService.isAllowed(roleCodes, request.getMethod(), path);
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Доступ к API запрещен для вашей роли.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicApi(String path) {
        return path.startsWith("/api/auth/") || path.equals("/api/health") || path.startsWith("/api/health/");
    }

    private String stripRolePrefix(String authority) {
        if (authority != null && authority.startsWith("ROLE_")) {
            return authority.substring("ROLE_".length());
        }
        return String.valueOf(authority == null ? "" : authority);
    }
}