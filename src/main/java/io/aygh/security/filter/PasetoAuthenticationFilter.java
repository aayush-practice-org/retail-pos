package io.aygh.security.filter;

import io.aygh.identity.service.ApplicationUserDetailService;
import io.aygh.identity.service.TokenService;
import io.aygh.security.context.UserHolder;
import io.aygh.security.token.AppToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final ApplicationUserDetailService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.debug("Request received at path: {}", request.getRequestURI());

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing 'Bearer ' prefix on path: {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token format");
            return;
        }

        String token = authorizationHeader.substring(7);

        Optional<AppToken> decrypted = tokenService.decrypt(token);
        if (decrypted.isEmpty()) {
            log.warn("Token decryption failed or expired on path: {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        AppToken appToken = decrypted.get();

        try {
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(appToken.getUserName());
            } catch (UsernameNotFoundException e) {
                // Valid token, but the account was removed after it was issued
                log.warn("Token references deleted user '{}' on path: {}", appToken.getUserName(), request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User no longer exists");
                return;
            }

            if (!userDetails.isEnabled()) {
                log.warn("Deactivated user '{}' attempted access on path: {}", appToken.getUserName(), request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account has been deactivated");
                return;
            }

            UserHolder.setUserId(appToken.getUserId());
            UserHolder.setUsername(appToken.getUserName());
            UserHolder.setRole(appToken.getRole());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, appToken, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } finally {
            UserHolder.clear();
        }
    }
}
