package com.footballstats.backend.service;

import com.footballstats.backend.domain.ApiAccessRule;
import com.footballstats.backend.repository.ApiAccessRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ApiAccessRuleService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final ApiAccessRuleRepository apiAccessRuleRepository;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private volatile RuleCache cache = new RuleCache(List.of(), Instant.EPOCH);

    public ApiAccessRuleService(ApiAccessRuleRepository apiAccessRuleRepository) {
        this.apiAccessRuleRepository = apiAccessRuleRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(List<String> roleCodes, String httpMethod, String requestPath) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }

        boolean superAdmin = roleCodes.stream()
            .map(code -> String.valueOf(code).trim().toUpperCase(Locale.ROOT))
            .anyMatch("SUPER_ADMIN"::equals);
        if (superAdmin) {
            return true;
        }

        String normalizedMethod = normalizeMethod(httpMethod);
        String normalizedPath = normalizePath(requestPath);

        List<RuleEntry> entries = getCachedRules();
        for (String roleCode : roleCodes) {
            String normalizedRole = String.valueOf(roleCode).trim().toUpperCase(Locale.ROOT);
            for (RuleEntry entry : entries) {
                if (!entry.roleCode().equals(normalizedRole)) {
                    continue;
                }
                if (!entry.httpMethod().equals("*") && !entry.httpMethod().equals(normalizedMethod)) {
                    continue;
                }
                if (antPathMatcher.match(entry.urlPattern(), normalizedPath)) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<RuleEntry> getCachedRules() {
        RuleCache current = cache;
        if (Instant.now().isBefore(current.loadedAt().plus(CACHE_TTL))) {
            return current.entries();
        }

        synchronized (this) {
            RuleCache refreshed = cache;
            if (Instant.now().isBefore(refreshed.loadedAt().plus(CACHE_TTL))) {
                return refreshed.entries();
            }

            List<RuleEntry> loaded = apiAccessRuleRepository.findAllActiveWithRole().stream()
                .map(this::toEntry)
                .toList();

            cache = new RuleCache(loaded, Instant.now());
            return loaded;
        }
    }

    private RuleEntry toEntry(ApiAccessRule rule) {
        return new RuleEntry(
            rule.getRole().getCode().name(),
            normalizeMethod(rule.getHttpMethod()),
            normalizePath(rule.getUrlPattern())
        );
    }

    private String normalizeMethod(String method) {
        return String.valueOf(method == null ? "" : method).trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePath(String path) {
        String normalized = String.valueOf(path == null ? "" : path).trim();
        if (normalized.isEmpty()) {
            return "/";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private record RuleEntry(String roleCode, String httpMethod, String urlPattern) {}

    private record RuleCache(List<RuleEntry> entries, Instant loadedAt) {}
}