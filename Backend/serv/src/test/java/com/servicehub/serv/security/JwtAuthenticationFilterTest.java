package com.servicehub.serv.security;

import com.servicehub.serv.config.SecurityConfig;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.enums.UserRole;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration unit test exercising the real JwtAuthenticationFilter, real JwtService,
 * real SecurityConfig-configured SecurityFilterChain, and real AuthorityAuthorizationManager.
 */
public class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "test-jwt-secret-key-at-least-256-bits-long-for-testing-purpose-only";
    private static final long ONE_HOUR_MS = 3600000L;

    private JwtService jwtService;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public JwtService jwtService() {
            return new JwtService(TEST_SECRET, ONE_HOUR_MS);
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
            return new JwtAuthenticationFilter(jwtService);
        }

        @Bean
        public SecurityConfig securityConfig(JwtAuthenticationFilter filter) {
            return new SecurityConfig(filter);
        }

        @Bean
        public SecurityFilterChain filterChain(SecurityConfig config, HttpSecurity http) throws Exception {
            return config.securityFilterChain(http);
        }
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService(TEST_SECRET, ONE_HOUR_MS);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Real filter with valid ADMIN JWT populates SecurityContext with ROLE_ADMIN and admin UUID")
    void validAdminJwtSetsAdminRoleInSecurityContext() throws ServletException, IOException {
        UUID adminId = UUID.randomUUID();
        Credentials adminCreds = new Credentials();
        adminCreds.setUserId(adminId);
        adminCreds.setEmail("admin@servicehub.com");
        adminCreds.setRole(UserRole.ADMIN);

        String token = jwtService.generateToken(adminCreds);
        assertNotNull(token);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/settlements");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication must be set in SecurityContext");
        assertEquals(adminId.toString(), auth.getName(), "Principal name must match admin UUID string");
        assertTrue(auth.isAuthenticated());

        boolean hasRoleAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        assertTrue(hasRoleAdmin, "Granted authorities must contain 'ROLE_ADMIN'");
    }

    @Test
    @DisplayName("Real filter with valid WORKER JWT populates SecurityContext with ROLE_WORKER")
    void validWorkerJwtSetsWorkerRoleInSecurityContext() throws ServletException, IOException {
        UUID workerId = UUID.randomUUID();
        Credentials workerCreds = new Credentials();
        workerCreds.setUserId(workerId);
        workerCreds.setEmail("worker@servicehub.com");
        workerCreds.setRole(UserRole.WORKER);

        String token = jwtService.generateToken(workerCreds);
        assertNotNull(token);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/worker/dashboard");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication must be set in SecurityContext");
        assertEquals(workerId.toString(), auth.getName());

        boolean hasRoleWorker = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_WORKER".equals(a.getAuthority()));
        assertTrue(hasRoleWorker, "Granted authorities must contain 'ROLE_WORKER'");

        boolean hasRoleAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        assertFalse(hasRoleAdmin, "WORKER must NOT have 'ROLE_ADMIN'");
    }

    @Test
    @DisplayName("Missing Authorization header leaves SecurityContext unauthenticated")
    void missingAuthorizationHeaderLeavesSecurityContextNull() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/settlements");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "SecurityContext must remain null when Authorization header is missing");
    }

    @Test
    @DisplayName("Non-Bearer Authorization header leaves SecurityContext unauthenticated")
    void nonBearerAuthorizationHeaderLeavesSecurityContextNull() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/settlements");
        request.addHeader("Authorization", "Basic YWRtaW46c2VjcmV0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "SecurityContext must remain null for non-Bearer Authorization headers");
    }

    @Test
    @DisplayName("Expired JWT leaves SecurityContext unauthenticated")
    void expiredJwtLeavesSecurityContextNull() throws ServletException, IOException {
        JwtService expiredJwtService = new JwtService(TEST_SECRET, -10000L);
        Credentials adminCreds = new Credentials();
        adminCreds.setUserId(UUID.randomUUID());
        adminCreds.setEmail("admin@servicehub.com");
        adminCreds.setRole(UserRole.ADMIN);

        String expiredToken = expiredJwtService.generateToken(adminCreds);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/settlements");
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Expired token must leave SecurityContext unauthenticated");
    }

    @Test
    @DisplayName("JWT with invalid HMAC signature leaves SecurityContext unauthenticated")
    void invalidSignatureJwtLeavesSecurityContextNull() throws ServletException, IOException {
        JwtService otherKeyService = new JwtService(
                "different-secret-key-that-does-not-match-the-configured-application-key",
                ONE_HOUR_MS
        );
        Credentials adminCreds = new Credentials();
        adminCreds.setUserId(UUID.randomUUID());
        adminCreds.setEmail("admin@servicehub.com");
        adminCreds.setRole(UserRole.ADMIN);

        String forgedToken = otherKeyService.generateToken(adminCreds);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/settlements");
        request.addHeader("Authorization", "Bearer " + forgedToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Tampered/untrusted signature must be rejected, leaving context unauthenticated");
    }

    @Test
    @DisplayName("Malformed 'Bearer null' header leaves SecurityContext unauthenticated")
    void bearerNullHeaderLeavesSecurityContextNull() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/settlements");
        request.addHeader("Authorization", "Bearer null");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Literal 'Bearer null' must be safely rejected without throwing unhandled exceptions");
    }

    @Test
    @DisplayName("Spring Security's real hasRole('ADMIN') rule grants ADMIN authentication and rejects WORKER or unauthenticated")
    void realSpringSecurityHasRoleRuleEvaluatesAuthenticationCorrectly() throws ServletException, IOException {
        AuthorityAuthorizationManager<Object> adminRoleManager = AuthorityAuthorizationManager.hasRole("ADMIN");

        // 1. Valid ADMIN through real filter
        UUID adminId = UUID.randomUUID();
        Credentials adminCreds = new Credentials();
        adminCreds.setUserId(adminId);
        adminCreds.setEmail("admin@servicehub.com");
        adminCreds.setRole(UserRole.ADMIN);
        String adminToken = jwtService.generateToken(adminCreds);

        MockHttpServletRequest adminRequest = new MockHttpServletRequest("POST", "/api/admin/settlements");
        adminRequest.addHeader("Authorization", "Bearer " + adminToken);
        jwtAuthenticationFilter.doFilter(adminRequest, new MockHttpServletResponse(), new MockFilterChain());

        Authentication adminAuth = SecurityContextHolder.getContext().getAuthentication();
        AuthorizationResult adminDecision = adminRoleManager.authorize(() -> adminAuth, adminRequest);
        assertNotNull(adminDecision);
        assertTrue(adminDecision.isGranted(),
                "Spring Security's hasRole('ADMIN') rule MUST grant access to Authentication produced by real filter for ADMIN JWT");

        // 2. Valid WORKER through real filter
        SecurityContextHolder.clearContext();
        Credentials workerCreds = new Credentials();
        workerCreds.setUserId(UUID.randomUUID());
        workerCreds.setEmail("worker@servicehub.com");
        workerCreds.setRole(UserRole.WORKER);
        String workerToken = jwtService.generateToken(workerCreds);

        MockHttpServletRequest workerRequest = new MockHttpServletRequest("POST", "/api/admin/settlements");
        workerRequest.addHeader("Authorization", "Bearer " + workerToken);
        jwtAuthenticationFilter.doFilter(workerRequest, new MockHttpServletResponse(), new MockFilterChain());

        Authentication workerAuth = SecurityContextHolder.getContext().getAuthentication();
        AuthorizationResult workerDecision = adminRoleManager.authorize(() -> workerAuth, workerRequest);
        assertNotNull(workerDecision);
        assertFalse(workerDecision.isGranted(),
                "Spring Security's hasRole('ADMIN') rule MUST REJECT access for WORKER JWT");

        // 3. Unauthenticated request (missing/expired token)
        SecurityContextHolder.clearContext();
        AuthorizationResult anonDecision = adminRoleManager.authorize(() -> null, adminRequest);
        assertNotNull(anonDecision);
        assertFalse(anonDecision.isGranted(),
                "Spring Security's hasRole('ADMIN') rule MUST REJECT unauthenticated requests");
    }

    @Test
    @DisplayName("Actual SecurityFilterChain invokes authenticationEntryPoint (401) and accessDeniedHandler (403) with mock requests")
    void actualSecurityFilterChainExecutesConfiguredExceptionHandlers() throws Exception {
        try (AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext()) {
            ctx.register(TestSecurityConfig.class);
            ctx.refresh();

            SecurityFilterChain securityFilterChain = ctx.getBean(SecurityFilterChain.class);
            FilterChainProxy filterChainProxy = new FilterChainProxy(securityFilterChain);

            // 1. Unauthenticated request to /api/admin/settlements exercises the real authenticationEntryPoint
            MockHttpServletRequest unauthRequest = new MockHttpServletRequest("POST", "/api/admin/settlements");
            MockHttpServletResponse unauthResponse = new MockHttpServletResponse();

            filterChainProxy.doFilter(unauthRequest, unauthResponse, new MockFilterChain());

            assertEquals(401, unauthResponse.getStatus(), "Unauthenticated request to admin endpoint must return 401");
            assertEquals("application/json", unauthResponse.getContentType());
            assertEquals("{\"error\":\"Authentication required.\"}", unauthResponse.getContentAsString());

            // 2. Authenticated WORKER request to /api/admin/settlements exercises the real accessDeniedHandler
            SecurityContextHolder.clearContext();
            Credentials workerCreds = new Credentials();
            workerCreds.setUserId(UUID.randomUUID());
            workerCreds.setEmail("worker@servicehub.com");
            workerCreds.setRole(UserRole.WORKER);
            String workerToken = jwtService.generateToken(workerCreds);

            MockHttpServletRequest forbiddenRequest = new MockHttpServletRequest("POST", "/api/admin/settlements");
            forbiddenRequest.addHeader("Authorization", "Bearer " + workerToken);
            MockHttpServletResponse forbiddenResponse = new MockHttpServletResponse();

            filterChainProxy.doFilter(forbiddenRequest, forbiddenResponse, new MockFilterChain());

            assertEquals(403, forbiddenResponse.getStatus(), "WORKER role attempting admin endpoint must return 403");
            assertEquals("application/json", forbiddenResponse.getContentType());
            assertEquals("{\"error\":\"Access denied.\"}", forbiddenResponse.getContentAsString());
        }
    }
}
