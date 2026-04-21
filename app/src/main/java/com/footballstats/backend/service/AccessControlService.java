package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.Role;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.UserRole;
import com.footballstats.backend.domain.UserTeamScope;
import com.footballstats.backend.dto.auth.AssignTeamScopeRequest;
import com.footballstats.backend.dto.auth.RoleAnnotationResponse;
import com.footballstats.backend.dto.auth.TeamScopeResponse;
import com.footballstats.backend.dto.auth.UserAccessResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.RoleRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.UserRoleRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AccessControlService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TeamRepository teamRepository;
    private final UserTeamScopeRepository userTeamScopeRepository;
    private final NotificationEventService notificationEventService;

    public AccessControlService(
        AppUserRepository appUserRepository,
        RoleRepository roleRepository,
        UserRoleRepository userRoleRepository,
        TeamRepository teamRepository,
        UserTeamScopeRepository userTeamScopeRepository,
        NotificationEventService notificationEventService
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.teamRepository = teamRepository;
        this.userTeamScopeRepository = userTeamScopeRepository;
        this.notificationEventService = notificationEventService;
    }

    @Transactional
    public void assignDefaultUserRole(Long userId) {
        ensureRole(userId, RoleCode.USER, null);
    }

    @Transactional
    public boolean ensureRole(Long userId, RoleCode roleCode, Long grantedByUserId) {
        if (userRoleRepository.existsByUser_IdAndRole_CodeAndActiveTrue(userId, roleCode)) {
            return false;
        }

        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + roleCode));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setGrantedByUserId(grantedByUserId);
        userRole.setActive(true);
        userRoleRepository.save(userRole);
        return true;
    }

    @Transactional(readOnly = true)
    public List<String> getRoleCodes(Long userId) {
        return userRoleRepository.findByUser_IdAndActiveTrue(userId).stream()
            .map(userRole -> userRole.getRole().getCode().name())
            .sorted()
            .toList();
    }


    @Transactional(readOnly = true)
    public Page<AppUser> searchUsers(String namePattern, String emailPattern, Pageable pageable) {
        return appUserRepository.searchUsers(namePattern, emailPattern, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AppUser> searchUsers(String namePattern, String emailPattern, String role, Pageable pageable) {
        if (role == null || role.isBlank()) {
            return appUserRepository.searchUsers(namePattern, emailPattern, pageable);
        }
        com.footballstats.backend.domain.RoleCode roleCode;
        try {
            roleCode = com.footballstats.backend.domain.RoleCode.valueOf(role);
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректная роль: " + role);
        }
        return appUserRepository.searchUsersByRole(namePattern, emailPattern, roleCode, pageable);
    }

    @Transactional
    public void assignRole(Long actorUserId, Long targetUserId, RoleCode roleCode) {
        boolean assigned = ensureRole(targetUserId, roleCode, actorUserId);
        if (assigned && roleCode == RoleCode.REFEREE) {
            AppUser user = appUserRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
            notificationEventService.enqueueRefereeRoleGranted(user);
        }
    }

    @Transactional
    public void revokeRole(Long targetUserId, RoleCode roleCode) {
        UserRole userRole = userRoleRepository.findByUser_IdAndRole_CodeAndActiveTrue(targetUserId, roleCode)
            .orElseThrow(() -> new IllegalArgumentException("У пользователя нет активной роли " + roleCode + "."));
        userRole.setActive(false);
        userRoleRepository.save(userRole);
    }

    @Transactional
    public void assignTeamScope(Long actorUserId, Long targetUserId, AssignTeamScopeRequest request) {
        if (userTeamScopeRepository.findByUser_IdAndTeam_IdAndActiveTrue(targetUserId, request.getTeamId()).isPresent()) {
            throw new IllegalArgumentException("Доступ к этой команде уже выдан.");
        }

        AppUser user = appUserRepository.findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
        Team team = teamRepository.findById(request.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("Команда не найдена."));

        UserTeamScope scope = new UserTeamScope();
        scope.setUser(user);
        scope.setTeam(team);
        scope.setGrantedByUserId(actorUserId);
        scope.setCanEditRoster(request.isCanEditRoster());
        scope.setCanEditApplication(request.isCanEditApplication());
        scope.setActive(true);
        userTeamScopeRepository.save(scope);

        ensureRole(targetUserId, RoleCode.TEAM_REP, actorUserId);
        notificationEventService.enqueueTeamRepRoleGranted(user, team);
    }

    @Transactional
    public void revokeTeamScope(Long targetUserId, Long teamId) {
        UserTeamScope scope = userTeamScopeRepository.findByUser_IdAndTeam_IdAndActiveTrue(targetUserId, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Активный доступ к команде не найден."));
        scope.setActive(false);
        scope.setValidTo(OffsetDateTime.now());
        userTeamScopeRepository.save(scope);
    }

    @Transactional(readOnly = true)
    public UserAccessResponse getUserAccess(Long userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        List<String> roles = getRoleCodes(userId);
        List<RoleAnnotationResponse> roleAnnotations = buildRoleAnnotations(roles);

        List<TeamScopeResponse> teamScopes = userTeamScopeRepository.findByUser_IdAndActiveTrue(userId).stream()
            .sorted(Comparator.comparing(scope -> scope.getTeam().getName()))
            .map(scope -> new TeamScopeResponse(
                scope.getTeam().getId(),
                scope.getTeam().getName(),
                scope.isCanEditRoster(),
                scope.isCanEditApplication(),
                scope.getValidFrom(),
                scope.getValidTo()
            ))
            .toList();

        return new UserAccessResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            roles,
            roleAnnotations,
            teamScopes,
            user.isMustChangePassword()
        );
    }

    @Transactional(readOnly = true)
    public List<RoleAnnotationResponse> buildRoleAnnotations(List<String> roleCodes) {
        return roleCodes.stream()
            .map(RoleCode::valueOf)
            .map(this::toRoleAnnotation)
            .toList();
    }

    private RoleAnnotationResponse toRoleAnnotation(RoleCode roleCode) {
        return switch (roleCode) {
            case SUPER_ADMIN -> new RoleAnnotationResponse(
                roleCode.name(),
                "Супер администратор",
                "Полный доступ ко всем разделам и управлению правами пользователей."
            );
            case REFEREE -> new RoleAnnotationResponse(
                roleCode.name(),
                "Рефери",
                "Может работать с матчевыми API и протоколами, но без раздела прав доступа и API Explorer."
            );
            case TEAM_REP -> new RoleAnnotationResponse(
                roleCode.name(),
                "Представитель команды",
                "Может редактировать данные назначенной команды в рамках выданных team scope прав."
            );
            case USER -> new RoleAnnotationResponse(
                roleCode.name(),
                "Пользователь",
                "Базовый доступ к просмотру защищенных разделов без административных прав."
            );
            case GUEST -> new RoleAnnotationResponse(
                roleCode.name(),
                "Гость",
                "Вход без регистрации с базовыми правами просмотра как у пользователя."
            );
        };
    }

    @Transactional(readOnly = true)
    public boolean hasTeamPermission(Long userId, Long teamId, String permissionCode) {
        UserTeamScope scope = userTeamScopeRepository.findByUser_IdAndTeam_IdAndActiveTrue(userId, teamId)
            .orElse(null);
        if (scope == null) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (scope.getValidFrom() != null && now.isBefore(scope.getValidFrom())) {
            return false;
        }
        if (scope.getValidTo() != null && now.isAfter(scope.getValidTo())) {
            return false;
        }

        return switch (permissionCode) {
            case "ROSTER_EDIT" -> scope.isCanEditRoster();
            case "APPLICATION_EDIT" -> scope.isCanEditApplication();
            default -> false;
        };
    }
}
