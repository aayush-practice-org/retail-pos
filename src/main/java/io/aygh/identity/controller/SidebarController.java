package io.aygh.identity.controller;

import io.aygh.identity.dto.response.SidebarGroupResponse;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.service.sidebar.SidebarService;
import io.aygh.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The navigation the console draws for the signed-in account. It sits under
 * {@code /me} because it describes the caller, not the mart: two accounts in the
 * same mart get different answers.
 */
@Tag(name = "Self · Sidebar", description = "Navigation for the signed-in account.")
@RestController
@RequestMapping("/me/sidebar")
@RequiredArgsConstructor
public class SidebarController {

    private final SidebarService sidebarService;

    @Operation(summary = "The sidebar for the signed-in account, filtered to its role")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SidebarGroupResponse>>> sidebar() {
        return ResponseEntity.ok(ApiResponse.ok(sidebarService.currentUserSidebar()));
    }

    /**
     * What a role would see. For an admin deciding which role to hand a new
     * hire, and for checking the table without signing in as each role in turn.
     */
    @Operation(summary = "The sidebar a given role would be shown")
    @GetMapping("/preview/{role}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SidebarGroupResponse>>> preview(@PathVariable UserRole role) {
        return ResponseEntity.ok(ApiResponse.ok(sidebarService.sidebarFor(role)));
    }
}
