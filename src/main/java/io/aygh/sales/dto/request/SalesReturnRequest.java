package io.aygh.sales.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Goods handed back against one bill.
 * <p>
 * Only quantities are accepted: what each line is worth is prorated from the
 * bill, so a return can never credit more than was charged.
 */
public record SalesReturnRequest(

        @NotNull(message = "The sale being returned against is required")
        Long saleId,

        @NotBlank(message = "A reason for the return is required")
        @Size(max = 255)
        String reason,

        /** The BS date of the credit note, e.g. "2081.09.05". Worked out when left out. */
        @Size(max = 20) String nepaliDate,

        @Size(max = 255) String remark,

        @NotEmpty(message = "A return needs at least one item")
        @Valid
        List<SalesReturnItemRequest> items
) {
}
