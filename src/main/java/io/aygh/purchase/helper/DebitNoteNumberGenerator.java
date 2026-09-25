package io.aygh.purchase.helper;

import io.aygh.purchase.repository.PurchaseReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Allocates the number on a debit note: {@code DN-YYYYMM-NNNNN}, restarting each
 * month. The unique index on {@code debit_note_number} is what stops two
 * requests keeping the same one.
 */
@Component
@RequiredArgsConstructor
public class DebitNoteNumberGenerator {

    private static final String PREFIX = "DN-";
    private static final String SEQUENCE_FORMAT = "%05d";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kathmandu");

    private final PurchaseReturnRepository purchaseReturnRepository;

    public String next() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        String prefix = "%s%d%02d-".formatted(PREFIX, today.getYear(), today.getMonthValue());
        String highest = purchaseReturnRepository.highestDebitNoteNumber(prefix + "%");

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
