package io.aygh.sales.helper;

import io.aygh.sales.repository.SalesReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Allocates the number printed on a credit note: {@code CN-YYYYMM-NNNNN}, on a
 * sequence of its own and restarting each month, exactly as
 * {@link InvoiceNumberGenerator} does for bills. The unique index on
 * {@code credit_note_number} is what stops two tills keeping the same one.
 */
@Component
@RequiredArgsConstructor
public class CreditNoteNumberGenerator {

    private static final String PREFIX = "CN-";
    private static final String SEQUENCE_FORMAT = "%05d";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kathmandu");

    private final SalesReturnRepository salesReturnRepository;

    public String next() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        String prefix = "%s%d%02d-".formatted(PREFIX, today.getYear(), today.getMonthValue());
        String highest = salesReturnRepository.highestCreditNoteNumber(prefix + "%");

        long sequence = highest == null ? 1 : parseSequence(highest) + 1;
        return prefix + SEQUENCE_FORMAT.formatted(sequence);
    }

    private static long parseSequence(String number) {
        int lastDash = number.lastIndexOf('-');
        try {
            return Long.parseLong(number.substring(lastDash + 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return 0;
        }
    }
}
