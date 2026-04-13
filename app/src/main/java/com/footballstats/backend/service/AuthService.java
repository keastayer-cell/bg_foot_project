package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.dto.auth.AuthResponse;
import com.footballstats.backend.dto.auth.GuestLoginRequest;
import com.footballstats.backend.dto.auth.LoginRequest;
import com.footballstats.backend.dto.auth.RegisterRequest;
import com.footballstats.backend.dto.auth.UserResponse;
import com.footballstats.backend.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final AccessControlService accessControlService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
        AppUserRepository appUserRepository,
        JwtService jwtService,
        AccessControlService accessControlService,
        BCryptPasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.accessControlService = accessControlService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует.");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        AppUser saved = appUserRepository.save(user);
        accessControlService.assignDefaultUserRole(saved.getId());
        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getName());
        List<String> roles = accessControlService.getRoleCodes(saved.getId());

        return new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getName(), roles);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный email или пароль.");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName());
        List<String> roles = accessControlService.getRoleCodes(user.getId());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), roles);
    }

    public AuthResponse guestLogin(GuestLoginRequest request) {
        String guestName = "Гость";
        if (request != null && StringUtils.hasText(request.getName())) {
            guestName = request.getName().trim();
        }
        if (guestName.length() > 80) {
            guestName = guestName.substring(0, 80);
        }

        String token = jwtService.generateGuestToken(guestName);
        return new AuthResponse(token, 0L, "guest@football.local", guestName, List.of(RoleCode.GUEST.name()));
    }

    public UserResponse getCurrentUser(String authHeader) {
        String token = extractBearerToken(authHeader);
        Claims claims = jwtService.parseToken(token);

        Long userId = readUserId(claims.get("uid"));
        boolean isGuest = userId == null || userId <= 0;
        List<String> roles = isGuest ? readRolesFromClaims(claims) : accessControlService.getRoleCodes(userId);

        return new UserResponse(
            isGuest ? 0L : userId,
            isGuest,
            claims.getSubject(),
            String.valueOf(claims.get("name")),
            roles
        );
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
        if (uidClaim instanceof String value && StringUtils.hasText(value)) {
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
        return List.of(RoleCode.GUEST.name());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String extractBearerToken(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Отсутствует Bearer токен.");
        }
        return authHeader.substring(7).trim();
    }
}
