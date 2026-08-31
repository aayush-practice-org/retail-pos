package io.aygh.identity.controller;

import io.aygh.identity.dto.request.AdminCreateRequest;
import io.aygh.identity.dto.request.AdminUpdateRequest;
import io.aygh.identity.dto.request.ResetPasswordRequest;
import io.aygh.identity.dto.response.AdminResponse;
import io.aygh.identity.entity.ProvisioningStatus;
import io.aygh.identity.service.command.AdminCommandService;
import io.aygh.identity.service.query.AdminQueryService;
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


@Tag(name = "Super admin", description = "Registering and overseeing marts.")
@RestController
@RequestMapping("/superadmin/admins")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final AdminCommandService adminCommandService;
    private final AdminQueryService adminQueryService;


    @Operation(summary = "Register a mart, then create and migrate its schema")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminResponse>> create(@Valid @RequestBody AdminCreateRequest request) {
        log.info("Registering mart '{}' with admin '{}'", request.companyName(), request.username());
        AdminResponse response = adminCommandService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "List every mart in the installation")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminResponse>>> getAdmins(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProvisioningStatus provisioningStatus,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminQueryService.findAll(search, provisioningStatus, pageable.toPageable())));
    }

    @Operation(summary = "One mart, with its company details and schema state")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminQueryService.findById(id)));
    }

    @Operation(summary = "Edit a mart's company details and subscription")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody AdminUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Mart updated", adminCommandService.updateAdmin(id, request)));
    }

    /**
     * Retries a schema build that failed. Idempotent, so it is safe on a READY mart too.
     */
    @Operation(summary = "Create and migrate a mart's schema again")
    @PostMapping("/{id}/provision")
    public ResponseEntity<ApiResponse<AdminResponse>> provision(@PathVariable UUID id) {
        log.info("Re-provisioning the schema for mart {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Provisioning attempted", adminCommandService.provisionAdmin(id)));
    }

    @Operation(summary = "Set a mart admin's password")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {

        adminCommandService.resetAdminPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset"));
    }

    @Operation(summary = "Retire a mart and its staff accounts; its schema and data are left in place")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        adminCommandService.deleteAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok("Mart retired"));
    }

    @Operation(summary = "Apply any new tenant migrations to every mart")
    @PostMapping("/migrations")
    public ResponseEntity<ApiResponse<List<String>>> runTenantMigrations() {
        List<String> failed = adminCommandService.migrateAllTenants();
        return failed.isEmpty()
                ? ResponseEntity.ok(ApiResponse.ok("Every mart is up to date", failed))
                : ResponseEntity.ok(ApiResponse.ok(failed.size() + " mart(s) failed to migrate", failed));
    }
}
