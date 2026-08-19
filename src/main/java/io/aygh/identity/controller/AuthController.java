package io.aygh.identity.controller;

import io.aygh.identity.dto.request.LoginRequest;
import io.aygh.identity.dto.response.LoginResponse;
import io.aygh.identity.service.auth.UserAuthService;
import io.aygh.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 *  The way in. The only endpoint under /api/auth that is reachable without a token —
 *  every other staff endpoint expects the PASETO token this hands back.
 *
 *  The response carries the role, which tells the front-end where to send them:
 *  a CASHIER to the counter app, everyone else to the back office. GET /api/sidebar
 *  then fills in the navigation for whichever one they land in.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login user: {}", request.username());
        LoginResponse response = userAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Logged in successfully", response));
    }
}
