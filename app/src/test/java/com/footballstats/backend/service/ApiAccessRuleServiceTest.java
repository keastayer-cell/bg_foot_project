package com.footballstats.backend.service;

import com.footballstats.backend.domain.ApiAccessRule;
import com.footballstats.backend.domain.Role;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.repository.ApiAccessRuleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiAccessRuleServiceTest {

    @Test
    void appliesRoleMatrixAndAlwaysAllowsSuperAdmin() {
        ApiAccessRuleRepository repository = mock(ApiAccessRuleRepository.class);
        when(repository.findAllActiveWithRole()).thenReturn(List.of(
            rule(RoleCode.REFEREE, "GET", "/api/referee/**"),
            rule(RoleCode.TEAM_REP, "PUT", "/api/team-rep/**")
        ));
        ApiAccessRuleService service = new ApiAccessRuleService(repository);

        assertThat(service.isAllowed(List.of("SUPER_ADMIN"), "DELETE", "/api/admin/users/1")).isTrue();
        assertThat(service.isAllowed(List.of("REFEREE"), "GET", "/api/referee/matches")).isTrue();
        assertThat(service.isAllowed(List.of("REFEREE"), "POST", "/api/referee/matches")).isFalse();
        assertThat(service.isAllowed(List.of("TEAM_REP"), "PUT", "/api/team-rep/roster")).isTrue();
        assertThat(service.isAllowed(List.of("TEAM_REP"), "GET", "/api/team-rep/roster")).isFalse();
        assertThat(service.isAllowed(List.of("GUEST"), "GET", "/api/referee/matches")).isFalse();
    }

    private ApiAccessRule rule(RoleCode roleCode, String method, String pattern) {
        Role role = new Role();
        role.setCode(roleCode);
        ApiAccessRule rule = new ApiAccessRule();
        rule.setRole(role);
        rule.setHttpMethod(method);
        rule.setUrlPattern(pattern);
        return rule;
    }
}
