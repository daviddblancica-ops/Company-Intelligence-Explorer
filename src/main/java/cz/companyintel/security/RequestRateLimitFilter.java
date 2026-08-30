package cz.companyintel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.companyintel.web.ErrorResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String ARES_PATH_PREFIX = "/api/import/ares/";
    private static final Set<String> BULK_IMPORT_PATHS = new HashSet<String>(Arrays.asList(
            "/api/import/json",
            "/api/import/csv",
            "/api/import/preview/json",
            "/api/import/preview/csv"));

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final FixedWindowRateLimiter loginLimiter;
    private final FixedWindowRateLimiter aresLimiter;
    private final FixedWindowRateLimiter bulkImportLimiter;

    @Autowired
    public RequestRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.max-keys:10000}") int maxKeys,
            @Value("${app.security.rate-limit.login.max-requests:10}") int loginMaxRequests,
            @Value("${app.security.rate-limit.login.window:5m}") Duration loginWindow,
            @Value("${app.security.rate-limit.ares.max-requests:30}") int aresMaxRequests,
            @Value("${app.security.rate-limit.ares.window:1m}") Duration aresWindow,
            @Value("${app.security.rate-limit.import.max-requests:20}") int importMaxRequests,
            @Value("${app.security.rate-limit.import.window:1m}") Duration importWindow) {
        this(objectMapper, enabled, maxKeys, loginMaxRequests, loginWindow,
                aresMaxRequests, aresWindow, importMaxRequests, importWindow, Clock.systemUTC());
    }

    RequestRateLimitFilter(
            ObjectMapper objectMapper,
            boolean enabled,
            int maxKeys,
            int loginMaxRequests,
            Duration loginWindow,
            int aresMaxRequests,
            Duration aresWindow,
            int importMaxRequests,
            Duration importWindow,
            Clock clock) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.loginLimiter = new FixedWindowRateLimiter(loginMaxRequests, loginWindow, maxKeys, clock);
        this.aresLimiter = new FixedWindowRateLimiter(aresMaxRequests, aresWindow, maxKeys, clock);
        this.bulkImportLimiter = new FixedWindowRateLimiter(importMaxRequests, importWindow, maxKeys, clock);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        FixedWindowRateLimiter.Decision decision = decision(request);
        if (decision != null && !decision.isAllowed()) {
            writeLimitExceeded(response, request, decision.getRetryAfterSeconds());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private FixedWindowRateLimiter.Decision decision(HttpServletRequest request) {
        if (!enabled || !HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        String path = applicationPath(request);
        if (LOGIN_PATH.equals(path)) {
            return loginLimiter.acquire("login:" + remoteAddress(request));
        }
        if (path.startsWith(ARES_PATH_PREFIX)) {
            return aresLimiter.acquire("ares:" + authenticatedSubject() + ":" + remoteAddress(request));
        }
        if (BULK_IMPORT_PATHS.contains(path)) {
            return bulkImportLimiter.acquire("import:" + authenticatedSubject() + ":" + remoteAddress(request));
        }
        return null;
    }

    private String applicationPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }

    private String remoteAddress(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return address == null || address.trim().isEmpty() ? "unknown" : address;
    }

    private String authenticatedSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private void writeLimitExceeded(
            HttpServletResponse response,
            HttpServletRequest request,
            long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Příliš mnoho požadavků. Zkuste akci zopakovat později.",
                request.getRequestURI()));
    }
}
