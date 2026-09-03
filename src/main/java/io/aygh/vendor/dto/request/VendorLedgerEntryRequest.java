package io.aygh.vendor.dto.request;

import io.aygh.vendor.entity.BalanceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Posts what the mart owes a vendor, or what the vendor owes back.
 * <p>
 * Purchasing will post these itself once it exists; until then this is how a
 * vendor's opening position gets onto the books. {@link BalanceType#SETTLEMENT}
 * is not accepted here — money leaving the till goes through its own endpoint so
 * it can be guarded separately.
 */
public record VendorLedgerEntryRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Balance type is required")
        BalanceType balanceType
) {
}
