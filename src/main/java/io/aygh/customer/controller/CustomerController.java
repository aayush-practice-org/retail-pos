package io.aygh.customer.controller;

import io.aygh.customer.dto.request.CustomerRequest;
import io.aygh.customer.dto.request.CustomerSettlementRequest;
import io.aygh.customer.dto.response.CustomerOutstandingResponse;
import io.aygh.customer.dto.response.CustomerResponse;
import io.aygh.customer.dto.response.CustomerSettlementResponse;
import io.aygh.customer.service.command.CustomerCommandService;
import io.aygh.customer.service.query.CustomerQueryService;
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

@Tag(name = "Customers", description = "Customer master data and directory.")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'SALES_EXECUTIVE', 'ACCOUNTANT', 'CASHIER', 'CUSTOMER_SUPPORT')")
public class CustomerController {

    private final CustomerCommandService customerCommandService;
    private final CustomerQueryService customerQueryService;

    @Operation(summary = "Register a customer")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(customerCommandService.create(request)));
    }

    @Operation(summary = "List customers — matches on name, phone or PAN")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponse>>> list(
            @RequestParam(required = false) String search,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(customerQueryService.findAll(search, pageable.toPageable())));
    }

    @Operation(summary = "All customers, unpaged — for pickers and dropdowns")
    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> selection() {
        return ResponseEntity.ok(ApiResponse.ok(customerQueryService.findAllForSelection()));
    }

    @Operation(summary = "Find customer by phone number")
    @GetMapping("/by-phone/{phone}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ApiResponse.ok(customerQueryService.findByPhone(phone)));
    }

    @Operation(summary = "One customer by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(customerQueryService.findById(id)));
    }

    @Operation(summary = "Customer credit outstanding — limit, total debt and unpaid invoices")
    @GetMapping("/{id}/outstanding")
    public ResponseEntity<ApiResponse<CustomerOutstandingResponse>> outstanding(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(customerQueryService.getOutstanding(id)));
    }

    @Operation(summary = "Settle customer credit — applies payment across oldest unpaid invoices first (FIFO)")
    @PostMapping("/{id}/settle")
    public ResponseEntity<ApiResponse<CustomerSettlementResponse>> settle(
            @PathVariable Long id,
            @Valid @RequestBody CustomerSettlementRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Settlement recorded", customerCommandService.settle(id, request)));
    }

    @Operation(summary = "Update customer details")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Customer updated", customerCommandService.update(id, request)));
    }

    @Operation(summary = "Remove a customer")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Customer removed"));
    }
}
