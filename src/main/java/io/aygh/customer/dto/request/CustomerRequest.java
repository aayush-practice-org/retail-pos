package io.aygh.customer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomerRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Size(max = 30, message = "PAN must not exceed 30 characters")
        String panNumber,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
        BigDecimal creditLimit,

        Boolean active
) {
}
