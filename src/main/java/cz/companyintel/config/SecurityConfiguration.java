package cz.companyintel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.companyintel.security.ImportPayloadLimitFilter;
import cz.companyintel.security.RequestRateLimitFilter;
import cz.companyintel.web.AuthSessionResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<RequestRateLimitFilter> requestRateLimitFilterRegistration(
            RequestRateLimitFilter filter) {
        FilterRegistrationBean<RequestRateLimitFilter> registration =
                new FilterRegistrationBean<RequestRateLimitFilter>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ImportPayloadLimitFilter> importPayloadLimitFilterRegistration(
            ImportPayloadLimitFilter filter) {
        FilterRegistrationBean<ImportPayloadLimitFilter> registration =
                new FilterRegistrationBean<ImportPayloadLimitFilter>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.security.users.admin.username}") String adminUsername,
            @Value("${app.security.users.admin.password}") String adminPassword,
            @Value("${app.security.users.editor.username}") String editorUsername,
            @Value("${app.security.users.editor.password}") String editorPassword,
            @Value("${app.security.users.viewer.username}") String viewerUsername,
            @Value("${app.security.users.viewer.password}") String viewerPassword) {
        Set<String> usernames = new HashSet<String>();
        UserDetails admin = user(adminUsername, adminPassword, "ADMIN", passwordEncoder, usernames);
        UserDetails editor = user(editorUsername, editorPassword, "EDITOR", passwordEncoder, usernames);
        UserDetails viewer = user(viewerUsername, viewerPassword, "VIEWER", passwordEncoder, usernames);
        return new InMemoryUserDetailsManager(admin, editor, viewer);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            RequestRateLimitFilter rateLimitFilter,
            ImportPayloadLimitFilter importPayloadLimitFilter,
            @Value("${app.security.csrf-enabled:true}") boolean csrfEnabled,
            @Value("${app.security.require-https:false}") boolean requireHttps) throws Exception {
        if (requireHttps) {
            http.requiresChannel()
                    .anyRequest()
                    .requiresSecure();
        }

        if (csrfEnabled) {
            CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            csrfRepository.setCookiePath("/");
            http.csrf()
                    .csrfTokenRepository(csrfRepository)
                    .ignoringAntMatchers("/h2-console/**");
        } else {
            http.csrf().disable();
        }

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(importPayloadLimitFilter, FilterSecurityInterceptor.class)
                .authorizeRequests()
                .antMatchers("/", "/index.html", "/styles.css", "/app.js", "/js/**").permitAll()
                .antMatchers("/api/auth/session", "/api/auth/login", "/api/auth/logout").permitAll()
                .antMatchers("/h2-console/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/companies/*", "/api/people/*").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/audit/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN", "EDITOR")
                .antMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN", "EDITOR")
                .antMatchers(HttpMethod.PATCH, "/api/**").hasAnyRole("ADMIN", "EDITOR")
                .antMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EDITOR")
                .antMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "EDITOR", "VIEWER")
                .anyRequest().denyAll()
                .and()
                .formLogin()
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) ->
                        writeJson(response, HttpServletResponse.SC_OK,
                                AuthSessionResponse.from(authentication), objectMapper))
                .failureHandler((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                "Neplatné uživatelské jméno nebo heslo.", objectMapper))
                .permitAll()
                .and()
                .logout()
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                        writeJson(response, HttpServletResponse.SC_OK,
                                AuthSessionResponse.anonymous(), objectMapper))
                .and()
                .httpBasic()
                .authenticationEntryPoint((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                "Pro tuto operaci se musíte přihlásit.", objectMapper))
                .and()
                .exceptionHandling()
                .accessDeniedHandler((request, response, exception) ->
                        writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                "Pro tuto operaci nemáte dostatečné oprávnění.", objectMapper))
                .and()
                .headers()
                .frameOptions()
                .sameOrigin();

        return http.build();
    }

    private UserDetails user(
            String username,
            String password,
            String role,
            PasswordEncoder passwordEncoder,
            Set<String> usernames) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty()) {
            throw new IllegalStateException("Uživatelské jméno pro roli " + role + " nesmí být prázdné.");
        }
        if (password == null || password.length() < 10) {
            throw new IllegalStateException("Heslo pro roli " + role + " musí mít alespoň 10 znaků.");
        }
        if (!usernames.add(cleanUsername.toLowerCase())) {
            throw new IllegalStateException("Uživatelská jména bezpečnostních rolí musí být jedinečná.");
        }
        return User.withUsername(cleanUsername)
                .password(passwordEncoder.encode(password))
                .roles(role)
                .build();
    }

    private void writeError(HttpServletResponse response, int status, String message, ObjectMapper objectMapper)
            throws IOException {
        writeJson(response, status, new SecurityErrorResponse(status, message), objectMapper);
    }

    private void writeJson(HttpServletResponse response, int status, Object body, ObjectMapper objectMapper)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static class SecurityErrorResponse {
        private final int status;
        private final String message;

        SecurityErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
