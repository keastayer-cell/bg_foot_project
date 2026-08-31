package com.footballstats.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    @Test
    void allowsLoginRequestsUpToLimitAndBlocksNextAttempt() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());

        for (int index = 0; index < 10; index += 1) {
            MockHttpServletResponse response = execute(filter, "/api/auth/login");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = execute(filter, "/api/auth/login");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void ignoresNonAuthPostRequests() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());

        MockHttpServletResponse response = execute(filter, "/api/teams");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void limitsPublicPasswordResetRequests() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());

        for (int index = 0; index < 5; index += 1) {
            assertThat(execute(filter, "/api/auth/password-reset/request").getStatus()).isEqualTo(200);
        }

        assertThat(execute(filter, "/api/auth/password-reset/request").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse execute(AuthRateLimitFilter filter, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}
