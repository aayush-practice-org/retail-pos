package io.aygh.config;

import io.aygh.identity.entity.User;
import io.aygh.identity.repository.UserRepository;
import io.aygh.shared.UserHolder;
import io.aygh.shared.service.AppToken;
import io.aygh.shared.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;


@Component
@RequiredArgsConstructor
@Slf4j
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        if (!header.startsWith(BEARER_PREFIX)) {
            unauthorized(request, response, "Authorization header must use the Bearer scheme");
            return;
        }

        Optional<AppToken> claims = tokenService.decrypt(header.substring(BEARER_PREFIX.length()).trim());
        if (claims.isEmpty()) {
            unauthorized(request, response, "Invalid or expired token");
            return;
        }

        AppToken token = claims.get();
        Instant now = Instant.now();

        if (token.isAccountExpired(now)) {
            unauthorized(request, response, "This account's subscription has expired");
            return;
        }

        Optional<User> account = userRepository.findById(token.userId());
        if (account.isEmpty()) {
            unauthorized(request, response, "This account no longer exists");
            return;
        }

        User user = account.get();

        if (!user.isEnabled() || !user.isAccountNonLocked() || !user.isAccountNonExpired()) {
            unauthorized(request, response, "This account is not active");
            return;
        }

        // A role or tenant change since the token was issued invalidates it: the
        // claims would otherwise keep granting what the account no longer has.
        if (!matchesCurrentAccount(token, user)) {
            log.warn("Token for '{}' no longer matches the account's role or tenant — rejecting", user.getUsername());
            unauthorized(request, response, "This session is no longer valid, please sign in again");
            return;
        }

        try {
            UserHolder.setUserId(user.getId());
            UserHolder.setUsername(user.getUsername());
            UserHolder.setRole(user.getRole());
            UserHolder.setTenantId(user.getTenantId());
            UserHolder.setTenantSlug(user.getTenantSlug());

            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } finally {
            // Request threads are pooled: a slug left behind would route the next
            // caller's queries into this tenant's schema.
            UserHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private static boolean matchesCurrentAccount(AppToken token, User user) {
        return user.getRole().name().equals(token.role())
                && Objects.equals(user.getTenantId(), token.tenantId())
                && Objects.equals(user.getTenantSlug(), token.tenantSlug());
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        log.warn("[401 UNAUTHORIZED] {} on {} {}", message, request.getMethod(), request.getRequestURI());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}
