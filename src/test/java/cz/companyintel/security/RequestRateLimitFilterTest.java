package cz.companyintel.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestRateLimitFilterTest {

    private RequestRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestRateLimitFilter(
                new ObjectMapper().findAndRegisterModules(),
                true,
                100,
                2,
                Duration.ofMinutes(5),
                1,
                Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksLoginRequestsAfterConfiguredLimit() throws Exception {
        assertThat(execute("/api/auth/login", "192.0.2.10").getStatus()).isEqualTo(200);
        assertThat(execute("/api/auth/login", "192.0.2.10").getStatus()).isEqualTo(200);

        MockHttpServletResponse blocked = execute("/api/auth/login", "192.0.2.10");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("300");
        assertThat(blocked.getContentAsString()).contains("Příliš mnoho požadavků");
        assertThat(execute("/api/auth/login", "192.0.2.11").getStatus()).isEqualTo(200);
    }

    @Test
    void keepsAresLimitsSeparateForAuthenticatedUsers() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("editor-a", "", "ROLE_EDITOR"));
        assertThat(execute("/api/import/ares/23143614", "192.0.2.10").getStatus()).isEqualTo(200);
        assertThat(execute("/api/import/ares/23143614", "192.0.2.10").getStatus()).isEqualTo(429);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("editor-b", "", "ROLE_EDITOR"));
        assertThat(execute("/api/import/ares/23143614", "192.0.2.10").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse execute(String path, String remoteAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
