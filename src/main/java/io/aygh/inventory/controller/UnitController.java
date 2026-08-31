package io.aygh.inventory.controller;

import io.aygh.inventory.dto.request.UnitRequest;
import io.aygh.inventory.dto.response.UnitResponse;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.service.command.UnitCommandService;
import io.aygh.inventory.service.query.UnitQueryService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The mart's unit dictionary.
 * <p>
 * The tenant is never a parameter: these rows live in the caller's own schema,
 * chosen from its token, so there is nothing on the wire to tamper with.
 */
@Tag(name = "Inventory · Units", description = "The units a mart trades in.")
@RestController
@RequestMapping("/inventory/units")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
public class UnitController {

    private final UnitCommandService unitCommandService;
    private final UnitQueryService unitQueryService;

    @Operation(summary = "Add a unit to the dictionary")
    @PostMapping
    public ResponseEntity<ApiResponse<UnitResponse>> create(@Valid @RequestBody UnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(unitCommandService.create(request)));
    }

    @Operation(summary = "List units")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UnitResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MeasurementType measurementType,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                unitQueryService.findAll(search, measurementType, pageable.toPageable())));
    }

    @Operation(summary = "Every unit, unpaged — for the pickers that choose one")
    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> selection(
            @RequestParam(required = false) MeasurementType measurementType) {

        return ResponseEntity.ok(ApiResponse.ok(unitQueryService.findAllForSelection(measurementType)));
    }

    @Operation(summary = "One unit")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(unitQueryService.findById(id)));
    }

    @Operation(summary = "Edit a mart-defined unit")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UnitResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UnitRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Unit updated", unitCommandService.update(id, request)));
    }

    @Operation(summary = "Remove a mart-defined unit")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        unitCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Unit removed"));
    }
}
