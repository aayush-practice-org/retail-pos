package io.aygh.unit.controller;

import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.aygh.unit.dto.request.CreateCustomUnitRequest;
import io.aygh.unit.dto.response.UnitResponse;
import io.aygh.unit.service.command.CustomUnitCommandService;
import io.aygh.unit.service.query.CustomUnitQueryService;
import io.aygh.unit.service.query.SystemUnitQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 *  Admin Unit controller
 *
 *  System units are seeded at the system level and are read-only.
 *  The mart may only add, edit and remove its own custom units.
 */
@RestController
@RequestMapping("/admin/units")
@RequiredArgsConstructor
@Slf4j
public class AdminUnitController {

    private final SystemUnitQueryService systemUnitQueryService;
    private final CustomUnitQueryService customUnitQueryService;
    private final CustomUnitCommandService customUnitCommandService;

    // ── Lookup ────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<UnitResponse>>> findAll() {
        log.info("ADMIN REST request to get all Units (system + custom)");
        List<UnitResponse> units = new ArrayList<>(systemUnitQueryService.findAll());
        units.addAll(customUnitQueryService.findAll());
        return ResponseEntity.ok(ApiResponse.ok("Units fetched successfully", units));
    }

    // ── System units ──────────────────────────────────────────────────────

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<PagedResponse<UnitResponse>>> getSystemUnits(
            @ModelAttribute PageableRequest pageable) {
        log.info("ADMIN REST request to get all SystemUnits");
        PagedResponse<UnitResponse> response = systemUnitQueryService.getAllSystemUnits(pageable.toPageable());
        return ResponseEntity.ok(ApiResponse.ok("System units fetched successfully", response));
    }

    // ── Custom units ──────────────────────────────────────────────────────

    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<UnitResponse>> createCustomUnit(
            @Valid @RequestBody CreateCustomUnitRequest request) {
        log.info("ADMIN REST request to create CustomUnit: {}", request.getName());
        UnitResponse response = customUnitCommandService.createCustomUnit(request);
        return ResponseEntity.ok(ApiResponse.ok("Custom unit created successfully", response));
    }

    @GetMapping("/custom")
    public ResponseEntity<ApiResponse<PagedResponse<UnitResponse>>> getCustomUnits(
            @ModelAttribute PageableRequest pageable) {
        log.info("ADMIN REST request to get all CustomUnits");
        PagedResponse<UnitResponse> response = customUnitQueryService.getAllCustomUnits(pageable.toPageable());
        return ResponseEntity.ok(ApiResponse.ok("Custom units fetched successfully", response));
    }

    @GetMapping("/custom/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> getCustomUnit(@PathVariable UUID id) {
        log.info("ADMIN REST request to get CustomUnit: {}", id);
        UnitResponse response = customUnitQueryService.getCustomUnit(id);
        return ResponseEntity.ok(ApiResponse.ok("Custom unit fetched successfully", response));
    }

    @PutMapping("/custom/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> updateCustomUnit(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCustomUnitRequest request) {
        log.info("ADMIN REST request to update CustomUnit: {}", id);
        UnitResponse response = customUnitCommandService.updateCustomUnit(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Custom unit updated successfully", response));
    }

    @DeleteMapping("/custom/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomUnit(@PathVariable UUID id) {
        log.info("ADMIN REST request to delete CustomUnit: {}", id);
        customUnitCommandService.deleteCustomUnit(id);
        return ResponseEntity.ok(ApiResponse.ok("Custom unit deleted successfully", null));
    }
}
