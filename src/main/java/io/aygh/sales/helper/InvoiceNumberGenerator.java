package io.aygh.sales.helper;

import io.aygh.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Allocates the number printed on a bill.
 * <p>
 * Shaped {@code INV-YYYYMM-NNNNN}, restarting the sequence each month, which is
 * what a monthly VAT return is filed against. The number is taken by reading the
 * highest already issued under the current prefix and adding one — zero-padded,
 * so the string ordering and the numeric ordering agree and {@code MAX} on a
 * text column is meaningful.
 * <p>
 * Two tills billing in the same instant can both read the same maximum. What
 * stops them keeping it is the unique index on {@code invoice_number}: the
 * loser's insert fails and the sale is retried rather than two bills going out
 * under one number.
 */
@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private static final String PREFIX = "INV-";
    private static final String SEQUENCE_FORMAT = "%05d";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kathmandu");

    private final SaleRepository saleRepository;

    public String next() {
        String prefix = prefixFor(LocalDate.now(BUSINESS_ZONE));
        String highest = saleRepository.highestInvoiceNumber(prefix + "%");

        long sequence = highest == null ? 1 : parseSequence(highest) + 1;
        return prefix + SEQUENCE_FORMAT.formatted(sequence);
    }

    private static String prefixFor(LocalDate date) {
        return "%s%d%02d-".formatted(PREFIX, date.getYear(), date.getMonthValue());
    }

    /** A number this service did not mint is treated as the start of the month. */
    private static long parseSequence(String invoiceNumber) {
        int lastDash = invoiceNumber.lastIndexOf('-');
        try {
            return Long.parseLong(invoiceNumber.substring(lastDash + 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return 0;
        }
    }
}
