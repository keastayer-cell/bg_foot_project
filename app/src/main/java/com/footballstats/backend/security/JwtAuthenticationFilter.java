package com.footballstats.backend.security;

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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccessControlService accessControlService;

    public JwtAuthenticationFilter(JwtService jwtService, AccessControlService accessControlService) {
        this.jwtService = jwtService;
        this.accessControlService = accessControlService;
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

            List<String> roleCodes = (userId != null && userId > 0)
                ? accessControlService.getRoleCodes(userId)
                : readRolesFromClaims(claims);

            List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .toList();

            AppUserPrincipal principal = new AppUserPrincipal(userId == null ? 0L : userId, email, name, authorities);
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

    @SuppressWarnings("unchecked")
    private List<String> readRolesFromClaims(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> rawRoles) {
            return rawRoles.stream().map(String::valueOf).toList();
        }
        return List.of("GUEST");
    }
}
