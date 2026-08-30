package io.aygh.config;

import io.aygh.security.filter.PasetoAuthenticationFilter;
import io.aygh.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/*
 *  Security for the mart backend.
 *
 *  Staff carry a PASETO token from POST /api/auth/login. PasetoAuthenticationFilter
 *  turns it back into an authenticated user, and the admin controllers sit behind
 *  @PreAuthorize("@rbac.canAccess('...')"), which reads the same SidebarMenu table
 *  the sidebar is built from — so a role can never call something it cannot see.
 *
 *  Multi-tenancy and RLS are still not ported from the restaurant backend; when
 *  they land, the tenant filter goes in beside the PASETO one below.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final PasetoAuthenticationFilter pasetoAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The filter is a {@code @Component}, which Boot would otherwise register against
     * every request. It belongs to the authenticated chain alone.
     */
    @Bean
    FilterRegistrationBean<PasetoAuthenticationFilter> pasetoFilterRegistration(
            PasetoAuthenticationFilter filter) {
        FilterRegistrationBean<PasetoAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // 1. AUTH — the way in, the one staff endpoint reachable without a token
    @Bean
    @Order(0)
    SecurityFilterChain authSecurity(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher("/api/auth/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req.anyRequest().permitAll());
        return http.build();
    }

    // 2. STAFF — the back office, the counter, and whoever is signed in
    @Bean
    @Order(1)
    SecurityFilterChain staffSecurity(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher("/admin/**", "/api/self/**", "/api/sidebar/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(pasetoAuthenticationFilter, AuthenticationFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    // 3. API — storefront / read endpoints
    @Bean
    @Order(2)
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    // 4. OpenAPI / Swagger / health endpoints
    @Bean
    @Order(-1)
    SecurityFilterChain openApiSecurity(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        "/swagger-resources/**", "/webjars/**", "/health-check")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * No token, or one that is no longer good.
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> writeError(response, HttpStatus.UNAUTHORIZED,
                "Authentication required. Please log in.");
    }

    /**
     * A valid token, but a role that does not reach this module.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, deniedException) -> writeError(response, HttpStatus.FORBIDDEN,
                "You do not have access to this resource");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(status, message));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:5174"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "gi"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
