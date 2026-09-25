package io.aygh.shared.cbms;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * A bill as the IRD's Central Billing Monitoring System expects it.
 */
@Builder
public record CbmsRequest(
        @JsonProperty("username")
        String username,
        @JsonProperty("password")
        String password,
        @JsonProperty("seller_pan")
        String sellerPan,
        @JsonProperty("buyer_pan")
        String buyerPan,
        @JsonProperty("fiscal_year")
        String fiscalYear,
        @JsonProperty("buyer_name")
        String buyerName,
        @JsonProperty("invoice_number")
        String invoiceNumber,
        @JsonProperty("invoice_date")
        String invoiceDate,
        @JsonProperty("total_sales")
        Double totalSales,
        @JsonProperty("taxable_sales_vat")
        Double taxableSalesVat,
        @JsonProperty("vat")
        Double vat,
        @JsonProperty("excisable_amount")
        Double excisableAmount,
        @JsonProperty("excise")
        Double excise,
        @JsonProperty("taxable_sales_hst")
        Double taxableSalesHst,
        @JsonProperty("hst")
        Double hst,
        @JsonProperty("amount_for_esf")
        Double amountForEsf,
        @JsonProperty("esf")
        Double esf,
        @JsonProperty("export_sales")
        Double exportSales,
        @JsonProperty("tax_exempted_sales")
        Double taxExemptedSales,
        @JsonProperty("is_real_time")
        Boolean isRealTime,

        // Exact layout the .NET API parses its DateTime from
        @JsonProperty("date_time_client")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime dateTimeClient
) {
}
