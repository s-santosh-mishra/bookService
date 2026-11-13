package com.servicehub.serv.config;

import com.servicehub.serv.security.JwtAuthenticationFilter;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        // BCrypt password hashing
        @Bean
        public PasswordEncoder passwordEncoder() {

                return new BCryptPasswordEncoder();
        }

        // CORS configuration
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(
                                List.of(
                                                "http://127.0.0.1:5500",
                                                "http://localhost:5500",
                                                "https://servicehub-pink.vercel.app"));

                configuration.setAllowedMethods(
                                List.of(
                                                "GET",
                                                "POST",
                                "PATCH",
                                                "PUT",
                                                "DELETE",
                                                "OPTIONS"));

                configuration.setAllowedHeaders(
                                List.of("*"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration(
                                "/**",
                                configuration);

                return source;
        }

        // Spring Security configuration
        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http

                                // CORS
                                .cors(cors -> cors.configurationSource(
                                                corsConfigurationSource()))

                                // CSRF is disabled because
                                // authentication is stateless and
                                // handled using JWT
                                .csrf(csrf -> csrf.disable())

                                // JWT authentication does not use
                                // server-side HTTP sessions
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                // Endpoint authorization
                                .authorizeHttpRequests(auth -> auth

                                                // Authentication endpoints
                                                .requestMatchers("/api/auth/**")
                                                .permitAll()

                                                // Customer registration
                                                .requestMatchers("/api/user/register")
                                                .permitAll()

                                                // Worker registration
                                                .requestMatchers("/api/worker/register")
                                                .permitAll()

                                                // Worker APIs require WORKER role
                                                .requestMatchers("/api/worker/**")
                                                .hasRole("WORKER")

                                                // Public categories
                                                .requestMatchers(HttpMethod.GET, "/api/categories")
                                                .permitAll()

                                                // Admin APIs require JWT + ADMIN role
                                                .requestMatchers("/api/admin/**")
                                                .hasRole("ADMIN")

                                                // Everything else requires authentication
                                                .anyRequest()
                                                .authenticated())

                                // Exception handling for unauthenticated requests (401) and access denied (403)
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        logger.warn("Unauthenticated request: method={} path={} reason={}",
                                                                        request.getMethod(), request.getRequestURI(), authException.getMessage());
                                                        response.setContentType("application/json");
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.getWriter().write("{\"error\":\"Authentication required.\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                                                        String principal = (auth != null) ? auth.getName() : "anonymous";
                                                        String authorities = (auth != null && auth.getAuthorities() != null)
                                                                        ? auth.getAuthorities().toString() : "none";
                                                        logger.warn("Access denied: method={} path={} principal={} authorities={} reason={}",
                                                                        request.getMethod(), request.getRequestURI(), principal, authorities,
                                                                        accessDeniedException.getMessage());
                                                        response.setContentType("application/json");
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.getWriter().write("{\"error\":\"Access denied.\"}");
                                                }))

                                // JWT filter
                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}