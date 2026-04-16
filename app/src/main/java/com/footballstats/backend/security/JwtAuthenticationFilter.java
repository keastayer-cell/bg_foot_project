package com.footballstats.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.service.AccessControlService;
import com.footballstats.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccessControlService accessControlService;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        AccessControlService accessControlService,
        AppUserRepository appUserRepository,
        ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
        this.accessControlService = accessControlService;
        this.appUserRepository = appUserRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7).trim();
            Claims claims = jwtService.parseToken(token);

            Long userId = readUserId(claims.get("uid"));
            String email = claims.getSubject();
            String name = String.valueOf(claims.get("name"));
            Integer tokenVersion = readTokenVersion(claims.get("ver"));

            boolean mustChangePassword = false;

            List<String> roleCodes;
            if (userId != null && userId > 0) {
                AppUser user = appUserRepository.findById(userId).orElse(null);
                if (user == null) {
                    writeUnauthorized(response, "Пользователь больше не найден. Войдите снова.");
                    return;
                }
                if (!Integer.valueOf(user.getTokenVersion() == null ? 0 : user.getTokenVersion()).equals(tokenVersion)) {
                    writeUnauthorized(response, "Сессия устарела. Войдите снова.");
                    return;
                }
                mustChangePassword = user.isMustChangePassword();
                roleCodes = accessControlService.getRoleCodes(userId);
            } else {
                roleCodes = readRolesFromClaims(claims);
            }

            List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .toList();

            AppUserPrincipal principal = new AppUserPrincipal(
                userId == null ? 0L : userId,
                email,
                name,
                mustChangePassword,
                authorities
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private Integer readTokenVersion(Object versionClaim) {
        if (versionClaim == null) {
            return 0;
        }
        if (versionClaim instanceof Integer value) {
            return value;
        }
        if (versionClaim instanceof Long value) {
            return value.intValue();
        }
        if (versionClaim instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }

    private Long readUserId(Object uidClaim) {
        if (uidClaim == null) {
            return null;
        }
        if (uidClaim instanceof Integer value) {
            return value.longValue();
        }
        if (uidClaim instanceof Long value) {
            return value;
        }
        if (uidClaim instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<String> readRolesFromClaims(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> rawRoles) {
            return rawRoles.stream().map(String::valueOf).toList();
        }
        return List.of("GUEST");
    }
}
