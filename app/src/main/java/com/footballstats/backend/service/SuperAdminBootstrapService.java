package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SuperAdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapService.class);

    private final String superAdminEmail;
    private final AppUserRepository appUserRepository;
    private final AccessControlService accessControlService;

    public SuperAdminBootstrapService(
        @Value("${APP_SUPER_ADMIN_EMAIL:}") String superAdminEmail,
        AppUserRepository appUserRepository,
        AccessControlService accessControlService
    ) {
        this.superAdminEmail = superAdminEmail == null ? "" : superAdminEmail.trim().toLowerCase();
        this.appUserRepository = appUserRepository;
        this.accessControlService = accessControlService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSuperAdminRole() {
        if (superAdminEmail.isEmpty()) {
            return;
        }

        Optional<AppUser> user = appUserRepository.findByEmailIgnoreCase(superAdminEmail);
        if (user.isEmpty()) {
            log.warn("APP_SUPER_ADMIN_EMAIL задан, но пользователь не найден: {}", superAdminEmail);
            return;
        }

        accessControlService.ensureRole(user.get().getId(), RoleCode.SUPER_ADMIN, null);
    }
}
