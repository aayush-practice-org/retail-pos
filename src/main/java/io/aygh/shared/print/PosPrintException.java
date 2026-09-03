package io.aygh.shared.print;

/**
 * Thrown when a printable document (receipt, invoice, report) could not be produced.
 * Always wraps the underlying cause.
 */
public class PosPrintException extends RuntimeException {
    public PosPrintException(String message, Throwable cause) {
        super(message, cause);
    }
}
