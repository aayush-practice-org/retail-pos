package io.aygh.identity.controller;

import io.aygh.identity.dto.request.LoginRequest;
import io.aygh.identity.dto.response.LoginResponse;
import io.aygh.identity.service.auth.UserAuthService;
import io.aygh.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signing in. Anonymous by necessity — everything else in the API needs the
 * token this hands out.
 * <p>
 * Nothing that acts on an existing session belongs here: everything under
 * {@code /public} is reachable without a token, so changing a password lives on
 * {@link SelfController} where authentication is actually required.
 */
@Tag(name = "Authentication", description = "Sign in and issue bearer tokens.")
@SecurityRequirements
@RestController
@RequestMapping("/public/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserAuthService userAuthService;

    @Operation(summary = "Sign in with a username or email and receive a bearer token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Sign-in attempt for '{}'", request.username());
        return ResponseEntity.ok(ApiResponse.ok("Signed in", userAuthService.login(request)));
    }
}
