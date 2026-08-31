package io.aygh.identity.controller;

import io.aygh.identity.dto.request.ChangePasswordRequest;
import io.aygh.identity.dto.response.SelfResponse;
import io.aygh.identity.service.auth.UserAuthService;
import io.aygh.identity.service.query.SelfQueryService;
import io.aygh.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in account, whatever tier it belongs to. No role guard: every
 * authenticated caller may read and change its own account, and nobody else's.
 */
@Tag(name = "Self", description = "The signed-in account.")
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class SelfController {

    private final SelfQueryService selfQueryService;
    private final UserAuthService userAuthService;

    @Operation(summary = "Who am I, and which mart am I working in")
    @GetMapping
    public ResponseEntity<ApiResponse<SelfResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(selfQueryService.currentUser()));
    }

    @Operation(summary = "Change your own password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userAuthService.changeOwnPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed"));
    }
}
