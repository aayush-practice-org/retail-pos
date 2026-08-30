package io.aygh.identity.controller;

import io.aygh.identity.dto.request.ChangePasswordRequest;
import io.aygh.identity.dto.response.SelfResponse;
import io.aygh.identity.service.query.SelfQueryService;
import io.aygh.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 *  The signed-in user, acting on their own account.
 *
 *  Whose account it is comes from the token, never from the request, so nobody
 *  reaches anyone else's password here. Resetting someone else's is an admin
 *  action and lives on /admin/staff instead.
 */
@Tag(name = "Self", description = "The signed-in user's own account.")
@RestController
@RequestMapping("/api/self")
@RequiredArgsConstructor
@Slf4j
public class SelfController {

    private final SelfQueryService selfQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<SelfResponse>> getSelfInfo() {
        log.info("REST request to get self");
        SelfResponse response = selfQueryService.getSelfInfo();
        return ResponseEntity.ok(ApiResponse.ok("Self fetched successfully", response));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change own password");
        selfQueryService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
