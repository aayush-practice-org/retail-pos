package io.aygh.shared.controller;

import io.aygh.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Liveness probe.")
@SecurityRequirements
@RestController
public class HealthCheckController {

    @GetMapping("/health-check")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.ok("Mart service is up and running", "UP"));
    }
}
