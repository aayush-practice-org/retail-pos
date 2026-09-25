package io.aygh.purchase.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Goods sent back against one vendor bill — only the lines that are damaged,
 * expired or wrong, in the quantities going back.
 * <p>
 * Only quantities are accepted: what each line is worth comes from the bill, so
 * a return can never debit the vendor more than the goods cost.
 */
public record PurchaseReturnRequest(

        @NotNull(message = "The purchase being returned against is required")
        Long purchaseId,

        @NotBlank(message = "A reason for the return is required")
        @Size(max = 255)
        String reason,

        /** Defaults to today. */
        LocalDate returnDate,

        @Size(max = 255) String remark,

        @NotEmpty(message = "A return needs at least one item")
        @Valid
        List<PurchaseReturnItemRequest> items
) {
}
