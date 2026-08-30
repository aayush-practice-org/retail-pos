package io.aygh.identity.controller;

import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.RoleOptionResponse;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.service.command.StaffCommandService;
import io.aygh.identity.service.query.StaffQueryService;
import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/*
 *  Admin Staff controller
 *
 *  The owner hires and lets go: create, edit and remove the people who work the
 *  mart, and set which role each of them holds. The role decides everything else —
 *  which app they land in and which modules they see once there.
 *
 *  Guarded by @PreAuthorize("@rbac.canAccess('STAFF')"), which reads the same
 *  SidebarMenu table the sidebar is built from: ADMIN and MANAGER only. A manager
 *  may staff the floor but cannot mint admins or fellow managers — that rule lives
 *  in UserValidation, one layer down, so it holds however staff are reached.
 */
@Tag(name = "Admin · Staff", description = "Staff accounts and their roles.")
@RestController
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("@rbac.canAccess('STAFF')")
public class AdminStaffController {

    private final StaffCommandService staffCommandService;
    private final StaffQueryService staffQueryService;

    // ── CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> create(@Valid @RequestBody StaffCreateRequest request) {
        log.info("ADMIN REST request to create Staff: {} as {}", request.username(), request.role());
        StaffResponse response = staffCommandService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Staff member '%s' created successfully".formatted(request.username()), response));
    }

    /**
     * Partial update — send only what changes. A supplied password is a reset.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody StaffUpdateRequest request) {
        log.info("ADMIN REST request to update Staff: {}", id);
        StaffResponse response = staffCommandService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Staff updated successfully", response));
    }

    /**
     * Suspend or reinstate an account without losing its history.
     */
    @PutMapping("/{id}/active")
    public ResponseEntity<ApiResponse<StaffResponse>> setActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        log.info("ADMIN REST request to set Staff {} active={}", id, active);
        StaffResponse response = staffCommandService.setActive(id, active);
        return ResponseEntity.ok(ApiResponse.ok(
                "Staff member %s successfully".formatted(active ? "reinstated" : "deactivated"), response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        log.info("ADMIN REST request to delete Staff: {}", id);
        staffCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Staff deleted successfully", null));
    }

    // ── Queries ───────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StaffResponse>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean isActive,
            @ModelAttribute PageableRequest pageableRequest) {
        log.info("ADMIN REST request to get all Staff with search: {}, role: {}", search, role);
        PagedResponse<StaffResponse> staff = staffQueryService.findAll(
                search, role, isActive, pageableRequest.toPageable());
        return ResponseEntity.ok(ApiResponse.ok("Staff fetched successfully", staff));
    }

    /**
     * The roles the caller may hand out, each with the apps and modules it unlocks —
     * an ADMIN sees every role, a MANAGER only the ones below them.
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleOptionResponse>>> assignableRoles() {
        log.info("ADMIN REST request to get assignable Staff roles");
        List<RoleOptionResponse> roles = staffQueryService.assignableRoles();
        return ResponseEntity.ok(ApiResponse.ok("Roles fetched successfully", roles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> findById(@PathVariable UUID id) {
        log.info("ADMIN REST request to get Staff by id: {}", id);
        StaffResponse response = staffQueryService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("Staff fetched successfully", response));
    }
}
