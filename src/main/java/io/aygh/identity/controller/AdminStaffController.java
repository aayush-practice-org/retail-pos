package io.aygh.identity.controller;

import io.aygh.identity.dto.request.ResetPasswordRequest;
import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import io.aygh.identity.service.command.StaffCommandService;
import io.aygh.identity.service.query.StaffQueryService;
import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * A mart admin's staff screens.
 * <p>
 * The tenant is never a parameter here — not in the path, not in the body. It
 * comes from the caller's token, so an admin reaches its own staff and only its
 * own, and there is nothing on the wire to tamper with.
 */
@Tag(name = "Staff", description = "Managing the staff of one mart.")
@RestController
@RequestMapping("/admin/staff")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final StaffCommandService staffCommandService;
    private final StaffQueryService staffQueryService;

    @Operation(summary = "Hire a staff member into your mart")
    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> create(@Valid @RequestBody StaffCreateRequest request) {
        log.info("Creating a {} account for '{}'", request.role(), request.username());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(staffCommandService.createStaff(request)));
    }

    @Operation(summary = "List your mart's staff")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StaffResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                staffQueryService.findAll(search, role, status, pageable.toPageable())));
    }

    @Operation(summary = "The roles you may assign to your staff")
    @GetMapping("/assignable-roles")
    public ResponseEntity<ApiResponse<List<UserRole>>> assignableRoles() {
        return ResponseEntity.ok(ApiResponse.ok(UserRole.assignableByAdmin().stream().sorted().toList()));
    }

    @Operation(summary = "One staff account")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(staffQueryService.findById(id)));
    }

    @Operation(summary = "Edit a staff account")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody StaffUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Staff updated", staffCommandService.updateStaff(id, request)));
    }

    @Operation(summary = "Set a staff member's password")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {

        staffCommandService.resetStaffPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset"));
    }

    @Operation(summary = "Retire a staff account")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        staffCommandService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.ok("Staff account retired"));
    }
}
